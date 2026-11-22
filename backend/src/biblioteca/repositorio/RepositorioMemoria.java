package biblioteca.repositorio;

import biblioteca.modelo.Identificavel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repositório genérico. Os objetos ficam em memória (para as regras de negócio trabalharem sobre o
 * grafo de objetos) e, quando há um Banco, cada alteração é gravada nele (write-through) e tudo é
 * recarregado na partida. Sem Banco, funciona só em memória (usado nos testes e na demonstração).
 */
public abstract class RepositorioMemoria<T extends Identificavel> {

    private final List<T> itens = new ArrayList<>();
    private final AtomicInteger sequencia = new AtomicInteger(0);

    /** Insere (id == 0) ou atualiza o item. */
    public T salvar(T item) {
        if (item.getId() == 0) {
            item.setId(inserir(item));
            itens.add(item);
        } else {
            atualizar(item);
        }
        return item;
    }

    /** Grava um item novo e devolve o seu id. Por padrão, gera o id em memória. */
    protected int inserir(T item) {
        return sequencia.incrementAndGet();
    }

    protected void atualizar(T item) {
    }

    /** Usado na carga inicial a partir do banco (não regrava). */
    protected void adicionarCarregado(T item) {
        itens.add(item);
    }

    public Optional<T> buscarPorId(int id) {
        return itens.stream().filter(i -> i.getId() == id).findFirst();
    }

    public List<T> listar() {
        return new ArrayList<>(itens);
    }
}
