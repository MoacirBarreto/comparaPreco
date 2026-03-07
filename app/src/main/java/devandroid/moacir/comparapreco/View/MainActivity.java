package devandroid.moacir.comparapreco.View;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    // AJUSTADO: Agora o histórico guarda apenas as últimas 2 comparações
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
        atualizarUiComBaseNaSelecaoDoRadio();
        atualizarTabelaHistorico();
        verificarInstrucoes();

    }


    private void verificarInstrucoes() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean mostrarNovamente = settings.getBoolean(PREF_MOSTRAR_INSTRUCOES, true);

        if (mostrarNovamente) {
            exibirDialogoInstrucoes();
        }
    }

    private void exibirDialogoInstrucoes() {
        // Criamos um CheckBox via código para o diálogo
        final CheckBox checkBox = new CheckBox(this);
        checkBox.setText(R.string.instrucoes_check);
        checkBox.setPadding(40, 20, 0, 20);

        // Criamos o layout do alerta
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(checkBox);

        new AlertDialog.Builder(this)
                .setTitle(R.string.instrucoes_titulo)
                .setMessage(R.string.instrucoes_texto)
                .setView(layout) // Adiciona o checkbox ao diálogo
                .setCancelable(false)
                .setPositiveButton(R.string.instrucoes_botao, (dialog, which) -> {
                    // Se o usuário marcou o check, salvamos para não mostrar mais
                    if (checkBox.isChecked()) {
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean(PREF_MOSTRAR_INSTRUCOES, false);
                        editor.apply();
                    }
                })
                .show();
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

        Button btnLimpar = findViewById(R.id.btnLimpar);
        btnLimpar.setOnClickListener(v -> {
            limparCampos();
            // Opcional: Limpar histórico ao clicar em limpar tudo
            listaHistoricoResultados.clear();
            atualizarTabelaHistorico();
        });
    }

    private void calcularPrecoUnitario() {
        esconderTeclado();

        String strPreco = editTxtPrecoTotal.getText().toString();
        if (strPreco.isEmpty()) {
            editTxtPrecoTotal.setError("Informe o preço");
            return;
        }

        double precoTotal = Double.parseDouble(strPreco);
        double valorEntrada = 0;
        String label = "";

        EditText campoAtivo = null;
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
            Toast.makeText(this, "Selecione um modo de conversão", Toast.LENGTH_SHORT).show();
            return;
        }

        String strEntrada = campoAtivo.getText().toString();
        if (strEntrada.isEmpty()) {
            campoAtivo.setError("Informe o valor");
            return;
        }

        valorEntrada = Double.parseDouble(strEntrada);
        if (valorEntrada <= 0) {
            campoAtivo.setError("Valor deve ser maior que zero");
            return;
        }

        double resultadoNumerico = precoTotal / valorEntrada;
        String textoResultado = String.format(Locale.getDefault(), "R$ %.2f por %s", resultadoNumerico, label);

        txtResultadoCalculado.setText(textoResultado);
        adicionarResultadoAoHistorico(new ResultadoItem(textoResultado, resultadoNumerico, label));
    }

    private void adicionarResultadoAoHistorico(ResultadoItem item) {
        // Se já tiver 2 itens, remove o mais antigo para dar lugar ao novo
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

        // Encontrar o menor valor entre os dois para destacar
        double menorValor = Double.MAX_VALUE;
        for (ResultadoItem item : listaHistoricoResultados) {
            if (item.getValorUnitario() < menorValor) {
                menorValor = item.getValorUnitario();
            }
        }

        // Mostra o mais recente primeiro (Inverte a lista de 2 itens)
        List<ResultadoItem> listaParaExibir = new ArrayList<>(listaHistoricoResultados);
        Collections.reverse(listaParaExibir);

        for (ResultadoItem item : listaParaExibir) {
            LinearLayout containerLinha = new LinearLayout(this);
            containerLinha.setOrientation(LinearLayout.VERTICAL);
            containerLinha.setPadding(40, 40, 40, 40); // Aumentado o padding para melhor toque

            TextView tvDescricao = new TextView(this);
            tvDescricao.setText(item.getDescricao());
            tvDescricao.setTextSize(20); // Texto um pouco maior para facilitar a leitura

            // DESTAQUE: Se houver 2 itens e este for o mais barato, pinta de verde
            if (listaHistoricoResultados.size() == 2 && item.getValorUnitario() == menorValor) {
                containerLinha.setBackgroundColor(Color.parseColor("#E8F5E9")); // Verde claro
                tvDescricao.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                tvDescricao.setTypeface(null, android.graphics.Typeface.BOLD);

                // Adiciona um selo de "MAIS BARATO"
                TextView tvMelhorPreco = new TextView(this);
                tvMelhorPreco.setText("★ MELHOR OPÇÃO");
                tvMelhorPreco.setTextSize(12);
                tvMelhorPreco.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
                containerLinha.addView(tvMelhorPreco);
            } else {
                containerLinha.setBackgroundColor(Color.TRANSPARENT);
                tvDescricao.setTextColor(Color.BLACK);
            }

            containerLinha.addView(tvDescricao, 0); // Adiciona a descrição acima do selo
            layoutHistoricoResultados.addView(containerLinha);

            // Divisor entre os dois itens
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
        } else {
            modoAtual = MODO_NAO_SELECIONADO;
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
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_MODO_ATUAL, modoAtual);
        outState.putParcelableArrayList(KEY_HISTORICO_RESULTADOS, listaHistoricoResultados);
    }
}