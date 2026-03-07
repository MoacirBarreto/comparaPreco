package devandroid.moacir.comparapreco.Model;

import android.os.Parcel;
import android.os.Parcelable;

public class ResultadoItem implements Parcelable {
    private String descricao;
    private double valorUnitario;
    private String label;

    public ResultadoItem(String descricao, double valorUnitario, String label) {
        this.descricao = descricao;
        this.valorUnitario = valorUnitario;
        this.label = label;
    }

    protected ResultadoItem(Parcel in) {
        descricao = in.readString();
        valorUnitario = in.readDouble();
        label = in.readString();
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

    // This is the missing method that caused the error!
    public double getValorUnitario() {
        return valorUnitario;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getLabel() {
        return label;
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
    }
}