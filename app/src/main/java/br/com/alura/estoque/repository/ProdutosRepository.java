package br.com.alura.estoque.repository;

import android.os.AsyncTask;

import androidx.room.Ignore;

import java.io.IOException;
import java.util.List;

import br.com.alura.estoque.asynctask.BaseAsyncTask;
import br.com.alura.estoque.database.dao.ProdutoDAO;
import br.com.alura.estoque.model.Produto;
import br.com.alura.estoque.retrofit.EstoqueRetrofit;
import br.com.alura.estoque.retrofit.service.ProdutoService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;

public class ProdutosRepository {

    private final ProdutoDAO produtoDAO;
    private final ProdutoService produtoService;

    public ProdutosRepository(ProdutoDAO produtoDAO) {
        this.produtoDAO = produtoDAO;
        produtoService = new EstoqueRetrofit().getProdutoService();
    }

    public void buscaProdutos(DadosCarregadosListener<List<Produto>> listener) {
        buscaProdutosInternos(listener);
    }

    private void buscaProdutosInternos(DadosCarregadosListener<List<Produto>> listener) {
        new BaseAsyncTask<>(produtoDAO::buscaTodos,
                resultado -> {
                    listener.quandoCarregados(resultado);
                    buscaProdutosNaApi(listener);
                })
                .execute();
    }

    private void buscaProdutosNaApi(DadosCarregadosListener<List<Produto>> listener) {
        final ProdutoService produtoService = new EstoqueRetrofit().getProdutoService();
        final Call<List<Produto>> call = produtoService.buscaTodos();
        new BaseAsyncTask<>(() -> {
            try {
                Thread.sleep(3000);
                final Response<List<Produto>> response = call.execute();
                final List<Produto> produtos = response.body();
                produtoDAO.salva(produtos);
                return produtoDAO.buscaTodos();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return produtoDAO.buscaTodos();
        }, listener::quandoCarregados)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void salva(Produto produto, DadosCarregadosCallback<Produto> callback) {
        salvaNaApi(produto, callback);

    }

    private void salvaNaApi(Produto produto, DadosCarregadosCallback<Produto> callback) {
        final Call<Produto> call = produtoService.salva(produto);
        call.enqueue(new Callback<Produto>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<Produto> call, Response<Produto> response) {
                if (response.isSuccessful()) {
                    final Produto produtoSalvo = response.body();
                    if (produtoSalvo != null) {
                        salvaInterno(produtoSalvo, callback);
                    } else {
                        callback.quandoFalha("Resposta não sucedida");
                    }
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<Produto> call, Throwable t) {
                callback.quandoFalha("Falha de comunicação: " + t.getMessage());
            }
        });
    }

    private void salvaInterno(Produto produtoSalvo, DadosCarregadosCallback<Produto> callback) {
        new BaseAsyncTask<>(() -> {
            long id = produtoDAO.salva(produtoSalvo);
            return produtoDAO.buscaProduto(id);
        }, callback::quandoSucesso)
                .execute();
    }

    public interface DadosCarregadosListener <T> {
        void quandoCarregados(T dados);
    }

    public interface DadosCarregadosCallback <T> {
        void quandoSucesso(T resultado);
        void quandoFalha(String erro);
    }
}
