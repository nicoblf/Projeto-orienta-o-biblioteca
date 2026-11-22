package biblioteca.modelo;

import biblioteca.excecao.RegraNegocioException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Classe central: liga quem retirou (Leitor), o que foi retirado (Exemplar) e quem registrou (Bibliotecario). */
public class Emprestimo implements Identificavel {

    private int id;
    private final Leitor leitor;
    private final Exemplar exemplar;
    private final Bibliotecario bibliotecario;
    private final LocalDate dataEmprestimo;
    private LocalDate dataPrevista;
    private LocalDate dataDevolucao;
    private int qtdRenovacoes;
    private Multa multa; // 0..1

    public Emprestimo(Leitor leitor, Exemplar exemplar, Bibliotecario bibliotecario, LocalDate dataEmprestimo) {
        this.leitor = leitor;
        this.exemplar = exemplar;
        this.bibliotecario = bibliotecario;
        this.dataEmprestimo = dataEmprestimo;
        this.dataPrevista = calcularDataPrevista();
    }

    /** Reconstrói um empréstimo gravado no banco e o liga ao seu leitor. */
    public static Emprestimo restaurar(int id, Leitor leitor, Exemplar exemplar, Bibliotecario bibliotecario,
                                       LocalDate dataEmprestimo, LocalDate dataPrevista,
                                       LocalDate dataDevolucao, int qtdRenovacoes) {
        Emprestimo e = new Emprestimo(leitor, exemplar, bibliotecario, dataEmprestimo);
        e.id = id;
        e.dataPrevista = dataPrevista;
        e.dataDevolucao = dataDevolucao;
        e.qtdRenovacoes = qtdRenovacoes;
        leitor.adicionarEmprestimo(e);
        return e;
    }

    /** Reconstrói a multa gravada para este empréstimo. */
    public Multa restaurarMulta(int idMulta, double valor, LocalDate dataGeracao, boolean paga) {
        this.multa = new Multa(idMulta, this, valor, dataGeracao, paga);
        return this.multa;
    }

    /** Data do empréstimo + prazo da categoria do leitor (RF05). */
    public final LocalDate calcularDataPrevista() {
        return dataEmprestimo.plusDays(leitor.getCategoria().getPrazoDias());
    }

    public boolean estaAtivo() {
        return dataDevolucao == null;
    }

    public boolean estaAtrasado() {
        return estaAtivo() && LocalDate.now().isAfter(dataPrevista);
    }

    /** Dias além do prazo: até hoje (se em aberto) ou até a devolução (se encerrado). */
    public int diasAtraso() {
        LocalDate referencia = estaAtivo() ? LocalDate.now() : dataDevolucao;
        long dias = ChronoUnit.DAYS.between(dataPrevista, referencia);
        return dias > 0 ? (int) dias : 0;
    }

    /** Multa que seria cobrada se o livro fosse devolvido hoje (0 se não está atrasado). A multa real só é gerada na devolução. */
    public double multaEstimada() {
        if (!estaAtrasado()) {
            return 0;
        }
        double bruto = diasAtraso() * leitor.getCategoria().getValorMultaDiaria();
        return Math.round(bruto * 100.0) / 100.0;
    }

    /** Motivo pelo qual a renovação não é permitida, ou null se pode renovar (RF06). */
    public String motivoImpedimentoRenovacao() {
        if (!estaAtivo()) {
            return "O empréstimo já foi encerrado.";
        }
        if (qtdRenovacoes >= leitor.getCategoria().getLimiteRenovacoes()) {
            return "Limite de " + leitor.getCategoria().getLimiteRenovacoes() + " renovação(ões) atingido.";
        }
        if (leitor.possuiPendencias()) {
            return "O leitor possui atraso ou multa em aberto.";
        }
        return null;
    }

    public boolean renovar() {
        if (motivoImpedimentoRenovacao() != null) {
            return false;
        }
        dataPrevista = dataPrevista.plusDays(leitor.getCategoria().getPrazoDias());
        qtdRenovacoes++;
        return true;
    }

    /** Dá baixa no empréstimo, libera o exemplar e gera multa se houve atraso (RF06, RF09). */
    public void registrarDevolucao(LocalDate data) {
        if (!estaAtivo()) {
            throw new RegraNegocioException("Este empréstimo já foi devolvido em " + dataDevolucao + ".");
        }
        if (data.isBefore(dataEmprestimo)) {
            throw new RegraNegocioException("A data de devolução não pode ser anterior à do empréstimo.");
        }
        this.dataDevolucao = data;
        if (exemplar.getSituacao() == SituacaoExemplar.EMPRESTADO) {
            exemplar.alterarSituacao(SituacaoExemplar.DISPONIVEL);
        }
        if (diasAtraso() > 0) {
            this.multa = new Multa(this);
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Leitor getLeitor() {
        return leitor;
    }

    public Exemplar getExemplar() {
        return exemplar;
    }

    public Bibliotecario getBibliotecario() {
        return bibliotecario;
    }

    public LocalDate getDataEmprestimo() {
        return dataEmprestimo;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public LocalDate getDataDevolucao() {
        return dataDevolucao;
    }

    public int getQtdRenovacoes() {
        return qtdRenovacoes;
    }

    public Multa getMulta() {
        return multa;
    }
}
