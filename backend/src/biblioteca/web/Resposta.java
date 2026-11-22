package biblioteca.web;

/** Resultado de uma ação de controlador: código HTTP e corpo (convertido para JSON). */
public record Resposta(int status, Object corpo) {

    public static Resposta ok(Object corpo) {
        return new Resposta(200, corpo);
    }

    public static Resposta criado(Object corpo) {
        return new Resposta(201, corpo);
    }
}
