package biblioteca.repositorio;

import biblioteca.modelo.Bibliotecario;
import biblioteca.modelo.CategoriaLeitor;
import biblioteca.modelo.Leitor;
import biblioteca.modelo.Usuario;
import java.util.List;
import java.util.Optional;

public class UsuarioRepositorio extends RepositorioMemoria<Usuario> {

    private final Banco banco;

    public UsuarioRepositorio() {
        this(null);
    }

    public UsuarioRepositorio(Banco banco) {
        this.banco = banco;
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return listar().stream().filter(u -> u.getEmail().equalsIgnoreCase(email.trim())).findFirst();
    }

    public List<Leitor> listarLeitores() {
        return listar().stream().filter(u -> u instanceof Leitor).map(u -> (Leitor) u).toList();
    }

    public Optional<Leitor> buscarLeitorPorMatricula(String matricula) {
        return listarLeitores().stream().filter(l -> l.getMatricula().equalsIgnoreCase(matricula.trim())).findFirst();
    }

    @Override
    protected int inserir(Usuario u) {
        if (banco == null) {
            return super.inserir(u);
        }
        if (u instanceof Leitor l) {
            return banco.inserir("INSERT INTO usuario (tipo, nome, email, senha_hash, matricula, categoria_id, telefone) "
                    + "VALUES ('LEITOR', ?, ?, ?, ?, ?, ?)", l.getNome(), l.getEmail(), l.getSenhaHash(),
                    l.getMatricula(), l.getCategoria().getId(), l.getTelefone());
        }
        Bibliotecario b = (Bibliotecario) u;
        return banco.inserir("INSERT INTO usuario (tipo, nome, email, senha_hash, registro_funcional) "
                + "VALUES ('BIBLIOTECARIO', ?, ?, ?, ?)", b.getNome(), b.getEmail(), b.getSenhaHash(),
                b.getRegistroFuncional());
    }

    @Override
    protected void atualizar(Usuario u) {
        if (banco != null) {
            banco.executar("UPDATE usuario SET nome = ?, email = ? WHERE id = ?", u.getNome(), u.getEmail(), u.getId());
        }
    }

    public void carregar(CategoriaRepositorio categorias) {
        banco.consultar("SELECT * FROM usuario ORDER BY id", rs -> {
            Usuario u;
            if ("LEITOR".equals(rs.getString("tipo"))) {
                CategoriaLeitor cat = categorias.buscarPorId(rs.getInt("categoria_id")).orElseThrow();
                u = Leitor.restaurar(rs.getInt("id"), rs.getString("nome"), rs.getString("email"),
                        rs.getString("senha_hash"), rs.getString("matricula"), cat, rs.getString("telefone"));
            } else {
                u = Bibliotecario.restaurar(rs.getInt("id"), rs.getString("nome"), rs.getString("email"),
                        rs.getString("senha_hash"), rs.getString("registro_funcional"));
            }
            adicionarCarregado(u);
            return u;
        });
    }
}
