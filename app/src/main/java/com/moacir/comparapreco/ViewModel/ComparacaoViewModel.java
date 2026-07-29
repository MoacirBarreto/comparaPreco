package com.moacir.comparapreco.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.moacir.comparapreco.Model.ResultadoItem;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComparacaoViewModel extends ViewModel {

    public static final int MODO_PESO = 1;
    public static final int MODO_UNIDADE = 2;
    public static final int MODO_VOLUME = 3;
    private static final int MAX_HISTORICO = 2;

    private final MutableLiveData<ArrayList<ResultadoItem>> _listaResultados = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<ArrayList<ResultadoItem>> listaResultados = _listaResultados;

    private final MutableLiveData<Integer> _modoAtual = new MutableLiveData<>(MODO_PESO);
    public final LiveData<Integer> modoAtual = _modoAtual;

    private final MutableLiveData<Boolean> _isMetricaReduzida = new MutableLiveData<>(false);
    public final LiveData<Boolean> isMetricaReduzida = _isMetricaReduzida;

    private final MutableLiveData<String> _mensagemToast = new MutableLiveData<>();
    public final LiveData<String> mensagemToast = _mensagemToast;

    private final MutableLiveData<OcrResultado> _ocrResultado = new MutableLiveData<>();
    public final LiveData<OcrResultado> ocrResultado = _ocrResultado;

    public void setModo(int modo) {
        if (_modoAtual.getValue() != null && _modoAtual.getValue() != modo) {
            _modoAtual.setValue(modo);
            limparHistorico();
        }
    }

    public void setMetricaReduzida(boolean reduzida) {
        if (_isMetricaReduzida.getValue() != null && _isMetricaReduzida.getValue() != reduzida) {
            _isMetricaReduzida.setValue(reduzida);
            limparHistorico();
        }
    }

    public void limparHistorico() {
        _listaResultados.setValue(new ArrayList<>());
    }

    public void limparOcrResultado() {
        _ocrResultado.setValue(null);
    }

    public void calcular(double precoTotal, double valorEntrada, String precoEtiqueta, String qtdEtiqueta, String label) {
        if (precoTotal <= 0) {
            _mensagemToast.setValue("Informe o preço total");
            return;
        }

        if (valorEntrada <= 0) {
            _mensagemToast.setValue("Informe a quantidade");
            return;
        }

        double resultadoNumerico = precoTotal / valorEntrada;
        
        if (Boolean.TRUE.equals(_isMetricaReduzida.getValue()) && _modoAtual.getValue() != MODO_UNIDADE) {
            resultadoNumerico = resultadoNumerico / 10.0;
        }

        String textoResultado = String.format(java.util.Locale.getDefault(), "R$ %.2f por %s", resultadoNumerico, label);
        
        ArrayList<ResultadoItem> atual = new ArrayList<>(_listaResultados.getValue());
        if (atual.size() >= MAX_HISTORICO) atual.remove(0);
        
        atual.add(new ResultadoItem(textoResultado, resultadoNumerico, label, precoEtiqueta, qtdEtiqueta));
        _listaResultados.setValue(atual);
    }

    public void processarOCR(String texto) {
        String textoLimpo = texto.toLowerCase().replace("\n", " ");
        
        String precoDetectado = null;
        String valorMedidaDetectado = null;
        String unidadeDetectada = null;

        // Preço
        Pattern precoPattern = Pattern.compile("(?:r\\$|\\$)?\\s?(\\d{1,3}[.,]\\d{2})");
        Matcher precoMatcher = precoPattern.matcher(textoLimpo);
        if (precoMatcher.find()) {
            precoDetectado = precoMatcher.group(1).replaceAll("[^0-9]", "");
        }

        // Medida
        Pattern medidaPattern = Pattern.compile("(\\d+[.,]?\\d*)\\s*(kg|g|l|ml|un|unid)");
        Matcher medidaMatcher = medidaPattern.matcher(textoLimpo);
        if (medidaMatcher.find()) {
            valorMedidaDetectado = medidaMatcher.group(1).replace(",", ".");
            unidadeDetectada = medidaMatcher.group(2);
        }

        if (precoDetectado != null || valorMedidaDetectado != null) {
            _ocrResultado.setValue(new OcrResultado(precoDetectado, valorMedidaDetectado, unidadeDetectada));
        }
    }

    public static class OcrResultado {
        public final String preco;
        public final String valor;
        public final String unidade;

        public OcrResultado(String preco, String valor, String unidade) {
            this.preco = preco;
            this.valor = valor;
            this.unidade = unidade;
        }
    }
}