package biblioteca.web;

/** Cada controlador registra as suas rotas no Roteador. */
public interface Controlador {
    void registrar(Roteador roteador);
}
