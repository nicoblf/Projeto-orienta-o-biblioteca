package biblioteca.web;

import biblioteca.modelo.Leitor;
import biblioteca.servico.LeitorServico;

/** Cadastro e listagem de leitores (RF03). Dados pessoais só são visíveis ao bibliotecário. */
public class LeitorControlador implements Controlador {

    private final LeitorServico leitores;

    public LeitorControlador(LeitorServico leitores) {
        this.leitores = leitores;
    }

    @Override
    public void registrar(Roteador r) {
        r.adicionar("GET", "/api/categorias", Perfil.BIBLIOTECARIO, ctx ->
                Resposta.ok(leitores.listarCategorias().stream().map(Apresentacao::categoria).toList()));

        r.adicionar("GET", "/api/leitores", Perfil.BIBLIOTECARIO, ctx ->
                Resposta.ok(leitores.listar().stream().map(Apresentacao::leitor).toList()));

        r.adicionar("POST", "/api/leitores", Perfil.BIBLIOTECARIO, ctx -> {
            Leitor l = leitores.cadastrar(ctx.texto("nome"), ctx.texto("email"), ctx.texto("senha"),
                    ctx.texto("matricula"), ctx.inteiro("categoriaId"));
            return Resposta.criado(Apresentacao.leitor(l));
        });
    }
}
