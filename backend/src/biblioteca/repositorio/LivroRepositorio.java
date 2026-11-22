package biblioteca.repositorio;

import biblioteca.modelo.Livro;
import java.util.Optional;

public class LivroRepositorio extends RepositorioMemoria<Livro> {

    private final Banco banco;

    public LivroRepositorio() {
        this(null);
    }

    public LivroRepositorio(Banco banco) {
        this.banco = banco;
    }

    public Optional<Livro> buscarPorIsbn(String isbn) {
        return listar().stream().filter(l -> l.getIsbn().equalsIgnoreCase(isbn.trim())).findFirst();
    }

    @Override
    protected int inserir(Livro l) {
        if (banco == null) {
            return super.inserir(l);
        }
        return banco.inserir("INSERT INTO livro (titulo, autor, editora, isbn, assunto) VALUES (?, ?, ?, ?, ?)",
                l.getTitulo(), l.getAutor(), l.getEditora(), l.getIsbn(), l.getAssunto());
    }

    public void carregar() {
        banco.consultar("SELECT * FROM livro ORDER BY id", rs -> {
            Livro l = new Livro(rs.getString("titulo"), rs.getString("autor"), rs.getString("editora"),
                    rs.getString("isbn"), rs.getString("assunto"));
            l.setId(rs.getInt("id"));
            adicionarCarregado(l);
            return l;
        });
    }
}
