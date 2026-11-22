package biblioteca.modelo;

import java.time.LocalDate;

/**
 * VERSÃO POSTERIOR (RF11). A classe já existe, conforme o diagrama de classes da Fase 2, para que a
 * estrutura comporte a evolução, mas ainda não está ligada a serviços nem à interface.
 * A reserva é feita sobre o TÍTULO (Livro): qualquer cópia devolvida atende a fila.
 */
public class Reserva implements Identificavel {

    private int id;
    private final Leitor leitor;
    private final Livro livro;
    private final LocalDate dataSolicitacao;
    private int posicaoFila;
    private boolean ativa = true;

    public Reserva(Leitor leitor, Livro livro, int posicaoFila) {
        this.leitor = leitor;
        this.livro = livro;
        this.posicaoFila = posicaoFila;
        this.dataSolicitacao = LocalDate.now();
    }

    /** Avisa o leitor que há exemplar disponível (pendente: envio real do aviso). */
    public void notificar() {
        System.out.println("Aviso para " + leitor.getNome() + ": já há exemplar de \"" + livro.getTitulo() + "\".");
    }

    public void cancelar() {
        this.ativa = false;
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

    public Livro getLivro() {
        return livro;
    }

    public LocalDate getDataSolicitacao() {
        return dataSolicitacao;
    }

    public int getPosicaoFila() {
        return posicaoFila;
    }

    public boolean isAtiva() {
        return ativa;
    }
}
