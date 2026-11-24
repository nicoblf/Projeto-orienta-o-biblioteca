package biblioteca.servico;

import biblioteca.excecao.RecursoNaoEncontradoException;
import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.Exemplar;
import biblioteca.modelo.Livro;
import biblioteca.repositorio.EmprestimoRepositorio;
import biblioteca.repositorio.ExemplarRepositorio;
import biblioteca.repositorio.LivroRepositorio;
import java.text.Normalizer;
import java.util.List;

/** Cadastro e consulta do acervo: livros (títulos), exemplares (cópias) e baixas. RF01, RF02, RF04, RF10. */
public class AcervoServico {

    private final LivroRepositorio livros;
    private final ExemplarRepositorio exemplares;
    private final EmprestimoRepositorio emprestimos;

    public AcervoServico(LivroRepositorio livros, ExemplarRepositorio exemplares, EmprestimoRepositorio emprestimos) {
        this.livros = livros;
        this.exemplares = exemplares;
        this.emprestimos = emprestimos;
    }

    /**
     * Exclui um título e seus exemplares. Só é permitido se nenhum exemplar tiver histórico de
     * empréstimo; caso contrário o histórico se perderia, e a saída correta é dar baixa nos exemplares.
     */
    public void excluirLivro(int livroId) {
        Livro livro = buscarLivro(livroId);
        boolean temHistorico = emprestimos.listar().stream()
                .anyMatch(e -> e.getExemplar().getLivro().getId() == livroId);
        if (temHistorico) {
            throw new RegraNegocioException("Não é possível excluir \"" + livro.getTitulo()
                    + "\": seus exemplares têm histórico de empréstimos. Dê baixa nos exemplares em vez de excluir.");
        }
        for (Exemplar e : livro.getExemplares()) {
            exemplares.remover(e);
        }
        livros.remover(livro);
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
