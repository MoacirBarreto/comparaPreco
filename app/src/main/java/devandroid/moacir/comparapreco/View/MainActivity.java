package devandroid.moacir.comparapreco.View;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import devandroid.moacir.converteunidades.Model.ResultadoItem;
import devandroid.moacir.comparapreco.R;

public class MainActivity extends AppCompatActivity {

    private RadioGroup radioGroupTipoConversao;
    private EditText editTxtPrecoTotal;
    private EditText editTxtPeso;
    private EditText editTxtUnid;
    private EditText editTxtVolume;
    private Button btnCalcular;
    private TextView txtResultadoCalculado;

    private LinearLayout layoutHistoricoResultados; // Container para a tabela de histórico

    private ArrayList<ResultadoItem> listaHistoricoResultados;
    private static final int MAX_HISTORICO = 6; // Número máximo de itens no histórico

    // ... (suas constantes MODO_ e KEY_ existentes) ...
    private static final String KEY_HISTORICO_RESULTADOS = "historicoResultados";

    // Constantes para os modos de conversão
    private static final int MODO_NAO_SELECIONADO = -1;
    private static final int MODO_PESO = 1;
    private static final int MODO_UNIDADE = 2;
    private static final int MODO_VOLUME = 3;

    private int modoAtual = MODO_NAO_SELECIONADO; // Inicializa com um valor padrão

    // Chaves para salvar/restaurar o estado
    private static final String KEY_MODO_ATUAL = "modoAtual";
    private static final String KEY_PRECO_TOTAL = "precoTotal"; // Embora EditText salve seu texto, podemos ser explícitos
    private static final String KEY_VALOR_PESO = "valorPeso";   // se necessário ou para outros usos.
    private static final String KEY_VALOR_UNID = "valorUnid";
    private static final String KEY_VALOR_VOLUME = "valorVolume";
    private static final String KEY_RESULTADO = "resultado";
    private static final String KEY_RADIO_SELECIONADO_ID = "radioSelecionadoId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialização das Views
        radioGroupTipoConversao = findViewById(R.id.radioGroupTipoConversao);
        editTxtPrecoTotal = findViewById(R.id.editTxtPreco);
        editTxtPeso = findViewById(R.id.editTxtPeso);
        editTxtUnid = findViewById(R.id.editTxtUnid);
        editTxtVolume = findViewById(R.id.editTxtVolume);
        btnCalcular = findViewById(R.id.btnCalcular);
        txtResultadoCalculado = findViewById(R.id.editTxtResultado);
        layoutHistoricoResultados = findViewById(R.id.layoutHistoricoResultados);

        if (savedInstanceState != null) {
            // Restaura o estado da lógica do aplicativo
            modoAtual = savedInstanceState.getInt(KEY_MODO_ATUAL, MODO_NAO_SELECIONADO);
            int radioIdSalvo = savedInstanceState.getInt(KEY_RADIO_SELECIONADO_ID, -1);

            if (radioIdSalvo != -1 && radioGroupTipoConversao.getCheckedRadioButtonId() != radioIdSalvo) {
                radioGroupTipoConversao.check(radioIdSalvo);
            }
            // Restaurar histórico
            if (savedInstanceState.containsKey(KEY_HISTORICO_RESULTADOS)) {
                listaHistoricoResultados = savedInstanceState.getParcelableArrayList(KEY_HISTORICO_RESULTADOS);
            } else {
                listaHistoricoResultados = new ArrayList<>();
            }

        } else {
            listaHistoricoResultados = new ArrayList<>();
        }
        // Configurar estado inicial da UI ou o estado restaurado.
        atualizarUiComBaseNaSelecaoDoRadio();
        atualizarTabelaHistorico(); // Atualiza a tabela com dados restaurados ou vazia

        radioGroupTipoConversao.setOnCheckedChangeListener((group, checkedId) -> atualizarUiComBaseNaSelecaoDoRadio());

        btnCalcular.setOnClickListener(v -> calcularPrecoUnitario());

        Button btnLimpar = findViewById(R.id.btnLimpar);
        btnLimpar.setOnClickListener(v -> limparCampos(true));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Salva os dados que você precisa para restaurar o estado da lógica do aplicativo
        outState.putInt(KEY_MODO_ATUAL, modoAtual);
        outState.putInt(KEY_RADIO_SELECIONADO_ID, radioGroupTipoConversao.getCheckedRadioButtonId());

        outState.putString(KEY_PRECO_TOTAL, editTxtPrecoTotal.getText().toString());
        outState.putString(KEY_VALOR_PESO, editTxtPeso.getText().toString());
        outState.putString(KEY_VALOR_UNID, editTxtUnid.getText().toString());
        outState.putString(KEY_VALOR_VOLUME, editTxtVolume.getText().toString());
        outState.putString(KEY_RESULTADO, txtResultadoCalculado.getText().toString());
        outState.putParcelableArrayList(KEY_HISTORICO_RESULTADOS, listaHistoricoResultados);
    }

    private void adicionarResultadoAoHistorico(ResultadoItem item) {
        if (listaHistoricoResultados.size() >= MAX_HISTORICO) {
            listaHistoricoResultados.remove(0); // Remove o mais antigo se a lista estiver cheia
        }
        listaHistoricoResultados.add(item);
        atualizarTabelaHistorico();
    }

    private void atualizarTabelaHistorico() {
        layoutHistoricoResultados.removeAllViews(); // Limpa a tabela antiga

        if (listaHistoricoResultados.isEmpty()) {
            TextView txtVazio = new TextView(this);
            txtVazio.setText("Sem histórico");
            txtVazio.setGravity(Gravity.CENTER);
            txtVazio.setPadding(0, 16, 0, 16);
            layoutHistoricoResultados.addView(txtVazio);
            return;
        }

        // Cabeçalho (Opcional, mas bom para clareza)
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setPadding(8, 8, 8, 8);

        // Adiciona uma linha divisória
        View separadorHeader = new View(this);
        separadorHeader.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
        separadorHeader.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        layoutHistoricoResultados.addView(separadorHeader);

        // Exibe os itens em ordem reversa (mais recente primeiro)
        List<ResultadoItem> listaParaExibir = new ArrayList<>(listaHistoricoResultados);
        Collections.reverse(listaParaExibir);

        for (ResultadoItem item : listaParaExibir) {
            LinearLayout linhaLayout = new LinearLayout(this);
            linhaLayout.setOrientation(LinearLayout.HORIZONTAL);
            linhaLayout.setPadding(8, 8, 8, 8); // Padding para cada linha

            TextView txtDescricaoItem = new TextView(this);
            txtDescricaoItem.setText(item.getDescricao());
            // Definindo peso para que as colunas se alinhem
            txtDescricaoItem.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); // Peso 1
            txtDescricaoItem.setGravity(Gravity.CENTER);

            linhaLayout.addView(txtDescricaoItem);
            //linhaLayout.addView(txtTipoItem);

            layoutHistoricoResultados.addView(linhaLayout);

            // Adiciona uma linha divisória
            View separador = new View(this);
            separador.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)); // Altura da linha
            separador.setBackgroundColor(ContextCompat.getColor(this, com.google.android.material.R.color.material_dynamic_neutral90)); // Uma cor clara
            layoutHistoricoResultados.addView(separador);
        }
    }

    private void atualizarUiComBaseNaSelecaoDoRadio() {
        int selectedId = radioGroupTipoConversao.getCheckedRadioButtonId();
        if (selectedId == R.id.radioBtnPeso) { // Supondo que você renomeou o ID no XML
            modoAtual = MODO_PESO;
            editTxtPeso.setVisibility(View.VISIBLE);
            editTxtUnid.setVisibility(View.GONE);
            editTxtVolume.setVisibility(View.GONE);
            if (editTxtUnid.hasFocus() || editTxtUnid.getText().length() > 0 || editTxtVolume.hasFocus() || editTxtVolume.getText().length() > 0) { // Limpa se tinha foco ou texto
                editTxtUnid.getText().clear();
                editTxtVolume.getText().clear();
            }
            txtResultadoCalculado.setHint(R.string.hint_resultado_por_kg);
        } else if (selectedId == R.id.radioBtnUnidades) { // Supondo que você renomeou o ID no XML
            modoAtual = MODO_UNIDADE;
            editTxtPeso.setVisibility(View.GONE);
            editTxtUnid.setVisibility(View.VISIBLE);
            editTxtVolume.setVisibility(View.GONE);
            if (editTxtPeso.hasFocus() || editTxtPeso.getText().length() > 0 || editTxtVolume.hasFocus() || editTxtVolume.getText().length() > 0) {
                editTxtPeso.getText().clear();
                editTxtVolume.getText().clear();
            }
            txtResultadoCalculado.setHint(R.string.hint_resultado_por_unidade);
        } else if (selectedId == R.id.radioBtnVolume) {
            modoAtual = MODO_VOLUME;
            editTxtPeso.setVisibility(View.GONE);
            editTxtUnid.setVisibility(View.GONE);
            editTxtVolume.setVisibility(View.VISIBLE);

            if (editTxtPeso.hasFocus() || editTxtPeso.getText().length() > 0 || editTxtUnid.hasFocus() || editTxtUnid.getText().length() > 0) {
                editTxtPeso.getText().clear();
                editTxtUnid.getText().clear();
            }
            txtResultadoCalculado.setHint(R.string.hint_resultado_por_volume);
        }   else {
            modoAtual = MODO_NAO_SELECIONADO;
            editTxtPeso.setVisibility(View.GONE);
            editTxtUnid.setVisibility(View.GONE);
            editTxtVolume.setVisibility(View.GONE);
            txtResultadoCalculado.setHint(R.string.hint_Resultado);
        }
        // Não limpa o txtResultadoCalculado aqui para permitir que o valor restaurado permaneça
        // txtResultadoCalculado.setText("");

    }

    private void calcularPrecoUnitario() {
        String strPrecoTotal = editTxtPrecoTotal.getText().toString();

        if (strPrecoTotal.isEmpty()) { /* ... erro ... */
            return;
        }
        double precoTotal;
        try {
            precoTotal = Double.parseDouble(strPrecoTotal);
            if (precoTotal < 0) { /* ... erro ... */
                return;
            }
        } catch (NumberFormatException e) { /* ... erro ... */
            return;
        }


        double valorEntrada;
        String strValorEntrada;
        double resultadoNumerico = 0;
        String tipoCalculo = ""; // Para ResultadoItem
        String textoResultadoComTipo = ""; // Para txtResultadoCalculado

        if (modoAtual == MODO_PESO) {
            strValorEntrada = editTxtPeso.getText().toString();
            if (strValorEntrada.isEmpty()) { /* ... erro ... */
                return;
            }
            try {
                valorEntrada = Double.parseDouble(strValorEntrada);
                if (valorEntrada <= 0) { /* ... erro ... */
                    return;
                }
                resultadoNumerico = precoTotal / valorEntrada;
                tipoCalculo = "Peso";
                textoResultadoComTipo = String.format(Locale.getDefault(), "R$ %.2f por Kg", resultadoNumerico);
            } catch (NumberFormatException e) { /* ... erro ... */
                return;
            }
        } else if (modoAtual == MODO_UNIDADE) {
            strValorEntrada = editTxtUnid.getText().toString();
            if (strValorEntrada.isEmpty()) { /* ... erro ... */
                return;
            }
            try {
                valorEntrada = Double.parseDouble(strValorEntrada);
                if (valorEntrada <= 0) { /* ... erro ... */
                    return;
                }
                resultadoNumerico = precoTotal / valorEntrada;
                tipoCalculo = "Unidade";
                textoResultadoComTipo = String.format(Locale.getDefault(), "R$ %.2f por Unid.", resultadoNumerico);
            } catch (NumberFormatException e) { /* ... erro ... */
                return;
            }
        } else if (modoAtual == MODO_VOLUME) {
            strValorEntrada = editTxtVolume.getText().toString();
            if (strValorEntrada.isEmpty()) { /* ... erro ... */
                return;
            }
            try {
                valorEntrada = Double.parseDouble(strValorEntrada);
                if (valorEntrada <= 0) { /* ... erro ... */
                    return;
                }
                resultadoNumerico = precoTotal / valorEntrada;
                tipoCalculo = "litro";
                textoResultadoComTipo = String.format(Locale.getDefault(), "R$ %.2f por Litro", resultadoNumerico);
            } catch (NumberFormatException e) { /* ... erro ... */
                return;
            }
        }   else {
            Toast.makeText(this, R.string.erro_selecione_modo, Toast.LENGTH_SHORT).show();
            return;
        }

        txtResultadoCalculado.setText(textoResultadoComTipo);
        adicionarResultadoAoHistorico(new ResultadoItem(textoResultadoComTipo, resultadoNumerico, tipoCalculo));

        // Limpar erros após cálculo bem-sucedido
        editTxtPrecoTotal.setError(null);
        editTxtPeso.setError(null);
        editTxtUnid.setError(null);
        editTxtVolume.setError(null);
    }

    private void limparCampos(boolean b) {
        editTxtPrecoTotal.setText("");
        editTxtPeso.setText("");
        editTxtUnid.setText("");
        editTxtVolume.setText("");
        txtResultadoCalculado.setText("");
        txtResultadoCalculado.setHint(R.string.hint_Resultado); // Restaura hint padrão

        // Limpa erros de validação
        editTxtPrecoTotal.setError(null);
        editTxtPeso.setError(null);
        editTxtUnid.setError(null);
        editTxtVolume.setError(null);

        // Opcional: redefinir a seleção do RadioGroup para o padrão, se desejado.
        // Se você fizer isso, o listener onCheckedChanged será acionado e atualizará a UI.
        // radioGroupTipoConversao.check(R.id.radioPrecoPorUnidade); // Exemplo de redefinir para Unidade
        // ou
        // radioGroupTipoConversao.clearCheck(); // Limpa a seleção, disparando onCheckedChanged
        //                                      // e o modo voltará para MODO_NAO_SELECIONADO
        // Se você não redefinir o RadioGroup aqui, a seleção atual permanecerá.
    }
}
