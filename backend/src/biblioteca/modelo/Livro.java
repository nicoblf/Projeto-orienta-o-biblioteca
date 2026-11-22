package biblioteca.modelo;

import biblioteca.excecao.RegraNegocioException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa o TÍTULO da obra (não a cópia física). Mantém uma composição com Exemplar:
 * um exemplar só é criado por meio de um livro e não existe sem ele.
 */
public class Livro implements Identificavel {

    private int id;
    private final String titulo;
    private final String autor;
    private final String editora;
    private final String isbn;
    private final String assunto;
    private final List<Exemplar> exemplares = new ArrayList<>();

    public Livro(String titulo, String autor, String editora, String isbn, String assunto) {
        exigir(titulo, "título");
        exigir(autor, "autor");
        exigir(editora, "editora");
        exigir(isbn, "ISBN");
        exigir(assunto, "assunto");
        this.titulo = titulo.trim();
        this.autor = autor.trim();
        this.editora = editora.trim();
        this.isbn = isbn.trim();
        this.assunto = assunto.trim();
    }

    private static void exigir(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException("O campo " + campo + " é obrigatório.");
        }
    }

    /** Cria uma nova cópia física vinculada a este título (RF02). */
    public Exemplar adicionarExemplar(String codigoTombo) {
        Exemplar exemplar = new Exemplar(this, codigoTombo);
        exemplares.add(exemplar);
        return exemplar;
    }

    /** Reconstrói um exemplar gravado no banco, mantendo a composição com este livro. */
    public Exemplar restaurarExemplar(int id, String codigoTombo, SituacaoExemplar situacao,
                                      java.time.LocalDate dataBaixa, String motivoBaixa) {
        Exemplar exemplar = new Exemplar(this, codigoTombo);
        exemplar.restaurarEstado(id, situacao, dataBaixa, motivoBaixa);
        exemplares.add(exemplar);
        return exemplar;
    }

    public int qtdDisponiveis() {
        int total = 0;
        for (Exemplar e : exemplares) {
            if (e.estaDisponivel()) {
                total++;
            }
        }
        return total;
    }

    /** Exemplares ainda em circulação (exclui os baixados). */
    public int qtdTotal() {
        int total = 0;
        for (Exemplar e : exemplares) {
            if (e.getSituacao() != SituacaoExemplar.BAIXADO) {
                total++;
            }
        }
        return total;
    }

    public List<Exemplar> getExemplares() {
        return Collections.unmodifiableList(exemplares);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public String getEditora() {
        return editora;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getAssunto() {
        return assunto;
    }
}
