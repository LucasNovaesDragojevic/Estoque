package br.com.alura.estoque.retrofit.callback;

import br.com.alura.estoque.repository.ProdutosRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;

public class CallbackComRetorno<T> implements Callback <T> {

    private final DadosCarregadosCallback<T> callback;

    public CallbackComRetorno(DadosCarregadosCallback<T> callback) {
        this.callback = callback;
    }

    @Override
    @EverythingIsNonNull
    public void onResponse(Call<T> call, Response<T> response) {
        if (response.isSuccessful()) {
            final T body = response.body();
            if (body != null) {
                callback.quandoSucesso(body);
            }
        } else {
            callback.quandoFalha("Resposta não sucedida");
        }
    }

    @Override
    @EverythingIsNonNull
    public void onFailure(Call<T> call, Throwable t) {
        callback.quandoFalha("Falha de comunicação: " + t.getMessage());
    }

    public interface DadosCarregadosCallback <T> {
        void quandoSucesso(T resultado);
        void quandoFalha(String erro);
    }
}
