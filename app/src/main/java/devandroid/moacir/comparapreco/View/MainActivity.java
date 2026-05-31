package devandroid.moacir.comparapreco.View;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import devandroid.moacir.comparapreco.Model.ResultadoItem;
import devandroid.moacir.comparapreco.R;

public class MainActivity extends AppCompatActivity {

    private RadioGroup radioGroupTipoConversao;
    private EditText editTxtPrecoTotal, editTxtPeso, editTxtUnid, editTxtVolume;
    private Button btnCalcular, btnLimpar;
    private TextView txtResultadoCalculado;
    private LinearLayout layoutHistoricoResultados;

    private ArrayList<ResultadoItem> listaHistoricoResultados;

    private static final int MAX_HISTORICO = 2;
    private static final int MODO_PESO = 1;
    private static final int MODO_UNIDADE = 2;
    private static final int MODO_VOLUME = 3;

    private int modoAtual = MODO_PESO; // Padrão inicial

    private static final String KEY_HISTORICO_RESULTADOS = "historicoResultados";
    private static final String KEY_MODO_ATUAL = "modoAtual";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarComponentes();

        if (savedInstanceState != null) {
            modoAtual = savedInstanceState.getInt(KEY_MODO_ATUAL, MODO_PESO);
            listaHistoricoResultados = savedInstanceState.getParcelableArrayList(KEY_HISTORICO_RESULTADOS);
        } else {
            listaHistoricoResultados = new ArrayList<>();
        }

        configurarListeners();
        configurarMascaras();
        atualizarVisibilidadeCampos();
        atualizarTabelaHistorico();
    }

    private void inicializarComponentes() {
        radioGroupTipoConversao = findViewById(R.id.radioGroupTipoConversao);
        editTxtPrecoTotal = findViewById(R.id.editTxtPreco);
        editTxtPeso = findViewById(R.id.editTxtPeso);
        editTxtUnid = findViewById(R.id.editTxtUnid);
        editTxtVolume = findViewById(R.id.editTxtVolume);
        btnCalcular = findViewById(R.id.btnCalcular);
        btnLimpar = findViewById(R.id.btnLimpar);
        txtResultadoCalculado = findViewById(R.id.editTxtResultado);
        layoutHistoricoResultados = findViewById(R.id.layoutHistoricoResultados);
    }

    private void configurarMascaras() {
        // Tipos: 0 = Moeda, 1 = Peso/Volume (3 casas), 2 = Inteiro
        editTxtPrecoTotal.addTextChangedListener(new MascaraFinanceira(editTxtPrecoTotal, 0));
        editTxtPeso.addTextChangedListener(new MascaraFinanceira(editTxtPeso, 1));
        editTxtVolume.addTextChangedListener(new MascaraFinanceira(editTxtVolume, 1));
        editTxtUnid.addTextChangedListener(new MascaraFinanceira(editTxtUnid, 2));
    }

    private void configurarListeners() {
        radioGroupTipoConversao.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnPeso) {
                modoAtual = MODO_PESO;
            } else if (checkedId == R.id.radioBtnUnidades) {
                modoAtual = MODO_UNIDADE;
            } else if (checkedId == R.id.radioBtnVolume) {
                modoAtual = MODO_VOLUME;
            }
            limparCamposEspecificos();
            atualizarVisibilidadeCampos();
        });

        btnCalcular.setOnClickListener(v -> calcularPrecoUnitario());

        btnLimpar.setOnClickListener(v -> {
            limparTodosOsCampos();
            listaHistoricoResultados.clear();
            atualizarTabelaHistorico();
            bloquearTrocaDeModo(false); // Reabilita a troca de modo
        });
    }

    private void atualizarVisibilidadeCampos() {
        editTxtPeso.setVisibility(View.GONE);
        editTxtUnid.setVisibility(View.GONE);
        editTxtVolume.setVisibility(View.GONE);

        switch (modoAtual) {
            case MODO_PESO:
                editTxtPeso.setVisibility(View.VISIBLE);
                txtResultadoCalculado.setHint(getString(R.string.hint_resultado_por_kg));
                break;
            case MODO_UNIDADE:
                editTxtUnid.setVisibility(View.VISIBLE);
                txtResultadoCalculado.setHint(getString(R.string.hint_resultado_por_unidade));
                break;
            case MODO_VOLUME:
                editTxtVolume.setVisibility(View.VISIBLE);
                txtResultadoCalculado.setHint(getString(R.string.hint_resultado_por_volume));
                break;
        }
    }

    private void bloquearTrocaDeModo(boolean bloquear) {
        for (int i = 0; i < radioGroupTipoConversao.getChildCount(); i++) {
            radioGroupTipoConversao.getChildAt(i).setEnabled(!bloquear);
        }
        radioGroupTipoConversao.setAlpha(bloquear ? 0.5f : 1.0f);
    }

    private void calcularPrecoUnitario() {
        esconderTeclado();

        double precoTotal = extrairValorDouble(editTxtPrecoTotal);
        EditText campoAtivo = obterCampoAtivo();

        if (precoTotal <= 0) {
            editTxtPrecoTotal.setError("Informe o preço");
            return;
        }

        if (campoAtivo == null || extrairValorDouble(campoAtivo) <= 0) {
            if (campoAtivo != null) campoAtivo.setError("Informe a quantidade");
            return;
        }

        // 1. CAPTURAR dados antes de limpar
        String precoEtiqueta = editTxtPrecoTotal.getText().toString();
        String qtdEtiqueta = campoAtivo.getText().toString();
        double valorEntrada = extrairValorDouble(campoAtivo);
        double resultadoNumerico = precoTotal / valorEntrada;

        String label = obterLabelModo();
        String textoResultado = String.format(Locale.getDefault(), "R$ %.2f por %s", resultadoNumerico, label);

        // 2. EXIBIR E SALVAR
        txtResultadoCalculado.setText(textoResultado);
        adicionarResultadoAoHistorico(new ResultadoItem(
                textoResultado, resultadoNumerico, label, precoEtiqueta, qtdEtiqueta));

        // 3. TRAVAR MODO E LIMPAR
        bloquearTrocaDeModo(true);
        editTxtPrecoTotal.setText("");
        campoAtivo.setText("");
        editTxtPrecoTotal.requestFocus();

        Toast.makeText(this, "Comparação adicionada!", Toast.LENGTH_SHORT).show();
    }

    private double extrairValorDouble(EditText campo) {
        if (campo == null) return 0.0;
        String limpo = campo.getText().toString().replaceAll("[^0-9]", "");
        if (limpo.isEmpty()) return 0.0;

        double valor = Double.parseDouble(limpo);
        if (campo.getId() == R.id.editTxtPeso || campo.getId() == R.id.editTxtVolume) {
            return valor / 1000.0; // 3 casas
        } else if (campo.getId() == R.id.editTxtPreco) {
            return valor / 100.0;  // 2 casas
        }
        return valor; // Inteiro (Unid)
    }

    private void adicionarResultadoAoHistorico(ResultadoItem item) {
        if (listaHistoricoResultados.size() >= MAX_HISTORICO) {
            listaHistoricoResultados.remove(0);
        }
        listaHistoricoResultados.add(item);
        atualizarTabelaHistorico();
    }

    private void atualizarTabelaHistorico() {
        layoutHistoricoResultados.removeAllViews();
        if (listaHistoricoResultados.isEmpty()) return;

        double menorValor = Double.MAX_VALUE;
        if (listaHistoricoResultados.size() == 2) {
            for (ResultadoItem r : listaHistoricoResultados) {
                if (r.getValorUnitario() < menorValor) menorValor = r.getValorUnitario();
            }
        }

        for (ResultadoItem item : listaHistoricoResultados) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(32, 24, 32, 24);

            TextView tvEtiqueta = new TextView(this);
            tvEtiqueta.setText("Etiqueta: " + item.getPrecoOriginalFormatado() + " por " + item.getQuantidadeOriginal());
            tvEtiqueta.setTextSize(14);

            TextView tvCalculo = new TextView(this);
            tvCalculo.setText(item.getDescricao());
            tvCalculo.setTextSize(18);
            tvCalculo.setTypeface(null, Typeface.BOLD);

            if (listaHistoricoResultados.size() == 2 && item.getValorUnitario() == menorValor) {
                itemLayout.setBackgroundResource(R.drawable.bg_melhor_opcao);
                tvCalculo.setTextColor(ContextCompat.getColor(this, R.color.verde_acao));
                TextView tvBadge = new TextView(this);
                tvBadge.setText("★ MELHOR CUSTO-BENEFÍCIO");
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.verde_acao));
                tvBadge.setTextSize(12);
                tvBadge.setTypeface(null, Typeface.BOLD);
                itemLayout.addView(tvBadge);
            }

            itemLayout.addView(tvEtiqueta);
            itemLayout.addView(tvCalculo);
            layoutHistoricoResultados.addView(itemLayout);
        }
    }

    private EditText obterCampoAtivo() {
        if (modoAtual == MODO_PESO) return editTxtPeso;
        if (modoAtual == MODO_UNIDADE) return editTxtUnid;
        if (modoAtual == MODO_VOLUME) return editTxtVolume;
        return null;
    }

    private String obterLabelModo() {
        if (modoAtual == MODO_PESO) return "Kg";
        if (modoAtual == MODO_UNIDADE) return "Unid.";
        if (modoAtual == MODO_VOLUME) return "Litro";
        return "";
    }

    private void limparTodosOsCampos() {
        editTxtPrecoTotal.setText("");
        limparCamposEspecificos();
        txtResultadoCalculado.setText("");
    }

    private void limparCamposEspecificos() {
        editTxtPeso.setText("");
        editTxtUnid.setText("");
        editTxtVolume.setText("");
        editTxtPeso.setError(null);
        editTxtUnid.setError(null);
        editTxtVolume.setError(null);
    }

    private void esconderTeclado() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private class MascaraFinanceira implements TextWatcher {
        private final EditText campo;
        private final int tipo;
        private String atual = "";

        public MascaraFinanceira(EditText campo, int tipo) {
            this.campo = campo;
            this.tipo = tipo;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0 || s.toString().equals(atual)) return;
            campo.removeTextChangedListener(this);

            String limpo = s.toString().replaceAll("[^0-9]", "");
            if (!limpo.isEmpty()) {
                double val = Double.parseDouble(limpo);
                Locale br = new Locale("pt", "BR");
                if (tipo == 0) atual = NumberFormat.getCurrencyInstance(br).format(val / 100.0);
                else if (tipo == 1) atual = String.format(br, "%,.3f", val / 1000.0);
                else atual = String.format(br, "%,.0f", val);

                campo.setText(atual);
                campo.setSelection(atual.length());
            } else {
                atual = "";
                campo.setText("");
            }
            campo.addTextChangedListener(this);
        }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_MODO_ATUAL, modoAtual);
        outState.putParcelableArrayList(KEY_HISTORICO_RESULTADOS, listaHistoricoResultados);
    }
}