package biblioteca.servico;

import biblioteca.excecao.RecursoNaoEncontradoException;
import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.Exemplar;
import biblioteca.modelo.Livro;
import biblioteca.repositorio.ExemplarRepositorio;
import biblioteca.repositorio.LivroRepositorio;
import java.text.Normalizer;
import java.util.List;

/** Cadastro e consulta do acervo: livros (títulos), exemplares (cópias) e baixas. RF01, RF02, RF04, RF10. */
public class AcervoServico {

    private final LivroRepositorio livros;
    private final ExemplarRepositorio exemplares;

    public AcervoServico(LivroRepositorio livros, ExemplarRepositorio exemplares) {
        this.livros = livros;
        this.exemplares = exemplares;
    }

    public Livro cadastrarLivro(String titulo, String autor, String editora, String isbn, String assunto) {
        Livro livro = new Livro(titulo, autor, editora, isbn, assunto);
        if (livros.buscarPorIsbn(livro.getIsbn()).isPresent()) {
            throw new RegraNegocioException("Já existe um livro cadastrado com o ISBN " + livro.getIsbn() + ".");
        }
        return livros.salvar(livro);
    }

    public Exemplar cadastrarExemplar(int livroId, String codigoTombo) {
        Livro livro = buscarLivro(livroId);
        if (codigoTombo != null && exemplares.buscarPorTombo(codigoTombo).isPresent()) {
            throw new RegraNegocioException("O código de tombo " + codigoTombo.trim().toUpperCase() + " já está em uso.");
        }
        Exemplar exemplar = livro.adicionarExemplar(codigoTombo);
        return exemplares.salvar(exemplar);
    }

    /** Consulta por título, autor, assunto ou ISBN, sem diferenciar maiúsculas nem acentos (RF04). */
    public List<Livro> buscar(String termo) {
        if (termo == null || termo.isBlank()) {
            return livros.listar();
        }
        String t = normalizar(termo);
        return livros.listar().stream()
                .filter(l -> normalizar(l.getTitulo()).contains(t)
                        || normalizar(l.getAutor()).contains(t)
                        || normalizar(l.getAssunto()).contains(t)
                        || normalizar(l.getIsbn()).contains(t))
                .toList();
    }

    public Livro buscarLivro(int id) {
        return livros.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado."));
    }

    public Exemplar buscarExemplar(int id) {
        return exemplares.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Exemplar não encontrado."));
    }

    public Exemplar buscarExemplarPorTombo(String codigoTombo) {
        return exemplares.buscarPorTombo(codigoTombo == null ? "" : codigoTombo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Exemplar com tombo " + codigoTombo + " não encontrado."));
    }

    public void registrarBaixa(int exemplarId, String motivo) {
        Exemplar exemplar = buscarExemplar(exemplarId);
        exemplar.registrarBaixa(motivo);
        exemplares.salvar(exemplar);
    }

    /** Grava no repositório a nova situação do exemplar (emprestado, devolvido...). */
    public void atualizarExemplar(Exemplar exemplar) {
        exemplares.salvar(exemplar);
    }

    public List<Exemplar> listarExemplares() {
        return exemplares.listar();
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
