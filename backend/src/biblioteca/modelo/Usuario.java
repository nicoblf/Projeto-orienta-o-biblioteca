package biblioteca.modelo;

import biblioteca.excecao.RegraNegocioException;
import biblioteca.util.SenhaUtil;

/**
 * Classe abstrata: dados comuns a quem acessa o sistema com login (nome, e-mail, senha criptografada)
 * e a operação de autenticação. Não é instanciada diretamente (Leitor e Bibliotecario herdam dela).
 */
public abstract class Usuario implements Identificavel {

    private int id;
    private String nome;
    private String email;
    private final String senhaHash;

    protected Usuario(String nome, String email, String senha) {
        validar(nome, email);
        if (senha == null || senha.length() < 6) {
            throw new RegraNegocioException("A senha deve ter ao menos 6 caracteres.");
        }
        this.nome = nome.trim();
        this.email = email.trim().toLowerCase();
        this.senhaHash = SenhaUtil.gerarHash(senha);
    }

    /** Reconstrói um usuário já gravado: o hash vem pronto do banco (não se recalcula nem se valida a senha). */
    protected Usuario(int id, String nome, String email, String senhaHash) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    public boolean autenticar(String senha) {
        return SenhaUtil.confere(senha, senhaHash);
    }

    public void atualizarDados(String nome, String email) {
        validar(nome, email);
        this.nome = nome.trim();
        this.email = email.trim().toLowerCase();
    }

    private static void validar(String nome, String email) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome é obrigatório.");
        }
        if (email == null || !email.contains("@")) {
            throw new RegraNegocioException("Informe um e-mail válido.");
        }
    }

    /** Perfil de acesso (RNF03): cada subclasse informa o seu. */
    public abstract String getPerfil();

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    /** Usado apenas pela camada de persistência; nunca é exposto pela API. */
    public String getSenhaHash() {
        return senhaHash;
    }
}
