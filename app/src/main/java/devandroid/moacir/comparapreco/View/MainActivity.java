package devandroid.moacir.comparapreco.View;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import devandroid.moacir.comparapreco.Model.ResultadoItem;
import devandroid.moacir.comparapreco.R;

public class MainActivity extends AppCompatActivity {

    private RadioGroup radioGroupTipoConversao;
    private EditText editTxtPrecoTotal, editTxtPeso, editTxtUnid, editTxtVolume;
    private Button btnCalcular, btnLimpar;
    private TextView txtResultadoCalculado;
    private LinearLayout layoutHistoricoResultados;

    private ArrayList<ResultadoItem> listaHistoricoResultados;
    private Uri fotoUri;

    private static final int MAX_HISTORICO = 2;
    private static final int MODO_PESO = 1;
    private static final int MODO_UNIDADE = 2;
    private static final int MODO_VOLUME = 3;

    private int modoAtual = MODO_PESO;

    private static final String KEY_HISTORICO_RESULTADOS = "historicoResultados";
    private static final String KEY_MODO_ATUAL = "modoAtual";

    // 1. Launcher para permissão
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) executarIntentCamera();
                else Toast.makeText(this, "Permissão de câmera negada.", Toast.LENGTH_LONG).show();
            });

    // 2. Launcher para Câmera em Alta Resolução
    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && fotoUri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fotoUri);
                        reconhecerTexto(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

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

        // Clique no ícone da câmera no campo de preço
        editTxtPrecoTotal.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableRight = 2;
                if (editTxtPrecoTotal.getCompoundDrawables()[drawableRight] != null) {
                    if (event.getRawX() >= (editTxtPrecoTotal.getRight() - editTxtPrecoTotal.getCompoundDrawables()[drawableRight].getBounds().width())) {
                        abrirCamera();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void abrirCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            executarIntentCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void executarIntentCamera() {
        try {
            File imageFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "scan_temp.jpg");
            fotoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
            cameraLauncher.launch(fotoUri);
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao criar arquivo de imagem", Toast.LENGTH_SHORT).show();
        }
    }

    private void reconhecerTexto(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        com.google.mlkit.vision.text.TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> processarTextoExtraido(visionText.getText()))
                .addOnFailureListener(e -> Toast.makeText(this, "Erro no scanner", Toast.LENGTH_SHORT).show());
    }

    private void processarTextoExtraido(String texto) {
        String textoLimpo = texto.toLowerCase().replace("\n", " ");

        // Regex Preço
        Pattern precoPattern = Pattern.compile("(?:r\\$|\\$)?\\s?(\\d{1,3}[.,]\\d{2})");
        Matcher precoMatcher = precoPattern.matcher(textoLimpo);
        if (precoMatcher.find()) {
            String precoStr = precoMatcher.group(1).replaceAll("[^0-9]", "");
            editTxtPrecoTotal.setText(precoStr);
        }

        // Regex Medidas (Peso, Volume, Unidade)
        Pattern medidaPattern = Pattern.compile("(\\d+[.,]?\\d*)\\s*(kg|g|l|ml|un|unid)");
        Matcher medidaMatcher = medidaPattern.matcher(textoLimpo);

        if (medidaMatcher.find()) {
            String valorStr = medidaMatcher.group(1).replace(",", ".");
            String unidade = medidaMatcher.group(2);
            double valor = Double.parseDouble(valorStr);

            if (unidade.contains("kg") || unidade.equals("l")) {
                modoAtual = unidade.contains("kg") ? MODO_PESO : MODO_VOLUME;
                radioGroupTipoConversao.check(unidade.contains("kg") ? R.id.radioBtnPeso : R.id.radioBtnVolume);
                EditText campo = unidade.contains("kg") ? editTxtPeso : editTxtVolume;
                campo.setText(String.valueOf((int)(valor * 1000)));
            } else if (unidade.equals("g") || unidade.equals("ml")) {
                modoAtual = unidade.equals("g") ? MODO_PESO : MODO_VOLUME;
                radioGroupTipoConversao.check(unidade.equals("g") ? R.id.radioBtnPeso : R.id.radioBtnVolume);
                EditText campo = unidade.equals("g") ? editTxtPeso : editTxtVolume;
                campo.setText(String.valueOf((int)valor));
            } else if (unidade.startsWith("un")) {
                modoAtual = MODO_UNIDADE;
                radioGroupTipoConversao.check(R.id.radioBtnUnidades);
                editTxtUnid.setText(String.valueOf((int)valor));
            }
            atualizarVisibilidadeCampos();
        }
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
        editTxtPrecoTotal.addTextChangedListener(new MascaraFinanceira(editTxtPrecoTotal, 0));
        editTxtPeso.addTextChangedListener(new MascaraFinanceira(editTxtPeso, 1));
        editTxtVolume.addTextChangedListener(new MascaraFinanceira(editTxtVolume, 1));
        editTxtUnid.addTextChangedListener(new MascaraFinanceira(editTxtUnid, 2));
    }

    private void configurarListeners() {
        radioGroupTipoConversao.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBtnPeso) modoAtual = MODO_PESO;
            else if (checkedId == R.id.radioBtnUnidades) modoAtual = MODO_UNIDADE;
            else if (checkedId == R.id.radioBtnVolume) modoAtual = MODO_VOLUME;
            limparCamposEspecificos();
            atualizarVisibilidadeCampos();
        });

        btnCalcular.setOnClickListener(v -> calcularPrecoUnitario());
        btnLimpar.setOnClickListener(v -> {
            limparTodosOsCampos();
            listaHistoricoResultados.clear();
            atualizarTabelaHistorico();
            bloquearTrocaDeModo(false);
        });
    }

    private void atualizarVisibilidadeCampos() {
        editTxtPeso.setVisibility(View.GONE);
        editTxtUnid.setVisibility(View.GONE);
        editTxtVolume.setVisibility(View.GONE);

        if (modoAtual == MODO_PESO) {
            editTxtPeso.setVisibility(View.VISIBLE);
            txtResultadoCalculado.setHint(getString(R.string.hint_resultado_por_kg));
        } else if (modoAtual == MODO_UNIDADE) {
            editTxtUnid.setVisibility(View.VISIBLE);
            txtResultadoCalculado.setHint(getString(R.string.hint_resultado_por_unidade));
        } else {
            editTxtVolume.setVisibility(View.VISIBLE);
            txtResultadoCalculado.setHint(getString(R.string.hint_resultado_por_volume));
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

        if (precoTotal <= 0 || campoAtivo == null || extrairValorDouble(campoAtivo) <= 0) {
            Toast.makeText(this, "Preencha os valores corretamente", Toast.LENGTH_SHORT).show();
            return;
        }

        String precoEtiqueta = editTxtPrecoTotal.getText().toString();
        String qtdEtiqueta = campoAtivo.getText().toString();
        double valorEntrada = extrairValorDouble(campoAtivo);
        double resultadoNumerico = precoTotal / valorEntrada;

        String label = obterLabelModo();
        String textoResultado = String.format(Locale.getDefault(), "R$ %.2f por %s", resultadoNumerico, label);

        txtResultadoCalculado.setText(textoResultado);
        adicionarResultadoAoHistorico(new ResultadoItem(textoResultado, resultadoNumerico, label, precoEtiqueta, qtdEtiqueta));

        bloquearTrocaDeModo(true);
        editTxtPrecoTotal.setText("");
        campoAtivo.setText("");
        editTxtPrecoTotal.requestFocus();
    }

    private double extrairValorDouble(EditText campo) {
        if (campo == null) return 0.0;
        String limpo = campo.getText().toString().replaceAll("[^0-9]", "");
        if (limpo.isEmpty()) return 0.0;
        double valor = Double.parseDouble(limpo);
        if (campo.getId() == R.id.editTxtPeso || campo.getId() == R.id.editTxtVolume) return valor / 1000.0;
        if (campo.getId() == R.id.editTxtPreco) return valor / 100.0;
        return valor;
    }

    private void adicionarResultadoAoHistorico(ResultadoItem item) {
        if (listaHistoricoResultados.size() >= MAX_HISTORICO) listaHistoricoResultados.remove(0);
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

            TextView tvCalculo = new TextView(this);
            tvCalculo.setText(item.getDescricao());
            tvCalculo.setTypeface(null, Typeface.BOLD);

            if (listaHistoricoResultados.size() == 2 && item.getValorUnitario() == menorValor) {
                itemLayout.setBackgroundResource(R.drawable.bg_melhor_opcao);
                tvCalculo.setTextColor(ContextCompat.getColor(this, R.color.verde_acao));
            }

            itemLayout.addView(tvEtiqueta);
            itemLayout.addView(tvCalculo);
            layoutHistoricoResultados.addView(itemLayout);
        }
    }

    private EditText obterCampoAtivo() {
        if (modoAtual == MODO_PESO) return editTxtPeso;
        if (modoAtual == MODO_UNIDADE) return editTxtUnid;
        return editTxtVolume;
    }

    private String obterLabelModo() {
        if (modoAtual == MODO_PESO) return "Kg";
        if (modoAtual == MODO_UNIDADE) return "Unid.";
        return "Litro";
    }

    private void limparTodosOsCampos() {
        editTxtPrecoTotal.setText("");
        limparCamposEspecificos();
        txtResultadoCalculado.setText("");
    }

    private void limparCamposEspecificos() {
        editTxtPeso.setText(""); editTxtUnid.setText(""); editTxtVolume.setText("");
        editTxtPeso.setError(null); editTxtUnid.setError(null); editTxtVolume.setError(null);
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

        public MascaraFinanceira(EditText campo, int tipo) { this.campo = campo; this.tipo = tipo; }

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
                atual = ""; campo.setText("");
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