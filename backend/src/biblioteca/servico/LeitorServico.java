package biblioteca.servico;

import biblioteca.excecao.RecursoNaoEncontradoException;
import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.CategoriaLeitor;
import biblioteca.modelo.Leitor;
import biblioteca.repositorio.CategoriaRepositorio;
import biblioteca.repositorio.UsuarioRepositorio;
import java.util.List;

/** Cadastro de leitores e consulta das categorias (RF03). */
public class LeitorServico {

    private final UsuarioRepositorio usuarios;
    private final CategoriaRepositorio categorias;

    public LeitorServico(UsuarioRepositorio usuarios, CategoriaRepositorio categorias) {
        this.usuarios = usuarios;
        this.categorias = categorias;
    }

    public Leitor cadastrar(String nome, String email, String senha, String matricula, int categoriaId, String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new RegraNegocioException("O telefone é obrigatório.");
        }
        CategoriaLeitor categoria = categorias.buscarPorId(categoriaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
        Leitor leitor = new Leitor(nome, email, senha, matricula, categoria, telefone);
        if (usuarios.buscarPorEmail(leitor.getEmail()).isPresent()) {
            throw new RegraNegocioException("Já existe um usuário com o e-mail " + leitor.getEmail() + ".");
        }
        if (usuarios.buscarLeitorPorMatricula(leitor.getMatricula()).isPresent()) {
            throw new RegraNegocioException("Já existe um leitor com a matrícula " + leitor.getMatricula() + ".");
        }
        usuarios.salvar(leitor);
        return leitor;
    }

    /** Autocadastro público: o novo leitor entra sempre na categoria "Aluno" (o bibliotecário cadastra as demais). */
    public Leitor autocadastrarAluno(String nome, String email, String senha, String matricula, String telefone) {
        CategoriaLeitor aluno = categorias.listar().stream()
                .filter(c -> c.getNome().equalsIgnoreCase("Aluno"))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Categoria Aluno não configurada."));
        return cadastrar(nome, email, senha, matricula, aluno.getId(), telefone);
    }

    public Leitor buscarPorMatricula(String matricula) {
        return usuarios.buscarLeitorPorMatricula(matricula == null ? "" : matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Leitor com matrícula " + matricula + " não encontrado."));
    }

    public List<Leitor> listar() {
        return usuarios.listarLeitores();
    }

    public List<CategoriaLeitor> listarCategorias() {
        return categorias.listar();
    }
}
