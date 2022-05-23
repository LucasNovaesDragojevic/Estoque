package br.com.alura.estoque.repository;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.List;

import br.com.alura.estoque.asynctask.BaseAsyncTask;
import br.com.alura.estoque.database.dao.ProdutoDAO;
import br.com.alura.estoque.model.Produto;
import br.com.alura.estoque.retrofit.EstoqueRetrofit;
import br.com.alura.estoque.retrofit.service.ProdutoService;
import retrofit2.Call;
import retrofit2.Response;

public class ProdutosRepository {

    private final ProdutoDAO produtoDAO;
    private ProdutosCarregadosListener listener;

    public ProdutosRepository(ProdutoDAO produtoDAO) {
        this.produtoDAO = produtoDAO;
    }

    public void buscaProdutos(ProdutosCarregadosListener listener) {
        buscaProdutosInternos(listener);
    }

    private void buscaProdutosInternos(ProdutosCarregadosListener listener) {
        new BaseAsyncTask<>(produtoDAO::buscaTodos,
                resultado -> {
                    listener.quandoCarregados(resultado);
                    buscaProdutosNaApi(listener);
                })
                .execute();
    }

    private void buscaProdutosNaApi(ProdutosCarregadosListener listener) {
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

    public interface ProdutosCarregadosListener {
        void quandoCarregados(List<Produto> produtos);
    }
}
