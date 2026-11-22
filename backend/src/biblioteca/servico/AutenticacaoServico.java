package biblioteca.servico;

import biblioteca.excecao.AcessoNegadoException;
import biblioteca.modelo.Usuario;
import biblioteca.repositorio.UsuarioRepositorio;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Login por e-mail e senha; mantém as sessões (token -> usuário) em memória. */
public class AutenticacaoServico {

    private final UsuarioRepositorio usuarios;
    private final Map<String, Usuario> sessoes = new ConcurrentHashMap<>();

    public AutenticacaoServico(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    /** Retorna o token da sessão. A mensagem de erro é a mesma para e-mail inexistente e senha errada. */
    public String login(String email, String senha) {
        if (email == null || senha == null) {
            throw new AcessoNegadoException(401, "E-mail ou senha inválidos.");
        }
        Optional<Usuario> usuario = usuarios.buscarPorEmail(email);
        if (usuario.isEmpty() || !usuario.get().autenticar(senha)) {
            throw new AcessoNegadoException(401, "E-mail ou senha inválidos.");
        }
        String token = UUID.randomUUID().toString();
        sessoes.put(token, usuario.get());
        return token;
    }

    public Usuario usuarioDoToken(String token) {
        return token == null ? null : sessoes.get(token);
    }

    public void logout(String token) {
        if (token != null) {
            sessoes.remove(token);
        }
    }
}
