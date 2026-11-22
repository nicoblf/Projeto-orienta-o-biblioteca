package biblioteca.modelo;

/** Toda entidade persistida tem um identificador atribuído pelo repositório. */
public interface Identificavel {
    int getId();

    void setId(int id);
}
