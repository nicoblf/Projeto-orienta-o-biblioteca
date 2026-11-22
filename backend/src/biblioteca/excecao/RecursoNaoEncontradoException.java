package biblioteca.excecao;

/** Registro inexistente (livro, exemplar, leitor...). Vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
