package biblioteca.excecao;

/** Falha de autenticação (401) ou de perfil de acesso (403). */
public class AcessoNegadoException extends RuntimeException {
    private final int status;

    public AcessoNegadoException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
