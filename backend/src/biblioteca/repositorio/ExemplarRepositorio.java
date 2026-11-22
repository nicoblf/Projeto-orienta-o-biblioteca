package biblioteca.repositorio;

import biblioteca.modelo.Exemplar;
import biblioteca.modelo.SituacaoExemplar;
import java.time.LocalDate;
import java.util.Optional;

public class ExemplarRepositorio extends RepositorioMemoria<Exemplar> {

    private final Banco banco;

    public ExemplarRepositorio() {
        this(null);
    }

    public ExemplarRepositorio(Banco banco) {
        this.banco = banco;
    }

    public Optional<Exemplar> buscarPorTombo(String codigoTombo) {
        return listar().stream().filter(e -> e.getCodigoTombo().equalsIgnoreCase(codigoTombo.trim())).findFirst();
    }

    @Override
    protected int inserir(Exemplar e) {
        if (banco == null) {
            return super.inserir(e);
        }
        return banco.inserir("INSERT INTO exemplar (livro_id, codigo_tombo, situacao, data_baixa, motivo_baixa) "
                + "VALUES (?, ?, ?, ?, ?)", e.getLivro().getId(), e.getCodigoTombo(), e.getSituacao().name(),
                e.getDataBaixa(), e.getMotivoBaixa());
    }

    @Override
    protected void atualizar(Exemplar e) {
        if (banco != null) {
            banco.executar("UPDATE exemplar SET situacao = ?, data_baixa = ?, motivo_baixa = ? WHERE id = ?",
                    e.getSituacao().name(), e.getDataBaixa(), e.getMotivoBaixa(), e.getId());
        }
    }

    public void carregar(LivroRepositorio livros) {
        banco.consultar("SELECT * FROM exemplar ORDER BY id", rs -> {
            var livro = livros.buscarPorId(rs.getInt("livro_id")).orElseThrow();
            Exemplar e = livro.restaurarExemplar(rs.getInt("id"), rs.getString("codigo_tombo"),
                    SituacaoExemplar.valueOf(rs.getString("situacao")),
                    rs.getObject("data_baixa", LocalDate.class), rs.getString("motivo_baixa"));
            adicionarCarregado(e);
            return e;
        });
    }
}
