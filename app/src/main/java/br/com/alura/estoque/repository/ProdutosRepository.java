package br.com.alura.estoque.repository;

import java.util.List;

import br.com.alura.estoque.asynctask.BaseAsyncTask;
import br.com.alura.estoque.database.dao.ProdutoDAO;
import br.com.alura.estoque.model.Produto;
import br.com.alura.estoque.retrofit.EstoqueRetrofit;
import br.com.alura.estoque.retrofit.callback.BaseCallback;
import br.com.alura.estoque.retrofit.service.ProdutoService;
import retrofit2.Call;

public class ProdutosRepository {

    private final ProdutoDAO produtoDAO;
    private final ProdutoService produtoService;

    public ProdutosRepository(ProdutoDAO produtoDAO) {
        this.produtoDAO = produtoDAO;
        produtoService = new EstoqueRetrofit().getProdutoService();
    }

    public void buscaProdutos(DadosCarregadosCallback<List<Produto>> callback) {
        buscaProdutosInternos(callback);
    }

    private void buscaProdutosInternos(DadosCarregadosCallback<List<Produto>> callback) {
        new BaseAsyncTask<>(produtoDAO::buscaTodos,
                resultado -> {
                    callback.quandoSucesso(resultado);
                    buscaProdutosNaApi(callback);
                })
                .execute();
    }

    private void buscaProdutosNaApi(DadosCarregadosCallback<List<Produto>> callback) {
        final Call<List<Produto>> call = produtoService.buscaTodos();
        call.enqueue(new BaseCallback<>(new BaseCallback.RespostaCallback<List<Produto>>() {
            @Override
            public void quandoSucesso(List<Produto> resultado) {
                atualizaInterno(resultado, callback);
            }

            @Override
            public void quandoFalha(String erro) {
                callback.quandoFalha(erro);
            }
        }));
    }

    private void atualizaInterno(List<Produto> produtos, DadosCarregadosCallback<List<Produto>> callback) {
        new BaseAsyncTask<>(() -> {
            produtoDAO.salva(produtos);
            return produtoDAO.buscaTodos();
        }, callback::quandoSucesso).execute();
    }

    public void salva(Produto produto, DadosCarregadosCallback<Produto> callback) {
        salvaNaApi(produto, callback);

    }

    private void salvaNaApi(Produto produto, DadosCarregadosCallback<Produto> callback) {
        final Call<Produto> call = produtoService.salva(produto);
        call.enqueue(new BaseCallback<>(new BaseCallback.RespostaCallback<Produto>() {
            @Override
            public void quandoSucesso(Produto produtoSalvo) {
                salvaInterno(produtoSalvo, callback);
            }

            @Override
            public void quandoFalha(String erro) {
                callback.quandoFalha(erro);
            }
        }));
    }

    private void salvaInterno(Produto produtoSalvo, DadosCarregadosCallback<Produto> callback) {
        new BaseAsyncTask<>(() -> {
            long id = produtoDAO.salva(produtoSalvo);
            return produtoDAO.buscaProduto(id);
        }, callback::quandoSucesso)
                .execute();
    }

    public void edita(Produto produto, DadosCarregadosCallback<Produto> callback) {
        editaNaApi(produto, callback);
    }

    private void editaNaApi(Produto produto, DadosCarregadosCallback<Produto> callback) {
        Call<Produto> call = produtoService.edita(produto.getId(), produto);
        call.enqueue(new BaseCallback<>(new BaseCallback.RespostaCallback<Produto>() {
            @Override
            public void quandoSucesso(Produto resultado) {
                editaInterno(produto, callback);
            }

            @Override
            public void quandoFalha(String erro) {
                callback.quandoFalha(erro);
            }
        }));
    }

    private void editaInterno(Produto produto, DadosCarregadosCallback<Produto> callback) {
        new BaseAsyncTask<>(() -> {
            produtoDAO.atualiza(produto);
            return produto;
        }, callback::quandoSucesso).execute();
    }

    public interface DadosCarregadosCallback <T> {
        void quandoSucesso(T resultado);
        void quandoFalha(String erro);
    }
}
