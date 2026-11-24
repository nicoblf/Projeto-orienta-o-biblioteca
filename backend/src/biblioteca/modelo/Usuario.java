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
    private String senhaHash;
    /** Senha definida por outra pessoa (cadastro no balcão ou redefinição): deve ser trocada no próximo login. */
    private boolean senhaProvisoria;

    protected Usuario(String nome, String email, String senha) {
        validar(nome, email);
        validarSenha(senha);
        this.nome = nome.trim();
        this.email = email.trim().toLowerCase();
        this.senhaHash = SenhaUtil.gerarHash(senha);
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.length() < 6) {
            throw new RegraNegocioException("A senha deve ter ao menos 6 caracteres.");
        }
    }

    /** Troca feita pelo próprio usuário: exige a senha atual e encerra a condição de senha provisória. */
    public void trocarSenha(String senhaAtual, String novaSenha) {
        if (!autenticar(senhaAtual)) {
            throw new RegraNegocioException("A senha atual está incorreta.");
        }
        validarSenha(novaSenha);
        if (novaSenha.equals(senhaAtual)) {
            throw new RegraNegocioException("A nova senha deve ser diferente da atual.");
        }
        this.senhaHash = SenhaUtil.gerarHash(novaSenha);
        this.senhaProvisoria = false;
    }

    /**
     * Primeiro acesso com senha provisória: o usuário acabou de entrar com ela, então só informa a
     * nova senha (não precisa redigitar a provisória).
     */
    public void substituirSenhaProvisoria(String novaSenha) {
        if (!senhaProvisoria) {
            throw new RegraNegocioException("Informe a senha atual.");
        }
        validarSenha(novaSenha);
        if (autenticar(novaSenha)) {
            throw new RegraNegocioException("A nova senha deve ser diferente da senha provisória.");
        }
        this.senhaHash = SenhaUtil.gerarHash(novaSenha);
        this.senhaProvisoria = false;
    }

    /** Define uma senha provisória (gerada pelo sistema), que o usuário será obrigado a trocar. */
    public void definirSenhaProvisoria(String senha) {
        validarSenha(senha);
        this.senhaHash = SenhaUtil.gerarHash(senha);
        this.senhaProvisoria = true;
    }

    /** Usado na carga a partir do banco. */
    public void restaurarSenhaProvisoria(boolean provisoria) {
        this.senhaProvisoria = provisoria;
    }

    public boolean isSenhaProvisoria() {
        return senhaProvisoria;
    }

    /** Usuário desativado não entra mais no sistema, mas continua nos registros históricos. */
    private boolean ativo = true;

    public boolean isAtivo() {
        return ativo;
    }

    public void desativar() {
        this.ativo = false;
    }

    /** Usado na carga a partir do banco. */
    public void restaurarAtivo(boolean ativo) {
        this.ativo = ativo;
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
