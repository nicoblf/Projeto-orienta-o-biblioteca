package biblioteca.web;

import biblioteca.modelo.Administrador;
import biblioteca.modelo.Bibliotecario;
import biblioteca.servico.AdministracaoServico;
import biblioteca.servico.LogServico;
import java.util.LinkedHashMap;
import java.util.Map;

/** Área do administrador: funcionários, redefinição de senhas e log de auditoria. */
public class AdministracaoControlador implements Controlador {

    private static final int LIMITE_LOGS = 1000;

    private final AdministracaoServico administracao;
    private final LogServico log;

    public AdministracaoControlador(AdministracaoServico administracao, LogServico log) {
        this.administracao = administracao;
        this.log = log;
    }

    @Override
    public void registrar(Roteador r) {
        r.adicionar("GET", "/api/admin/usuarios", Perfil.ADMIN, ctx ->
                Resposta.ok(administracao.listarUsuarios().stream().map(Apresentacao::usuario).toList()));

        r.adicionar("POST", "/api/admin/funcionarios", Perfil.ADMIN, ctx -> {
            AdministracaoServico.ComSenha<Bibliotecario> novo = administracao.cadastrarFuncionario(
                    ctx.texto("nome"), ctx.texto("email"), ctx.texto("registroFuncional"));
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("usuario", Apresentacao.usuario(novo.usuario()));
            corpo.put("senhaProvisoria", novo.senhaProvisoria());
            return Resposta.criado(corpo);
        });

        r.adicionar("POST", "/api/admin/usuarios/{id}/redefinir-senha", Perfil.ADMIN, ctx -> {
            var res = administracao.redefinirSenha((Administrador) ctx.usuario(), ctx.idRota("id"));
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("usuario", Apresentacao.usuario(res.usuario()));
            corpo.put("senhaProvisoria", res.senhaProvisoria());
            return Resposta.ok(corpo);
        });

        r.adicionar("DELETE", "/api/admin/usuarios/{id}", Perfil.ADMIN, ctx -> {
            boolean apagado = administracao.excluirUsuario((Administrador) ctx.usuario(), ctx.idRota("id"));
            return Resposta.ok(Map.of("apagado", apagado, "mensagem", apagado
                    ? "Usuário excluído."
                    : "Usuário desativado: ele tem histórico de empréstimos, então o cadastro foi mantido no histórico, sem acesso ao sistema."));
        });

        r.adicionar("GET", "/api/admin/logs", Perfil.ADMIN, ctx ->
                Resposta.ok(log.listarRecentes(LIMITE_LOGS).stream().map(Apresentacao::log).toList()));
    }
}
