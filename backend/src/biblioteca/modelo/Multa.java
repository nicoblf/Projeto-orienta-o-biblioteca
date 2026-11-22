package biblioteca.modelo;

import biblioteca.excecao.RegraNegocioException;
import java.time.LocalDate;

/** Valor gerado por um empréstimo devolvido com atraso. Enquanto não paga, bloqueia novos empréstimos. */
public class Multa implements Identificavel {

    private int id;
    private final Emprestimo emprestimo;
    private final double valor;
    private final LocalDate dataGeracao;
    private boolean paga;

    /** Criada apenas por Emprestimo.registrarDevolucao(), quando há atraso. */
    Multa(Emprestimo emprestimo) {
        this.emprestimo = emprestimo;
        this.dataGeracao = emprestimo.getDataDevolucao();
        this.valor = calcularValor();
    }

    /** Reconstrução a partir do banco (usada por Emprestimo.restaurarMulta). */
    Multa(int id, Emprestimo emprestimo, double valor, LocalDate dataGeracao, boolean paga) {
        this.id = id;
        this.emprestimo = emprestimo;
        this.valor = valor;
        this.dataGeracao = dataGeracao;
        this.paga = paga;
    }

    /** Dias de atraso x multa diária da categoria do leitor (RF09), arredondado em centavos. */
    public double calcularValor() {
        double bruto = emprestimo.diasAtraso() * emprestimo.getLeitor().getCategoria().getValorMultaDiaria();
        return Math.round(bruto * 100.0) / 100.0;
    }

    public void quitar() {
        if (paga) {
            throw new RegraNegocioException("Esta multa já foi paga.");
        }
        this.paga = true;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Emprestimo getEmprestimo() {
        return emprestimo;
    }

    public double getValor() {
        return valor;
    }

    public LocalDate getDataGeracao() {
        return dataGeracao;
    }

    public boolean isPaga() {
        return paga;
    }
}
