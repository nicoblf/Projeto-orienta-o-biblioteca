package biblioteca.repositorio;

import biblioteca.modelo.CategoriaLeitor;

public class CategoriaRepositorio extends RepositorioMemoria<CategoriaLeitor> {

    private final Banco banco;

    public CategoriaRepositorio() {
        this(null);
    }

    public CategoriaRepositorio(Banco banco) {
        this.banco = banco;
    }

    @Override
    protected int inserir(CategoriaLeitor c) {
        if (banco == null) {
            return super.inserir(c);
        }
        return banco.inserir("INSERT INTO categoria_leitor (nome, prazo_dias, limite_obras, valor_multa_diaria, limite_renovacoes) "
                + "VALUES (?, ?, ?, ?, ?)", c.getNome(), c.getPrazoDias(), c.getLimiteObras(),
                c.getValorMultaDiaria(), c.getLimiteRenovacoes());
    }

    public void carregar() {
        banco.consultar("SELECT * FROM categoria_leitor ORDER BY id", rs -> {
            CategoriaLeitor c = new CategoriaLeitor(rs.getString("nome"), rs.getInt("prazo_dias"),
                    rs.getInt("limite_obras"), rs.getDouble("valor_multa_diaria"), rs.getInt("limite_renovacoes"));
            c.setId(rs.getInt("id"));
            adicionarCarregado(c);
            return c;
        });
    }
}
