package biblioteca.web;

import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.Emprestimo;
import biblioteca.servico.CirculacaoServico;
import java.util.List;

/** Empréstimo, devolução, renovação, multas e painel (RF05 a RF09). */
public class CirculacaoControlador implements Controlador {

    private final CirculacaoServico circulacao;

    public CirculacaoControlador(CirculacaoServico circulacao) {
        this.circulacao = circulacao;
    }

    @Override
    public void registrar(Roteador r) {
        r.adicionar("GET", "/api/emprestimos", Perfil.BIBLIOTECARIO, ctx -> {
            String status = ctx.consulta("status") == null ? "ativos" : ctx.consulta("status");
            List<Emprestimo> lista = switch (status) {
                case "ativos" -> circulacao.listarAtivos();
                case "atrasados" -> circulacao.listarAtrasados();
                case "todos" -> circulacao.listarTodos();
                default -> throw new RegraNegocioException("Status inválido: use ativos, atrasados ou todos.");
            };
            return Resposta.ok(lista.stream().map(Apresentacao::emprestimo).toList());
        });

        r.adicionar("POST", "/api/emprestimos", Perfil.BIBLIOTECARIO, ctx -> {
            Emprestimo e = circulacao.registrarEmprestimo(ctx.bibliotecario(),
                    ctx.texto("matricula"), ctx.texto("codigoTombo"));
            return Resposta.criado(Apresentacao.emprestimo(e));
        });

        r.adicionar("POST", "/api/emprestimos/{id}/devolucao", Perfil.BIBLIOTECARIO, ctx ->
                Resposta.ok(Apresentacao.emprestimo(circulacao.registrarDevolucao(ctx.bibliotecario(), ctx.idRota("id")))));

        r.adicionar("POST", "/api/emprestimos/{id}/renovacao", Perfil.AUTENTICADO, ctx ->
                Resposta.ok(Apresentacao.emprestimo(circulacao.renovar(ctx.usuario(), ctx.idRota("id")))));

        r.adicionar("GET", "/api/meus-emprestimos", Perfil.LEITOR, ctx ->
                Resposta.ok(ctx.leitor().consultarEmprestimos().stream().map(Apresentacao::emprestimo).toList()));

        r.adicionar("POST", "/api/multas/{id}/quitar", Perfil.BIBLIOTECARIO, ctx -> {
            circulacao.quitarMulta(ctx.idRota("id"));
            return Resposta.ok(java.util.Map.of("mensagem", "Multa quitada."));
        });

        r.adicionar("GET", "/api/painel", Perfil.BIBLIOTECARIO, ctx -> Resposta.ok(circulacao.painel()));
    }
}
