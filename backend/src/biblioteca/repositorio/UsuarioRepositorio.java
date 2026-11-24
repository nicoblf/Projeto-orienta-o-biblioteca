package biblioteca.repositorio;

import biblioteca.modelo.Administrador;
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
            return banco.inserir("INSERT INTO usuario (tipo, nome, email, senha_hash, senha_provisoria, matricula, categoria_id, telefone) "
                    + "VALUES ('LEITOR', ?, ?, ?, ?, ?, ?, ?)", l.getNome(), l.getEmail(), l.getSenhaHash(),
                    l.isSenhaProvisoria(), l.getMatricula(), l.getCategoria().getId(), l.getTelefone());
        }
        if (u instanceof Administrador a) {
            return banco.inserir("INSERT INTO usuario (tipo, nome, email, senha_hash, senha_provisoria) "
                    + "VALUES ('ADMIN', ?, ?, ?, ?)", a.getNome(), a.getEmail(), a.getSenhaHash(), a.isSenhaProvisoria());
        }
        Bibliotecario b = (Bibliotecario) u;
        return banco.inserir("INSERT INTO usuario (tipo, nome, email, senha_hash, senha_provisoria, registro_funcional) "
                + "VALUES ('BIBLIOTECARIO', ?, ?, ?, ?, ?)", b.getNome(), b.getEmail(), b.getSenhaHash(),
                b.isSenhaProvisoria(), b.getRegistroFuncional());
    }

    @Override
    protected void atualizar(Usuario u) {
        if (banco != null) {
            banco.executar("UPDATE usuario SET nome = ?, email = ?, senha_hash = ?, senha_provisoria = ?, ativo = ? WHERE id = ?",
                    u.getNome(), u.getEmail(), u.getSenhaHash(), u.isSenhaProvisoria(), u.isAtivo(), u.getId());
        }
    }

    @Override
    protected void excluir(Usuario u) {
        if (banco != null) {
            banco.executar("DELETE FROM usuario WHERE id = ?", u.getId());
        }
    }

    public boolean existeAdministrador() {
        return listar().stream().anyMatch(u -> u instanceof Administrador);
    }

    public void carregar(CategoriaRepositorio categorias) {
        banco.consultar("SELECT * FROM usuario ORDER BY id", rs -> {
            Usuario u;
            String tipo = rs.getString("tipo");
            if ("LEITOR".equals(tipo)) {
                CategoriaLeitor cat = categorias.buscarPorId(rs.getInt("categoria_id")).orElseThrow();
                u = Leitor.restaurar(rs.getInt("id"), rs.getString("nome"), rs.getString("email"),
                        rs.getString("senha_hash"), rs.getString("matricula"), cat, rs.getString("telefone"));
            } else if ("ADMIN".equals(tipo)) {
                u = Administrador.restaurar(rs.getInt("id"), rs.getString("nome"), rs.getString("email"),
                        rs.getString("senha_hash"));
            } else {
                u = Bibliotecario.restaurar(rs.getInt("id"), rs.getString("nome"), rs.getString("email"),
                        rs.getString("senha_hash"), rs.getString("registro_funcional"));
            }
            u.restaurarSenhaProvisoria(rs.getBoolean("senha_provisoria"));
            u.restaurarAtivo(rs.getBoolean("ativo"));
            adicionarCarregado(u);
            return u;
        });
    }
}
