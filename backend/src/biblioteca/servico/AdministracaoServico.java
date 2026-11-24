package biblioteca.servico;

import biblioteca.excecao.RecursoNaoEncontradoException;
import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.Administrador;
import biblioteca.modelo.Bibliotecario;
import biblioteca.modelo.Leitor;
import biblioteca.modelo.Usuario;
import biblioteca.repositorio.EmprestimoRepositorio;
import biblioteca.repositorio.UsuarioRepositorio;
import biblioteca.util.SenhaUtil;
import java.util.List;

/** Funções do administrador: cadastrar funcionários e redefinir senhas (sempre com senha provisória). */
public class AdministracaoServico {

    /** Usuário recém-criado ou com senha redefinida, junto da senha provisória (mostrada uma única vez). */
    public record ComSenha<T extends Usuario>(T usuario, String senhaProvisoria) {
    }

    private final UsuarioRepositorio usuarios;
    private final EmprestimoRepositorio emprestimos;
    private final AutenticacaoServico autenticacao;

    public AdministracaoServico(UsuarioRepositorio usuarios, EmprestimoRepositorio emprestimos,
                                AutenticacaoServico autenticacao) {
        this.usuarios = usuarios;
        this.emprestimos = emprestimos;
        this.autenticacao = autenticacao;
    }

    /**
     * Exclui um funcionário ou leitor. Quem tem histórico de empréstimos é apenas DESATIVADO (perde o
     * acesso, mas o histórico continua apontando para ele); quem não tem é apagado. Leitor com
     * empréstimo em aberto ou multa não paga não pode ser excluído. Retorna true se foi apagado.
     */
    public boolean excluirUsuario(Administrador quem, int usuarioId) {
        Usuario alvo = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (alvo == quem) {
            throw new RegraNegocioException("Você não pode excluir a sua própria conta.");
        }
        if (alvo instanceof Administrador) {
            throw new RegraNegocioException("Contas de administrador não podem ser excluídas por aqui.");
        }
        if (!alvo.isAtivo()) {
            throw new RegraNegocioException(alvo.getNome() + " já está desativado(a).");
        }
        boolean temHistorico;
        if (alvo instanceof Leitor l) {
            if (l.qtdEmprestimosAtivos() > 0) {
                throw new RegraNegocioException(l.getNome() + " ainda tem " + l.qtdEmprestimosAtivos()
                        + " empréstimo(s) em aberto. Registre a devolução antes de excluir.");
            }
            if (l.possuiMultaEmAberto()) {
                throw new RegraNegocioException(l.getNome() + " tem multa em aberto. Registre o pagamento antes de excluir.");
            }
            temHistorico = !l.consultarEmprestimos().isEmpty();
        } else {
            temHistorico = emprestimos.listar().stream().anyMatch(e -> e.getBibliotecario() == alvo);
        }
        autenticacao.encerrarSessoesDe(alvo);
        if (temHistorico) {
            alvo.desativar();
            usuarios.salvar(alvo);
            return false;
        }
        usuarios.remover(alvo);
        return true;
    }

    public List<Usuario> listarUsuarios() {
        return usuarios.listar();
    }

    public ComSenha<Bibliotecario> cadastrarFuncionario(String nome, String email, String registroFuncional) {
        String senha = SenhaUtil.gerarSenhaProvisoria();
        Bibliotecario b = new Bibliotecario(nome, email, senha, registroFuncional);
        b.definirSenhaProvisoria(senha);
        if (usuarios.buscarPorEmail(b.getEmail()).isPresent()) {
            throw new RegraNegocioException("Já existe um usuário com o e-mail " + b.getEmail() + ".");
        }
        usuarios.salvar(b);
        return new ComSenha<>(b, senha);
    }

    /** Gera nova senha provisória e derruba as sessões abertas do usuário. */
    public ComSenha<Usuario> redefinirSenha(Administrador quem, int usuarioId) {
        Usuario alvo = usuarios.buscarPorId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        if (alvo == quem) {
            throw new RegraNegocioException("Para alterar a sua própria senha, use \"Trocar senha\".");
        }
        if (!alvo.isAtivo()) {
            throw new RegraNegocioException(alvo.getNome() + " está desativado(a).");
        }
        String senha = SenhaUtil.gerarSenhaProvisoria();
        alvo.definirSenhaProvisoria(senha);
        usuarios.salvar(alvo);
        autenticacao.encerrarSessoesDe(alvo);
        return new ComSenha<>(alvo, senha);
    }

    /** Garante que exista ao menos um administrador (bancos antigos ou instalação nova). */
    public void garantirAdministrador(String email, String senhaInicial) {
        if (usuarios.existeAdministrador()) {
            return;
        }
        if (usuarios.buscarPorEmail(email).isPresent()) {
            System.err.println("Não foi possível criar o administrador padrão: e-mail " + email + " já em uso.");
            return;
        }
        Administrador adm = new Administrador("Administrador", email, senhaInicial);
        usuarios.salvar(adm);
        System.out.println("Administrador criado: " + email);
    }
}
