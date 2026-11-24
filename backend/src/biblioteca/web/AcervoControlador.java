package biblioteca.web;

import biblioteca.modelo.Exemplar;
import biblioteca.modelo.Livro;
import biblioteca.servico.AcervoServico;
import java.util.Map;

/** Consulta pública do acervo (RF04) e cadastros de livro, exemplar e baixa (RF01, RF02, RF10). */
public class AcervoControlador implements Controlador {

    private final AcervoServico acervo;

    public AcervoControlador(AcervoServico acervo) {
        this.acervo = acervo;
    }

    @Override
    public void registrar(Roteador r) {
        r.adicionar("GET", "/api/livros", Perfil.PUBLICO, ctx ->
                Resposta.ok(acervo.buscar(ctx.consulta("q")).stream().map(Apresentacao::livro).toList()));

        r.adicionar("GET", "/api/livros/{id}", Perfil.PUBLICO, ctx ->
                Resposta.ok(Apresentacao.livro(acervo.buscarLivro(ctx.idRota("id")))));

        r.adicionar("POST", "/api/livros", Perfil.BIBLIOTECARIO, ctx -> {
            Livro livro = acervo.cadastrarLivro(ctx.texto("titulo"), ctx.texto("autor"),
                    ctx.texto("editora"), ctx.texto("isbn"), ctx.texto("assunto"));
            return Resposta.criado(Apresentacao.livro(livro));
        });

        r.adicionar("DELETE", "/api/livros/{id}", Perfil.BIBLIOTECARIO, ctx -> {
            acervo.excluirLivro(ctx.idRota("id"));
            return Resposta.ok(Map.of("mensagem", "Livro excluído."));
        });

        r.adicionar("GET", "/api/livros/{id}/exemplares",Perfil.BIBLIOTECARIO, ctx ->
                Resposta.ok(acervo.buscarLivro(ctx.idRota("id")).getExemplares().stream()
                        .map(Apresentacao::exemplar).toList()));

        r.adicionar("POST", "/api/livros/{id}/exemplares", Perfil.BIBLIOTECARIO, ctx -> {
            Exemplar e = acervo.cadastrarExemplar(ctx.idRota("id"), ctx.texto("codigoTombo"));
            return Resposta.criado(Apresentacao.exemplar(e));
        });

        r.adicionar("POST", "/api/exemplares/{id}/baixa", Perfil.BIBLIOTECARIO, ctx -> {
            acervo.registrarBaixa(ctx.idRota("id"), ctx.texto("motivo"));
            return Resposta.ok(Apresentacao.exemplar(acervo.buscarExemplar(ctx.idRota("id"))));
        });
    }
}
