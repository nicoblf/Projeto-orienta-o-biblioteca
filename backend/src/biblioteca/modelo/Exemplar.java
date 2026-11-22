package biblioteca.modelo;

import biblioteca.excecao.RegraNegocioException;
import java.time.LocalDate;

/** Cópia física de um Livro, com código de tombo e situação própria. É o objeto que de fato é emprestado. */
public class Exemplar implements Identificavel {

    private int id;
    private final Livro livro;
    private final String codigoTombo;
    private SituacaoExemplar situacao = SituacaoExemplar.DISPONIVEL;
    private LocalDate dataBaixa;
    private String motivoBaixa;

    /** Construtor restrito ao pacote: só Livro.adicionarExemplar() cria exemplares (composição). */
    Exemplar(Livro livro, String codigoTombo) {
        if (codigoTombo == null || codigoTombo.isBlank()) {
            throw new RegraNegocioException("O código de tombo é obrigatório.");
        }
        this.livro = livro;
        this.codigoTombo = codigoTombo.trim().toUpperCase();
    }

    /** Reconstrução a partir do banco (chamado por Livro.restaurarExemplar). */
    void restaurarEstado(int id, SituacaoExemplar situacao, LocalDate dataBaixa, String motivoBaixa) {
        this.id = id;
        this.situacao = situacao;
        this.dataBaixa = dataBaixa;
        this.motivoBaixa = motivoBaixa;
    }

    public boolean estaDisponivel() {
        return situacao == SituacaoExemplar.DISPONIVEL;
    }

    public void alterarSituacao(SituacaoExemplar nova) {
        if (situacao == SituacaoExemplar.BAIXADO) {
            throw new RegraNegocioException("O exemplar " + codigoTombo + " foi baixado e não pode mais mudar de situação.");
        }
        this.situacao = nova;
    }

    /** Retira o exemplar de circulação (perda ou dano), preservando o histórico (RF10). */
    public void registrarBaixa(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Informe o motivo da baixa.");
        }
        if (situacao == SituacaoExemplar.BAIXADO) {
            throw new RegraNegocioException("O exemplar " + codigoTombo + " já foi baixado.");
        }
        if (situacao == SituacaoExemplar.EMPRESTADO) {
            throw new RegraNegocioException("O exemplar " + codigoTombo + " está emprestado: registre a devolução antes da baixa.");
        }
        this.situacao = SituacaoExemplar.BAIXADO;
        this.dataBaixa = LocalDate.now();
        this.motivoBaixa = motivo.trim();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public Livro getLivro() {
        return livro;
    }

    public String getCodigoTombo() {
        return codigoTombo;
    }

    public SituacaoExemplar getSituacao() {
        return situacao;
    }

    public LocalDate getDataBaixa() {
        return dataBaixa;
    }

    public String getMotivoBaixa() {
        return motivoBaixa;
    }
}
