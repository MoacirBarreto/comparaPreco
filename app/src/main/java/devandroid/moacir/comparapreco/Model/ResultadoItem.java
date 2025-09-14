package devandroid.moacir.converteunidades.Model;

import android.os.Parcel;
import android.os.Parcelable;

public class ResultadoItem implements Parcelable {
    private final String descricao; // Ex: "15.76 por Kg"
    private final double valorCalculado; // O valor numérico puro
    private final String tipoConversao; // Ex: "Peso" ou "Unidade"

    public ResultadoItem(String descricao, double valorCalculado, String tipoConversao) {
        this.descricao = descricao;
        this.valorCalculado = valorCalculado;
        this.tipoConversao = tipoConversao;
    }

    // Getters
    public String getDescricao() {
        return descricao;
    }

    public double getValorCalculado() {
        return valorCalculado;
    }

    public String getTipoConversao() {
        return tipoConversao;
    }

    // Parcelable implementation (para salvar em onSaveInstanceState)
    protected ResultadoItem(Parcel in) {
        descricao = in.readString();
        valorCalculado = in.readDouble();
        tipoConversao = in.readString();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(descricao);
        dest.writeDouble(valorCalculado);
        dest.writeString(tipoConversao);
    }

    // Opcional: sobrescrever toString() para facilitar o debug
    @Override
    public String toString() {
        return descricao;
    }
}

