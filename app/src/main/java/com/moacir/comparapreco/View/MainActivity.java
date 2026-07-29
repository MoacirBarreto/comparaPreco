package com.moacir.comparapreco.View;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.moacir.comparapreco.Model.ResultadoItem;
import com.moacir.comparapreco.R;
import com.moacir.comparapreco.ViewModel.ComparacaoViewModel;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RadioGroup radioGroupTipoConversao;
    private EditText editTxtPrecoTotal, editTxtPeso, editTxtUnid, editTxtVolume;
    private Button btnCalcular, btnLimpar, btnCompartilhar;
    private com.google.android.material.switchmaterial.SwitchMaterial switchMetricaReduzida;
    private TextView txtResultadoCalculado, txtIndicadorPasso;
    private LinearLayout layoutHistoricoResultados;
    private ComparacaoViewModel viewModel;
    private Uri fotoUri;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) executarIntentCamera();
                else Toast.makeText(this, "Permissão de câmera negada.", Toast.LENGTH_LONG).show();
            });

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

        viewModel = new ViewModelProvider(this).get(ComparacaoViewModel.class);

        inicializarComponentes();
        verificarPrimeiroAcesso();
        configurarListeners();
        configurarMascaras();
        configurarObservadores();

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

    private void configurarObservadores() {
        viewModel.listaResultados.observe(this, this::atualizarTabelaHistorico);

        viewModel.modoAtual.observe(this, modo -> {
            // Sincroniza o RadioGroup se o valor for diferente (evita loops)
            int checkId = R.id.radioBtnPeso;
            if (modo == ComparacaoViewModel.MODO_UNIDADE) checkId = R.id.radioBtnUnidades;
            else if (modo == ComparacaoViewModel.MODO_VOLUME) checkId = R.id.radioBtnVolume;

            if (radioGroupTipoConversao.getCheckedRadioButtonId() != checkId) {
                radioGroupTipoConversao.check(checkId);
            }
            atualizarVisibilidadeCampos();
        });

        viewModel.isMetricaReduzida.observe(this, reduzida -> {
            // Sincroniza o Switch se o valor for diferente
            if (switchMetricaReduzida.isChecked() != reduzida) {
                switchMetricaReduzida.setChecked(reduzida);
            }
            atualizarVisibilidadeCampos();
        });

        viewModel.mensagemToast.observe(this, mensagem -> {
            if (mensagem != null) {
                Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.ocrResultado.observe(this, resultado -> {
            if (resultado != null) {
                exibirDialogoConfirmacaoOCR(resultado.preco, resultado.valor, resultado.unidade);
                viewModel.limparOcrResultado();
            }
        });
    }

    private void verificarPrimeiroAcesso() {
        SharedPreferences prefs = getSharedPreferences("ConfigPrecos", MODE_PRIVATE);
        boolean jaViuInstrucoes = prefs.getBoolean("jaViuInstrucoes", false);

        if (!jaViuInstrucoes) {
            exibirDialogoInstrucoes(prefs);
        }
    }

    private void exibirDialogoInstrucoes(SharedPreferences prefs) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_instrucoes, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.TemaDialogoArredondado).create();
        dialog.setView(dialogView);
        dialog.setCancelable(false);

        CheckBox checkBox = dialogView.findViewById(R.id.checkNaoMostrarNovamente);
        Button btnEntendi = dialogView.findViewById(R.id.btnEntendi);

        btnEntendi.setOnClickListener(v -> {
            if (checkBox.isChecked()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("jaViuInstrucoes", true);
                editor.apply();
            }
            dialog.dismiss();
        });

        dialog.show();
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
                .addOnSuccessListener(visionText -> viewModel.processarOCR(visionText.getText()))
                .addOnFailureListener(e -> Toast.makeText(this, "Erro no scanner", Toast.LENGTH_SHORT).show());
    }

    private void exibirDialogoConfirmacaoOCR(String preco, String valor, String unidade) {
        String precoFormatado = (preco != null) ? 
                NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(Double.parseDouble(preco) / 100.0) 
                : "---";
        String valorExibicao = (valor != null) ? valor : "";
        String unidadeExibicao = (unidade != null) ? unidade : "";

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ocr_dialogo_titulo)
                .setMessage(getString(R.string.ocr_dialogo_mensagem, precoFormatado, valorExibicao, unidadeExibicao))
                .setPositiveButton(R.string.ocr_confirmar, (dialog, which) -> {
                    if (preco != null) {
                        editTxtPrecoTotal.setText(preco);
                    }
                    if (valor != null) {
                        double v = Double.parseDouble(valor);
                        if (unidade.contains("kg") || unidade.equals("l")) {
                            viewModel.setModo(unidade.contains("kg") ? ComparacaoViewModel.MODO_PESO : ComparacaoViewModel.MODO_VOLUME);
                            radioGroupTipoConversao.check(unidade.contains("kg") ? R.id.radioBtnPeso : R.id.radioBtnVolume);
                            EditText campo = unidade.contains("kg") ? editTxtPeso : editTxtVolume;
                            campo.setText(String.valueOf((int) (v * 1000)));
                        } else if (unidade.equals("g") || unidade.equals("ml")) {
                            viewModel.setModo(unidade.equals("g") ? ComparacaoViewModel.MODO_PESO : ComparacaoViewModel.MODO_VOLUME);
                            radioGroupTipoConversao.check(unidade.equals("g") ? R.id.radioBtnPeso : R.id.radioBtnVolume);
                            EditText campo = unidade.equals("g") ? editTxtPeso : editTxtVolume;
                            campo.setText(String.valueOf((int) v));
                        } else if (unidade.startsWith("un")) {
                            viewModel.setModo(ComparacaoViewModel.MODO_UNIDADE);
                            radioGroupTipoConversao.check(R.id.radioBtnUnidades);
                            editTxtUnid.setText(String.valueOf((int) v));
                        }
                    }
                })
                .setNegativeButton(R.string.ocr_ignorar, null)
                .show();
    }

    private void vibrar() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(50);
            }
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
        btnCompartilhar = findViewById(R.id.btnCompartilhar);
        switchMetricaReduzida = findViewById(R.id.switchMetricaReduzida);
        txtResultadoCalculado = findViewById(R.id.editTxtResultado);
        txtIndicadorPasso = findViewById(R.id.txtIndicadorPasso);
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
            int novoModo = ComparacaoViewModel.MODO_PESO;
            if (checkedId == R.id.radioBtnUnidades) novoModo = ComparacaoViewModel.MODO_UNIDADE;
            else if (checkedId == R.id.radioBtnVolume) novoModo = ComparacaoViewModel.MODO_VOLUME;
            
            viewModel.setModo(novoModo);
            limparCamposEspecificos();
            vibrar();
        });

        switchMetricaReduzida.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setMetricaReduzida(isChecked);
            txtResultadoCalculado.setText("");
            btnCompartilhar.setVisibility(View.GONE);
            vibrar();
        });

        btnCalcular.setOnClickListener(v -> calcularPrecoUnitario());
        btnLimpar.setOnClickListener(v -> {
            limparTodosOsCampos();
            viewModel.limparHistorico();
            bloquearTrocaDeModo(false);
            btnCompartilhar.setVisibility(View.GONE);
            vibrar();
        });

        btnCompartilhar.setOnClickListener(v -> compartilharMelhorOpcao());
    }

    private void atualizarVisibilidadeCampos() {
        editTxtPeso.setVisibility(View.GONE);
        editTxtUnid.setVisibility(View.GONE);
        editTxtVolume.setVisibility(View.GONE);

        Integer modo = viewModel.modoAtual.getValue();
        if (modo == null) modo = ComparacaoViewModel.MODO_PESO;

        EditText campoAtivo;
        if (modo == ComparacaoViewModel.MODO_PESO) {
            campoAtivo = editTxtPeso;
            String label = Boolean.TRUE.equals(viewModel.isMetricaReduzida.getValue()) ? "100g" : "Kg";
            txtResultadoCalculado.setHint("Resultado (R$ por " + label + ")");
        } else if (modo == ComparacaoViewModel.MODO_UNIDADE) {
            campoAtivo = editTxtUnid;
            txtResultadoCalculado.setHint(getString(R.string.hint_resultado_por_unidade));
        } else {
            campoAtivo = editTxtVolume;
            String label = Boolean.TRUE.equals(viewModel.isMetricaReduzida.getValue()) ? "100ml" : "Litro";
            txtResultadoCalculado.setHint("Resultado (R$ por " + label + ")");
        }

        campoAtivo.setVisibility(View.VISIBLE);
        
        if (modo == ComparacaoViewModel.MODO_UNIDADE) {
            switchMetricaReduzida.setVisibility(View.GONE);
        } else {
            switchMetricaReduzida.setVisibility(View.VISIBLE);
        }

        campoAtivo.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.verde_acao));
        editTxtPrecoTotal.setBackgroundTintList(null);
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
        double valorEntrada = extrairValorDouble(campoAtivo);

        String precoEtiqueta = editTxtPrecoTotal.getText().toString();
        String qtdEtiqueta = campoAtivo != null ? campoAtivo.getText().toString() : "";
        
        viewModel.calcular(precoTotal, valorEntrada, precoEtiqueta, qtdEtiqueta, obterLabelModo());

        bloquearTrocaDeModo(true);
        editTxtPrecoTotal.setText("");
        if (campoAtivo != null) campoAtivo.setText("");
        editTxtPrecoTotal.requestFocus();
        vibrar();
    }

    private double extrairValorDouble(EditText campo) {
        if (campo == null) return 0.0;
        String texto = campo.getText().toString();
        String limpo = texto.replaceAll("[^0-9]", "");
        
        if (limpo.isEmpty()) return 0.0;

        try {
            double valor = Double.parseDouble(limpo);
            int id = campo.getId();

            if (id == R.id.editTxtPeso || id == R.id.editTxtVolume || campo == editTxtPeso || campo == editTxtVolume) {
                return valor / 1000.0;
            } else if (id == R.id.editTxtPreco || campo == editTxtPrecoTotal) {
                return valor / 100.0;
            }
            
            return valor;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void atualizarTabelaHistorico(ArrayList<ResultadoItem> resultados) {
        layoutHistoricoResultados.removeAllViews();
        
        String textoAnterior = txtIndicadorPasso.getText().toString();

        if (resultados.isEmpty()) {
            txtIndicadorPasso.setText(R.string.label_produto_1);
            txtIndicadorPasso.setBackgroundTintList(null);
            btnCompartilhar.setVisibility(View.GONE);
        } else if (resultados.size() == 1) {
            txtIndicadorPasso.setText(R.string.label_produto_2);
            txtIndicadorPasso.setBackgroundTintList(null);
            btnCompartilhar.setVisibility(View.GONE);
        } else {
            txtIndicadorPasso.setText(R.string.label_comparacao_pronta);
            txtIndicadorPasso.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.verde_acao));
            btnCompartilhar.setVisibility(View.VISIBLE);
        }

        if (!textoAnterior.equalsIgnoreCase(txtIndicadorPasso.getText().toString())) {
            animarBadgePasso();
        }

        if (resultados.isEmpty()) return;
        
        double menorValor = Double.MAX_VALUE;
        if (resultados.size() == 2) {
            for (ResultadoItem r : resultados) {
                if (r.getValorUnitario() < menorValor) menorValor = r.getValorUnitario();
            }
        }

        int contador = 1;
        for (ResultadoItem item : resultados) {
            LinearLayout itemRoot = new LinearLayout(this);
            itemRoot.setOrientation(LinearLayout.HORIZONTAL);
            itemRoot.setGravity(Gravity.CENTER_VERTICAL);
            itemRoot.setPadding(32, 24, 32, 24);

            TextView tvNumero = new TextView(this);
            tvNumero.setText(String.valueOf(contador++));
            tvNumero.setTextColor(Color.WHITE);
            tvNumero.setGravity(Gravity.CENTER);
            tvNumero.setBackgroundResource(R.drawable.bg_numero_item);
            
            LinearLayout.LayoutParams lpNumero = new LinearLayout.LayoutParams(
                (int) (24 * getResources().getDisplayMetrics().density),
                (int) (24 * getResources().getDisplayMetrics().density)
            );
            lpNumero.setMargins(0, 0, (int) (12 * getResources().getDisplayMetrics().density), 0);
            tvNumero.setLayoutParams(lpNumero);

            LinearLayout itemInfo = new LinearLayout(this);
            itemInfo.setOrientation(LinearLayout.VERTICAL);

            TextView tvEtiqueta = new TextView(this);
            tvEtiqueta.setText("Etiqueta: " + item.getPrecoOriginalFormatado() + " por " + item.getQuantidadeOriginal());

            TextView tvCalculo = new TextView(this);
            tvCalculo.setText(item.getDescricao());
            tvCalculo.setTypeface(null, Typeface.BOLD);

            if (resultados.size() == 2 && item.getValorUnitario() == menorValor) {
                itemRoot.setBackgroundResource(R.drawable.bg_melhor_opcao);
                tvCalculo.setTextColor(ContextCompat.getColor(this, R.color.verde_acao));
            }

            itemInfo.addView(tvEtiqueta);
            itemInfo.addView(tvCalculo);
            itemRoot.addView(tvNumero);
            itemRoot.addView(itemInfo);
            layoutHistoricoResultados.addView(itemRoot);
        }
    }

    private void compartilharMelhorOpcao() {
        ArrayList<ResultadoItem> resultados = viewModel.listaResultados.getValue();
        if (resultados == null || resultados.size() < 2) return;
        ResultadoItem item1 = resultados.get(0);
        ResultadoItem item2 = resultados.get(1);
        ResultadoItem melhor = (item1.getValorUnitario() < item2.getValorUnitario()) ? item1 : item2;
        ResultadoItem caro = (melhor == item1) ? item2 : item1;
        double economia = ((caro.getValorUnitario() - melhor.getValorUnitario()) / caro.getValorUnitario()) * 100;

        String mensagem = "🛒 *Dica de Compra - Compara Preço*\n\n" +
                "✅ *MELHOR:* " + melhor.getPrecoOriginalFormatado() + " (" + melhor.getQuantidadeOriginal() + ")\n" +
                "👉 Sai por: *" + melhor.getDescricao() + "*\n\n" +
                "❌ *OUTRO:* " + caro.getPrecoOriginalFormatado() + " (" + caro.getQuantidadeOriginal() + ")\n" +
                "👉 Sai por: " + caro.getDescricao() + "\n\n" +
                String.format(Locale.getDefault(), "💰 Economia de *%.1f%%*", economia) +
                "\n\n_Enviado por App Compara Preço_";

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mensagem);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Enviar comparação via:"));
    }

    private EditText obterCampoAtivo() {
        Integer modo = viewModel.modoAtual.getValue();
        if (modo == null) modo = ComparacaoViewModel.MODO_PESO;
        if (modo == ComparacaoViewModel.MODO_PESO) return editTxtPeso;
        if (modo == ComparacaoViewModel.MODO_UNIDADE) return editTxtUnid;
        return editTxtVolume;
    }

    private String obterLabelModo() {
        Integer modo = viewModel.modoAtual.getValue();
        if (modo == null) modo = ComparacaoViewModel.MODO_PESO;
        boolean isReduzida = Boolean.TRUE.equals(viewModel.isMetricaReduzida.getValue());
        
        if (modo == ComparacaoViewModel.MODO_PESO) {
            return isReduzida ? getString(R.string.unidade_100g) : "Kg";
        }
        if (modo == ComparacaoViewModel.MODO_UNIDADE) return "Unid.";
        return isReduzida ? getString(R.string.unidade_100ml) : "Litro";
    }

    private void limparTodosOsCampos() {
        editTxtPrecoTotal.setText("");
        limparCamposEspecificos();
        txtResultadoCalculado.setText("");
        editTxtPrecoTotal.requestFocus();
    }

    private void limparCamposEspecificos() {
        editTxtPeso.setText("");
        editTxtUnid.setText("");
        editTxtVolume.setText("");
        editTxtPeso.setError(null);
        editTxtUnid.setError(null);
        editTxtVolume.setError(null);
    }

    private void animarBadgePasso() {
        ScaleAnimation scale = new ScaleAnimation(1.0f, 1.2f, 1.0f, 1.2f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(150);
        scale.setRepeatCount(1);
        scale.setRepeatMode(ScaleAnimation.REVERSE);
        scale.setInterpolator(new AccelerateDecelerateInterpolator());
        txtIndicadorPasso.startAnimation(scale);
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

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {}
    }
}