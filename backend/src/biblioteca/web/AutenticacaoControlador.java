package biblioteca.web;

import biblioteca.modelo.Usuario;
import biblioteca.servico.AutenticacaoServico;
import biblioteca.servico.LeitorServico;
import java.util.LinkedHashMap;
import java.util.Map;

/** Login, logout e dados da sessão atual. */
public class AutenticacaoControlador implements Controlador {

    private final AutenticacaoServico autenticacao;

    private final LeitorServico leitores;

    public AutenticacaoControlador(AutenticacaoServico autenticacao, LeitorServico leitores) {
        this.autenticacao = autenticacao;
        this.leitores = leitores;
    }

    @Override
    public void registrar(Roteador r) {
        r.adicionar("POST", "/api/login", Perfil.PUBLICO, ctx -> {
            String token = autenticacao.login(ctx.texto("email"), ctx.texto("senha"));
            Map<String, Object> corpo = dadosUsuario(autenticacao.usuarioDoToken(token));
            corpo.put("token", token);
            return Resposta.ok(corpo);
        });
        // Autocadastro de aluno (público): cria o leitor e já inicia a sessão.
        r.adicionar("POST", "/api/cadastro", Perfil.PUBLICO, ctx -> {
            String email = ctx.texto("email");
            String senha = ctx.texto("senha");
            leitores.autocadastrarAluno(ctx.texto("nome"), email, senha, ctx.texto("matricula"), ctx.texto("telefone"));
            String token = autenticacao.login(email, senha);
            Map<String, Object> corpo = dadosUsuario(autenticacao.usuarioDoToken(token));
            corpo.put("token", token);
            return Resposta.criado(corpo);
        });
        r.adicionar("POST", "/api/logout", Perfil.AUTENTICADO, ctx -> {
            autenticacao.logout(ctx.token());
            return Resposta.ok(Map.of("mensagem", "Sessão encerrada."));
        });
        r.adicionar("GET", "/api/eu", Perfil.AUTENTICADO, ctx -> Resposta.ok(dadosUsuario(ctx.usuario())));
        r.adicionar("POST", "/api/trocar-senha", Perfil.AUTENTICADO, ctx -> {
            autenticacao.trocarSenha(ctx.usuario(), ctx.token(), ctx.texto("senhaAtual"), ctx.texto("novaSenha"));
            return Resposta.ok(dadosUsuario(ctx.usuario()));
        });
    }

    private Map<String, Object> dadosUsuario(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", u.getNome());
        m.put("perfil", u.getPerfil());
        m.put("trocarSenha", u.isSenhaProvisoria());
        return m;
    }
}
