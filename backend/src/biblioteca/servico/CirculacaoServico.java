package biblioteca.servico;

import biblioteca.excecao.AcessoNegadoException;
import biblioteca.excecao.RecursoNaoEncontradoException;
import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.Bibliotecario;
import biblioteca.modelo.Emprestimo;
import biblioteca.modelo.Exemplar;
import biblioteca.modelo.Leitor;
import biblioteca.modelo.Multa;
import biblioteca.modelo.SituacaoExemplar;
import biblioteca.modelo.Usuario;
import biblioteca.repositorio.EmprestimoRepositorio;
import biblioteca.repositorio.MultaRepositorio;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquestra a circulação (empréstimo, devolução, renovação, multas) e delega as regras às classes
 * do modelo. RF05 a RF09. Esta classe não contém regra: apenas busca os objetos e chama os métodos.
 */
public class CirculacaoServico {

    private final EmprestimoRepositorio emprestimos;
    private final MultaRepositorio multas;
    private final AcervoServico acervo;
    private final LeitorServico leitores;

    public CirculacaoServico(EmprestimoRepositorio emprestimos, MultaRepositorio multas,
                             AcervoServico acervo, LeitorServico leitores) {
        this.emprestimos = emprestimos;
        this.multas = multas;
        this.acervo = acervo;
        this.leitores = leitores;
    }

    public Emprestimo registrarEmprestimo(Bibliotecario bibliotecario, String matricula, String codigoTombo) {
        Leitor leitor = leitores.buscarPorMatricula(matricula);
        Exemplar exemplar = acervo.buscarExemplarPorTombo(codigoTombo);
        return gravarEmprestimo(bibliotecario.registrarEmprestimo(leitor, exemplar));
    }

    /** Grava o empréstimo novo e a nova situação (EMPRESTADO) do exemplar. */
    private Emprestimo gravarEmprestimo(Emprestimo emprestimo) {
        emprestimos.salvar(emprestimo);
        acervo.atualizarExemplar(emprestimo.getExemplar());
        return emprestimo;
    }

    /** Versão com data retroativa, usada pela carga inicial de dados e pelos testes. */
    public Emprestimo registrarEmprestimo(Bibliotecario bibliotecario, Leitor leitor, Exemplar exemplar, LocalDate data) {
        return gravarEmprestimo(bibliotecario.registrarEmprestimo(leitor, exemplar, data));
    }

    public Emprestimo registrarDevolucao(Bibliotecario bibliotecario, int emprestimoId) {
        return registrarDevolucao(bibliotecario, buscarEmprestimo(emprestimoId), LocalDate.now());
    }

    public Emprestimo registrarDevolucao(Bibliotecario bibliotecario, Emprestimo emprestimo, LocalDate data) {
        bibliotecario.registrarDevolucao(emprestimo, data);
        emprestimos.salvar(emprestimo);
        acervo.atualizarExemplar(emprestimo.getExemplar());
        if (emprestimo.getMulta() != null) {
            multas.salvar(emprestimo.getMulta());
        }
        return emprestimo;
    }

    /** O leitor só renova os próprios empréstimos; o bibliotecário renova qualquer um. */
    public Emprestimo renovar(Usuario solicitante, int emprestimoId) {
        Emprestimo emprestimo = buscarEmprestimo(emprestimoId);
        if (solicitante instanceof Leitor l && l.getId() != emprestimo.getLeitor().getId()) {
            throw new AcessoNegadoException(403, "Você só pode renovar os seus próprios empréstimos.");
        }
        if (!emprestimo.renovar()) {
            throw new RegraNegocioException("Renovação não permitida: " + emprestimo.motivoImpedimentoRenovacao());
        }
        return emprestimos.salvar(emprestimo);
    }

    public Multa quitarMulta(int multaId) {
        Multa multa = multas.buscarPorId(multaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Multa não encontrada."));
        multa.quitar();
        multas.salvar(multa);
        return multa;
    }

    public Emprestimo buscarEmprestimo(int id) {
        return emprestimos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));
    }

    public List<Emprestimo> listarTodos() {
        return emprestimos.listar();
    }

    public List<Emprestimo> listarAtivos() {
        return emprestimos.listarAtivos();
    }

    public List<Emprestimo> listarAtrasados() {
        return emprestimos.listarAtrasados();
    }

    /** Visão geral para o painel do bibliotecário. */
    public Map<String, Object> painel() {
        LocalDate hoje = LocalDate.now();
        long devolvidosHoje = emprestimos.listar().stream()
                .filter(e -> hoje.equals(e.getDataDevolucao())).count();
        long multasAbertas = multas.listar().stream().filter(m -> !m.isPaga()).count();
        double valorAberto = multas.listar().stream().filter(m -> !m.isPaga()).mapToDouble(Multa::getValor).sum();
        List<Exemplar> todos = acervo.listarExemplares();
        long disponiveis = todos.stream().filter(e -> e.getSituacao() == SituacaoExemplar.DISPONIVEL).count();
        long baixados = todos.stream().filter(e -> e.getSituacao() == SituacaoExemplar.BAIXADO).count();

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("emprestimosAtivos", emprestimos.listarAtivos().size());
        p.put("emprestimosAtrasados", emprestimos.listarAtrasados().size());
        p.put("devolucoesHoje", devolvidosHoje);
        p.put("multasEmAberto", multasAbertas);
        p.put("valorMultasEmAberto", Math.round(valorAberto * 100.0) / 100.0);
        p.put("titulos", acervo.buscar(null).size());
        p.put("exemplares", todos.size() - baixados);
        p.put("exemplaresDisponiveis", disponiveis);
        p.put("exemplaresBaixados", baixados);
        return p;
    }
}
