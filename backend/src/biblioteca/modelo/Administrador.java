package biblioteca.modelo;

/**
 * Administrador do sistema: tem acesso total. Por ser um Bibliotecario, pode fazer tudo o que o
 * balcão faz (empréstimos, devoluções, acervo, leitores) e, além disso, cadastra e exclui
 * funcionários e leitores, redefine senhas e consulta o log de auditoria.
 */
public class Administrador extends Bibliotecario {

    private static final String REGISTRO = "ADMIN";

    public Administrador(String nome, String email, String senha) {
        super(nome, email, senha, REGISTRO);
    }

    private Administrador(int id, String nome, String email, String senhaHash) {
        super(id, nome, email, senhaHash, REGISTRO);
    }

    /** Reconstrói um administrador gravado no banco. */
    public static Administrador restaurar(int id, String nome, String email, String senhaHash) {
        return new Administrador(id, nome, email, senhaHash);
    }

    @Override
    public String getPerfil() {
        return "ADMIN";
    }
}
