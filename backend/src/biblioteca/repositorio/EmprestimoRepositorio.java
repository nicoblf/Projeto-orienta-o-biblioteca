package biblioteca.repositorio;

import biblioteca.modelo.Bibliotecario;
import biblioteca.modelo.Emprestimo;
import biblioteca.modelo.Leitor;
import java.time.LocalDate;
import java.util.List;

public class EmprestimoRepositorio extends RepositorioMemoria<Emprestimo> {

    private final Banco banco;

    public EmprestimoRepositorio() {
        this(null);
    }

    public EmprestimoRepositorio(Banco banco) {
        this.banco = banco;
    }

    public List<Emprestimo> listarAtivos() {
        return listar().stream().filter(Emprestimo::estaAtivo).toList();
    }

    public List<Emprestimo> listarAtrasados() {
        return listar().stream().filter(Emprestimo::estaAtrasado).toList();
    }

    @Override
    protected int inserir(Emprestimo e) {
        if (banco == null) {
            return super.inserir(e);
        }
        return banco.inserir("INSERT INTO emprestimo (leitor_id, exemplar_id, bibliotecario_id, data_emprestimo, "
                + "data_prevista, data_devolucao, qtd_renovacoes) VALUES (?, ?, ?, ?, ?, ?, ?)",
                e.getLeitor().getId(), e.getExemplar().getId(), e.getBibliotecario().getId(),
                e.getDataEmprestimo(), e.getDataPrevista(), e.getDataDevolucao(), e.getQtdRenovacoes());
    }

    @Override
    protected void atualizar(Emprestimo e) {
        if (banco != null) {
            banco.executar("UPDATE emprestimo SET data_prevista = ?, data_devolucao = ?, qtd_renovacoes = ? WHERE id = ?",
                    e.getDataPrevista(), e.getDataDevolucao(), e.getQtdRenovacoes(), e.getId());
        }
    }

    public void carregar(UsuarioRepositorio usuarios, ExemplarRepositorio exemplares) {
        banco.consultar("SELECT * FROM emprestimo ORDER BY id", rs -> {
            Leitor leitor = (Leitor) usuarios.buscarPorId(rs.getInt("leitor_id")).orElseThrow();
            Bibliotecario bib = (Bibliotecario) usuarios.buscarPorId(rs.getInt("bibliotecario_id")).orElseThrow();
            var exemplar = exemplares.buscarPorId(rs.getInt("exemplar_id")).orElseThrow();
            Emprestimo e = Emprestimo.restaurar(rs.getInt("id"), leitor, exemplar, bib,
                    rs.getObject("data_emprestimo", LocalDate.class), rs.getObject("data_prevista", LocalDate.class),
                    rs.getObject("data_devolucao", LocalDate.class), rs.getInt("qtd_renovacoes"));
            adicionarCarregado(e);
            return e;
        });
    }
}
