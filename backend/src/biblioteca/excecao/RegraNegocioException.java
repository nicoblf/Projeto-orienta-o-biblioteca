package biblioteca.excecao;

/** Violação de uma regra do sistema (ex.: exemplar já emprestado, leitor com multa). Vira HTTP 422. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
