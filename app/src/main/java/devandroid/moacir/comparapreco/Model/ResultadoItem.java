package devandroid.moacir.comparapreco.Model;

import android.os.Parcel;
import android.os.Parcelable;

public class ResultadoItem implements Parcelable {
    private String descricao;
    private double valorUnitario;
    private String label;
    // Novos campos para armazenar os dados da etiqueta
    private String precoOriginal;
    private String quantidadeOriginal;

    // Construtor atualizado para receber os dados da etiqueta
    public ResultadoItem(String descricao, double valorUnitario, String label, String precoOriginal, String quantidadeOriginal) {
        this.descricao = descricao;
        this.valorUnitario = valorUnitario;
        this.label = label;
        this.precoOriginal = precoOriginal;
        this.quantidadeOriginal = quantidadeOriginal;
    }

    protected ResultadoItem(Parcel in) {
        descricao = in.readString();
        valorUnitario = in.readDouble();
        label = in.readString();
        precoOriginal = in.readString();
        quantidadeOriginal = in.readString();
    }

    public static final Creator<ResultadoItem> CREATOR = new Creator<ResultadoItem>() {
        @Override
        public ResultadoItem createFromParcel(Parcel in) {
            return new ResultadoItem(in);
        }

        @Override
        public ResultadoItem[] newArray(int size) {
            return new ResultadoItem[size];
        }
    };

    public ResultadoItem(String textoResultado, double resultadoNumerico, String label) {
    }

    public double getValorUnitario() {
        return valorUnitario;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getLabel() {
        return label;
    }

    // Retorna o preço que estava na etiqueta (ex: R$ 25,90)
    public String getPrecoOriginalFormatado() {
        return precoOriginal != null ? precoOriginal : "---";
    }

    // Retorna a quantidade que estava na etiqueta (ex: 5,00)
    public String getQuantidadeOriginal() {
        return quantidadeOriginal != null ? quantidadeOriginal : "---";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(descricao);
        dest.writeDouble(valorUnitario);
        dest.writeString(label);
        dest.writeString(precoOriginal);
        dest.writeString(quantidadeOriginal);
    }
}