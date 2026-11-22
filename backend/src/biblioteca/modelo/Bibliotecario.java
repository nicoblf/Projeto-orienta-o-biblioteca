package biblioteca.modelo;

import biblioteca.excecao.RegraNegocioException;
import java.time.LocalDate;

/** Operador do balcão: único perfil que registra empréstimos e devoluções (a entrega física é no balcão). */
public class Bibliotecario extends Usuario {

    private final String registroFuncional;

    public Bibliotecario(String nome, String email, String senha, String registroFuncional) {
        super(nome, email, senha);
        if (registroFuncional == null || registroFuncional.isBlank()) {
            throw new RegraNegocioException("O registro funcional é obrigatório.");
        }
        this.registroFuncional = registroFuncional.trim();
    }

    private Bibliotecario(int id, String nome, String email, String senhaHash, String registroFuncional) {
        super(id, nome, email, senhaHash);
        this.registroFuncional = registroFuncional;
    }

    /** Reconstrói um bibliotecário gravado no banco. */
    public static Bibliotecario restaurar(int id, String nome, String email, String senhaHash, String registroFuncional) {
        return new Bibliotecario(id, nome, email, senhaHash, registroFuncional);
    }

    public Emprestimo registrarEmprestimo(Leitor leitor, Exemplar exemplar) {
        return registrarEmprestimo(leitor, exemplar, LocalDate.now());
    }

    /**
     * Aplica as regras RF05, RF07 e RF08 antes de gravar: exemplar livre, leitor sem atraso/multa
     * e limite de obras respeitado. A data é parâmetro para permitir carga de dados e testes.
     */
    public Emprestimo registrarEmprestimo(Leitor leitor, Exemplar exemplar, LocalDate dataEmprestimo) {
        if (!exemplar.estaDisponivel()) {
            throw new RegraNegocioException("O exemplar " + exemplar.getCodigoTombo()
                    + " não está disponível (situação: " + exemplar.getSituacao() + ").");
        }
        if (leitor.possuiPendencias()) {
            throw new RegraNegocioException(leitor.getNome() + " possui atraso ou multa em aberto.");
        }
        if (!leitor.podeEmprestar()) {
            throw new RegraNegocioException(leitor.getNome() + " atingiu o limite de "
                    + leitor.getCategoria().getLimiteObras() + " obra(s) emprestada(s).");
        }
        Emprestimo emprestimo = new Emprestimo(leitor, exemplar, this, dataEmprestimo);
        exemplar.alterarSituacao(SituacaoExemplar.EMPRESTADO);
        leitor.adicionarEmprestimo(emprestimo);
        return emprestimo;
    }

    public void registrarDevolucao(Emprestimo emprestimo) {
        registrarDevolucao(emprestimo, LocalDate.now());
    }

    public void registrarDevolucao(Emprestimo emprestimo, LocalDate dataDevolucao) {
        emprestimo.registrarDevolucao(dataDevolucao);
    }

    @Override
    public String getPerfil() {
        return "BIBLIOTECARIO";
    }

    public String getRegistroFuncional() {
        return registroFuncional;
    }
}
