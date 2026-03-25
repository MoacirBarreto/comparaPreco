package devandroid.moacir.comparapreco.View;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    private Button btnCalcular;
    private TextView txtResultadoCalculado;
    private LinearLayout layoutHistoricoResultados;

    private ArrayList<ResultadoItem> listaHistoricoResultados;

    private static final int MAX_HISTORICO = 2;
    private static final int MODO_NAO_SELECIONADO = -1;
    private static final int MODO_PESO = 1;
    private static final int MODO_UNIDADE = 2;
    private static final int MODO_VOLUME = 3;

    private int modoAtual = MODO_NAO_SELECIONADO;

    private static final String KEY_HISTORICO_RESULTADOS = "historicoResultados";
    private static final String KEY_MODO_ATUAL = "modoAtual";
    private static final String PREFS_NAME = "ConfiguracoesApp";
    private static final String PREF_MOSTRAR_INSTRUCOES = "mostrarInstrucoes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarComponentes();

        if (savedInstanceState != null) {
            modoAtual = savedInstanceState.getInt(KEY_MODO_ATUAL, MODO_NAO_SELECIONADO);
            listaHistoricoResultados = savedInstanceState.getParcelableArrayList(KEY_HISTORICO_RESULTADOS);
        } else {
            listaHistoricoResultados = new ArrayList<>();
        }

        configurarListeners();
        configurarMascaraFinanceira();
        atualizarUiComBaseNaSelecaoDoRadio();
        atualizarTabelaHistorico();
        verificarInstrucoes();
    }

    private void configurarMascaraFinanceira() {
        editTxtPrecoTotal.addTextChangedListener(new MascaraFinanceira(editTxtPrecoTotal, true));
        editTxtPeso.addTextChangedListener(new MascaraFinanceira(editTxtPeso, false));
        editTxtUnid.addTextChangedListener(new MascaraFinanceira(editTxtUnid, false));
        editTxtVolume.addTextChangedListener(new MascaraFinanceira(editTxtVolume, false));
    }

    private void verificarInstrucoes() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.getBoolean(PREF_MOSTRAR_INSTRUCOES, true)) {
            exibirDialogoInstrucoes();
        }
    }

    private void exibirDialogoInstrucoes() {
        final CheckBox checkBox = new CheckBox(this);
        checkBox.setText(R.string.instrucoes_check);
        checkBox.setPadding(40, 20, 0, 20);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(checkBox);

        new AlertDialog.Builder(this)
                .setTitle(R.string.instrucoes_titulo)
                .setMessage(R.string.instrucoes_texto)
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(R.string.instrucoes_botao, (dialog, which) -> {
                    if (checkBox.isChecked()) {
                        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
                        editor.putBoolean(PREF_MOSTRAR_INSTRUCOES, false);
                        editor.apply();
                    }
                }).show();
    }

    private void inicializarComponentes() {
        radioGroupTipoConversao = findViewById(R.id.radioGroupTipoConversao);
        editTxtPrecoTotal = findViewById(R.id.editTxtPreco);
        editTxtPeso = findViewById(R.id.editTxtPeso);
        editTxtUnid = findViewById(R.id.editTxtUnid);
        editTxtVolume = findViewById(R.id.editTxtVolume);
        btnCalcular = findViewById(R.id.btnCalcular);
        txtResultadoCalculado = findViewById(R.id.editTxtResultado);
        layoutHistoricoResultados = findViewById(R.id.layoutHistoricoResultados);

        btnCalcular.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_blue));
        btnCalcular.setTextColor(Color.WHITE);
    }

    private void configurarListeners() {
        radioGroupTipoConversao.setOnCheckedChangeListener((group, checkedId) -> atualizarUiComBaseNaSelecaoDoRadio());
        btnCalcular.setOnClickListener(v -> calcularPrecoUnitario());

        findViewById(R.id.btnLimpar).setOnClickListener(v -> {
            limparCampos();
            listaHistoricoResultados.clear();
            atualizarTabelaHistorico();
        });
    }

    private void calcularPrecoUnitario() {
        esconderTeclado();

        String strPrecoRaw = editTxtPrecoTotal.getText().toString().replaceAll("[^0-9]", "");
        if (strPrecoRaw.isEmpty()) {
            editTxtPrecoTotal.setError("Informe o preço");
            return;
        }
        double precoTotal = Double.parseDouble(strPrecoRaw) / 100.0;

        EditText campoAtivo = null;
        String label = "";
        if (modoAtual == MODO_PESO) {
            campoAtivo = editTxtPeso;
            label = "Kg";
        } else if (modoAtual == MODO_UNIDADE) {
            campoAtivo = editTxtUnid;
            label = "Unid.";
        } else if (modoAtual == MODO_VOLUME) {
            campoAtivo = editTxtVolume;
            label = "Litro";
        }

        if (campoAtivo == null) {
            Toast.makeText(this, "Selecione um modo", Toast.LENGTH_SHORT).show();
            return;
        }

        String strEntrada = campoAtivo.getText().toString().replaceAll("[^0-9]", "");
        if (strEntrada.isEmpty()) {
            campoAtivo.setError("Informe o valor");
            return;
        }

        double valorEntrada = Double.parseDouble(strEntrada) / 100.0;
        if (valorEntrada <= 0) {
            campoAtivo.setError("Valor inválido");
            return;
        }

        double resultadoNumerico = precoTotal / valorEntrada;
        String textoResultado = String.format(Locale.getDefault(), "R$ %.2f por %s", resultadoNumerico, label);

        txtResultadoCalculado.setText(textoResultado);
        adicionarResultadoAoHistorico(new ResultadoItem(textoResultado, resultadoNumerico, label));
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

        if (listaHistoricoResultados.isEmpty()) {
            TextView tvVazio = new TextView(this);
            tvVazio.setText("Inicie uma comparação");
            tvVazio.setPadding(0, 30, 0, 30);
            tvVazio.setGravity(Gravity.CENTER);
            layoutHistoricoResultados.addView(tvVazio);
            return;
        }

        // Identifica o menor valor para destacar (mesmo que não esteja no topo)
        double menorValor = Double.MAX_VALUE;
        for (ResultadoItem item : listaHistoricoResultados) {
            if (item.getValorUnitario() < menorValor) {
                menorValor = item.getValorUnitario();
            }
        }

        // REMOVIDO: Collections.reverse(listaParaExibir)
        // Agora usamos a listaHistoricoResultados diretamente para manter a ordem de entrada
        for (ResultadoItem item : listaHistoricoResultados) {
            LinearLayout containerLinha = new LinearLayout(this);
            containerLinha.setOrientation(LinearLayout.VERTICAL);
            containerLinha.setPadding(40, 40, 40, 40);

            TextView tvDescricao = new TextView(this);
            tvDescricao.setText(item.getDescricao());
            tvDescricao.setTextSize(20);

            // Destaque visual para o mais barato (independente da posição)
            if (listaHistoricoResultados.size() == 2 && item.getValorUnitario() == menorValor) {
                containerLinha.setBackgroundColor(Color.parseColor("#E8F5E9")); // Fundo verde claro
                tvDescricao.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                tvDescricao.setTypeface(null, Typeface.BOLD);

                TextView tvMelhorPreco = new TextView(this);
                tvMelhorPreco.setText("★ MELHOR OPÇÃO");
                tvMelhorPreco.setTextSize(12);
                tvMelhorPreco.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                containerLinha.addView(tvMelhorPreco);
            } else {
                tvDescricao.setTextColor(Color.BLACK);
            }

            containerLinha.addView(tvDescricao, 0);
            layoutHistoricoResultados.addView(containerLinha);

            // Divisor entre os itens
            View divisor = new View(this);
            divisor.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 3));
            divisor.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_color));
            layoutHistoricoResultados.addView(divisor);
        }
    }


    private void atualizarUiComBaseNaSelecaoDoRadio() {
        int selectedId = radioGroupTipoConversao.getCheckedRadioButtonId();
        editTxtPeso.setVisibility(View.GONE);
        editTxtUnid.setVisibility(View.GONE);
        editTxtVolume.setVisibility(View.GONE);

        if (selectedId == R.id.radioBtnPeso) {
            modoAtual = MODO_PESO;
            editTxtPeso.setVisibility(View.VISIBLE);
            txtResultadoCalculado.setHint(R.string.hint_resultado_por_kg);
        } else if (selectedId == R.id.radioBtnUnidades) {
            modoAtual = MODO_UNIDADE;
            editTxtUnid.setVisibility(View.VISIBLE);
            txtResultadoCalculado.setHint(R.string.hint_resultado_por_unidade);
        } else if (selectedId == R.id.radioBtnVolume) {
            modoAtual = MODO_VOLUME;
            editTxtVolume.setVisibility(View.VISIBLE);
            txtResultadoCalculado.setHint(R.string.hint_resultado_por_volume);
        }
    }

    private void limparCampos() {
        editTxtPrecoTotal.setText("");
        editTxtPeso.setText("");
        editTxtUnid.setText("");
        editTxtVolume.setText("");
        txtResultadoCalculado.setText("");
        editTxtPrecoTotal.setError(null);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_MODO_ATUAL, modoAtual);
        outState.putParcelableArrayList(KEY_HISTORICO_RESULTADOS, listaHistoricoResultados);
    }

    // CLASSE DE MASCARA ÚNICA (Corrigida e Finalizada)
    private class MascaraFinanceira implements TextWatcher {
        private final EditText campo;
        private final boolean exibirMoeda;
        private String atual = "";

        public MascaraFinanceira(EditText campo, boolean exibirMoeda) {
            this.campo = campo;
            this.exibirMoeda = exibirMoeda;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!s.toString().equals(atual)) {
                campo.removeTextChangedListener(this);

                String limpo = s.toString().replaceAll("[^0-9]", "");

                if (!limpo.isEmpty()) {
                    try {
                        double valor = Double.parseDouble(limpo) / 100.0;
                        Locale localeBR = new Locale("pt", "BR");
                        if (exibirMoeda) {
                            atual = NumberFormat.getCurrencyInstance(localeBR).format(valor);
                        } else {
                            atual = String.format(localeBR, "%,.2f", valor);
                        }
                        campo.setText(atual);
                        campo.setSelection(atual.length());
                    } catch (NumberFormatException e) {
                        campo.setText("");
                    }
                } else {
                    campo.setText("");
                }

                campo.addTextChangedListener(this);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}