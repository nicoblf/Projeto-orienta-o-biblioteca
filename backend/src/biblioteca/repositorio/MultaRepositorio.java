package biblioteca.repositorio;

import biblioteca.modelo.Multa;
import java.time.LocalDate;

public class MultaRepositorio extends RepositorioMemoria<Multa> {

    private final Banco banco;

    public MultaRepositorio() {
        this(null);
    }

    public MultaRepositorio(Banco banco) {
        this.banco = banco;
    }

    @Override
    protected int inserir(Multa m) {
        if (banco == null) {
            return super.inserir(m);
        }
        return banco.inserir("INSERT INTO multa (emprestimo_id, valor, data_geracao, paga) VALUES (?, ?, ?, ?)",
                m.getEmprestimo().getId(), m.getValor(), m.getDataGeracao(), m.isPaga());
    }

    @Override
    protected void atualizar(Multa m) {
        if (banco != null) {
            banco.executar("UPDATE multa SET paga = ? WHERE id = ?", m.isPaga(), m.getId());
        }
    }

    public void carregar(EmprestimoRepositorio emprestimos) {
        banco.consultar("SELECT * FROM multa ORDER BY id", rs -> {
            var emprestimo = emprestimos.buscarPorId(rs.getInt("emprestimo_id")).orElseThrow();
            Multa m = emprestimo.restaurarMulta(rs.getInt("id"), rs.getDouble("valor"),
                    rs.getObject("data_geracao", LocalDate.class), rs.getBoolean("paga"));
            adicionarCarregado(m);
            return m;
        });
    }
}
