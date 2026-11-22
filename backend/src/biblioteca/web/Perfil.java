package biblioteca.web;

/** Perfil exigido por uma rota (RNF03): visitante não precisa de login; os demais precisam. */
public enum Perfil {
    PUBLICO,
    AUTENTICADO,
    LEITOR,
    BIBLIOTECARIO
}
