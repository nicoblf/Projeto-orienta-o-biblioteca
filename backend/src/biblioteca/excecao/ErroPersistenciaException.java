package biblioteca.excecao;

/** Falha ao ler ou gravar no banco de dados. Vira HTTP 500 (detalhes só no log do servidor). */
public class ErroPersistenciaException extends RuntimeException {
    public ErroPersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
