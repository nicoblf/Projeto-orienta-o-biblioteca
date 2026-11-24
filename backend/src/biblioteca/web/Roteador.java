package biblioteca.web;

import biblioteca.excecao.AcessoNegadoException;
import biblioteca.excecao.RecursoNaoEncontradoException;
import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.Administrador;
import biblioteca.modelo.Bibliotecario;
import biblioteca.modelo.Leitor;
import biblioteca.modelo.Usuario;
import biblioteca.servico.AutenticacaoServico;
import biblioteca.servico.LogServico;
import biblioteca.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recebe todas as requisições HTTP: serve a interface (arquivos estáticos) e encaminha /api/* ao
 * controlador certo, verificando o perfil de acesso e traduzindo exceções em códigos HTTP.
 */
public class Roteador implements HttpHandler {

    @FunctionalInterface
    public interface Acao {
        Resposta executar(Contexto ctx);
    }

    private record Rota(String metodo, String caminho, Pattern padrao, List<String> nomes, Perfil perfil, Acao acao) {
    }

    private static final int LIMITE_CORPO = 1_000_000;

    /** Rotas liberadas enquanto o usuário ainda não trocou a senha provisória. */
    private static final Set<String> LIBERADAS_COM_SENHA_PROVISORIA = Set.of("/api/eu", "/api/logout", "/api/trocar-senha");

    /** Descrição legível de cada ação para o log de auditoria. */
    private static final Map<String, String> DESCRICOES = Map.ofEntries(
            Map.entry("POST /api/login", "Login"),
            Map.entry("POST /api/logout", "Logout"),
            Map.entry("POST /api/cadastro", "Autocadastro de leitor"),
            Map.entry("POST /api/trocar-senha", "Troca de senha"),
            Map.entry("POST /api/livros", "Cadastro de livro"),
            Map.entry("DELETE /api/livros/{id}", "Exclusão de livro"),
            Map.entry("POST /api/livros/{id}/exemplares", "Cadastro de exemplar"),
            Map.entry("POST /api/exemplares/{id}/baixa", "Baixa de exemplar"),
            Map.entry("POST /api/emprestimos", "Empréstimo"),
            Map.entry("POST /api/emprestimos/{id}/devolucao", "Devolução"),
            Map.entry("POST /api/emprestimos/{id}/renovacao", "Renovação"),
            Map.entry("POST /api/multas/{id}/quitar", "Pagamento de multa"),
            Map.entry("POST /api/leitores", "Cadastro de leitor"),
            Map.entry("POST /api/admin/funcionarios", "Cadastro de funcionário"),
            Map.entry("POST /api/admin/usuarios/{id}/redefinir-senha", "Redefinição de senha"),
            Map.entry("DELETE /api/admin/usuarios/{id}", "Exclusão de usuário"));

    /** Campos do corpo que podem aparecer no log (nunca senhas). */
    private static final List<String> CAMPOS_LOG = List.of(
            "email", "nome", "matricula", "codigoTombo", "titulo", "isbn", "motivo", "registroFuncional");

    private final List<Rota> rotas = new ArrayList<>();
    private final AutenticacaoServico autenticacao;
    private final LogServico log;
    private final Path diretorioWeb;
    /** Serializa as ações: dois atendimentos simultâneos nunca emprestam o mesmo exemplar (RNF07). */
    private final Object trava = new Object();

    public Roteador(AutenticacaoServico autenticacao, LogServico log, Path diretorioWeb) {
        this.autenticacao = autenticacao;
        this.log = log;
        this.diretorioWeb = diretorioWeb.toAbsolutePath().normalize();
    }

    public void adicionar(String metodo, String caminho, Perfil perfil, Acao acao) {
        List<String> nomes = new ArrayList<>();
        Matcher m = Pattern.compile("\\{(\\w+)}").matcher(caminho);
        while (m.find()) {
            nomes.add(m.group(1));
        }
        String regex = caminho.replaceAll("\\{\\w+}", "([^/]+)");
        rotas.add(new Rota(metodo, caminho, Pattern.compile("^" + regex + "$"), nomes, perfil, acao));
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String caminho = ex.getRequestURI().getPath();
            if (caminho.startsWith("/api/")) {
                tratarApi(ex, caminho);
            } else {
                servirArquivo(ex, caminho);
            }
        } catch (Exception e) {
            System.err.println("Erro inesperado: " + e);
            e.printStackTrace();
            responder(ex, 500, Map.of("erro", "Erro interno do servidor."));
        } finally {
            ex.close();
        }
    }

    // ---------- API ----------

    private void tratarApi(HttpExchange ex, String caminho) throws IOException {
        boolean caminhoExiste = false;
        for (Rota rota : rotas) {
            Matcher m = rota.padrao().matcher(caminho);
            if (!m.matches()) {
                continue;
            }
            caminhoExiste = true;
            if (!rota.metodo().equalsIgnoreCase(ex.getRequestMethod())) {
                continue;
            }
            Map<String, String> params = new LinkedHashMap<>();
            for (int i = 0; i < rota.nomes().size(); i++) {
                params.put(rota.nomes().get(i), URLDecoder.decode(m.group(i + 1), StandardCharsets.UTF_8));
            }
            executar(ex, rota, params);
            return;
        }
        responder(ex, caminhoExiste ? 405 : 404, Map.of("erro", caminhoExiste ? "Método não permitido." : "Rota não encontrada."));
    }

    private void executar(HttpExchange ex, Rota rota, Map<String, String> params) throws IOException {
        Usuario usuario = null;
        Map<String, Object> corpo = Map.of();
        int status;
        Object saida;
        try {
            String token = extrairToken(ex);
            usuario = autenticacao.usuarioDoToken(token);
            corpo = lerCorpo(ex);
            verificarPerfil(rota, usuario);
            Contexto ctx = new Contexto(params, lerConsulta(ex), corpo, usuario, token);
            Resposta resposta;
            synchronized (trava) {
                resposta = rota.acao().executar(ctx);
            }
            status = resposta.status();
            saida = resposta.corpo();
        } catch (RegraNegocioException e) {
            status = 422;
            saida = Map.of("erro", e.getMessage());
        } catch (RecursoNaoEncontradoException e) {
            status = 404;
            saida = Map.of("erro", e.getMessage());
        } catch (AcessoNegadoException e) {
            status = e.getStatus();
            saida = Map.of("erro", e.getMessage());
        } catch (IllegalArgumentException e) {
            status = 400;
            saida = Map.of("erro", "Requisição inválida.");
        }
        synchronized (trava) { // os repositórios em memória não são thread-safe
            auditar(ex, rota, usuario, corpo, params, status, saida);
        }
        responder(ex, status, saida);
    }

    /** Grava no log toda ação que altera dados (consultas GET não são registradas). */
    private void auditar(HttpExchange ex, Rota rota, Usuario usuario, Map<String, Object> corpo,
                         Map<String, String> params, int status, Object saida) {
        if ("GET".equalsIgnoreCase(rota.metodo())) {
            return;
        }
        String chave = rota.metodo().toUpperCase() + " " + rota.caminho();
        String acao = DESCRICOES.getOrDefault(chave, chave);

        StringBuilder detalhe = new StringBuilder();
        params.forEach((k, v) -> detalhe.append(k).append('=').append(v).append("; "));
        for (String campo : CAMPOS_LOG) {
            Object v = corpo.get(campo);
            if (v != null && !v.toString().isBlank()) {
                detalhe.append(campo).append('=').append(v).append("; ");
            }
        }

        String quem;
        String perfil;
        if (usuario != null) {
            quem = usuario.getNome() + " <" + usuario.getEmail() + ">";
            perfil = usuario.getPerfil();
        } else {
            Object email = corpo.get("email");
            quem = email == null ? "(visitante)" : email.toString();
            perfil = null;
        }

        boolean ok = status < 400;
        String resultado = ok ? "OK"
                : (saida instanceof Map<?, ?> m && m.get("erro") != null ? m.get("erro").toString() : "Erro " + status);

        String ip = ex.getRequestHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = ex.getRemoteAddress() == null ? null : ex.getRemoteAddress().getAddress().getHostAddress();
        }
        log.registrar(quem, perfil, acao, detalhe.toString().trim(), resultado, ok, ip);
    }

    private void verificarPerfil(Rota rota, Usuario usuario) {
        Perfil exigido = rota.perfil();
        if (exigido == Perfil.PUBLICO) {
            return;
        }
        if (usuario == null) {
            throw new AcessoNegadoException(401, "Faça login para continuar.");
        }
        if (usuario.isSenhaProvisoria() && !LIBERADAS_COM_SENHA_PROVISORIA.contains(rota.caminho())) {
            throw new AcessoNegadoException(403, "Troque sua senha provisória antes de continuar.");
        }
        if (exigido == Perfil.ADMIN && !(usuario instanceof Administrador)) {
            throw new AcessoNegadoException(403, "Acesso restrito ao administrador.");
        }
        if (exigido == Perfil.LEITOR && !(usuario instanceof Leitor)) {
            throw new AcessoNegadoException(403, "Acesso restrito a leitores.");
        }
        if (exigido == Perfil.BIBLIOTECARIO && !(usuario instanceof Bibliotecario)) {
            throw new AcessoNegadoException(403, "Acesso restrito ao bibliotecário.");
        }
    }

    private String extrairToken(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        return (h != null && h.startsWith("Bearer ")) ? h.substring(7).trim() : null;
    }

    private Map<String, String> lerConsulta(HttpExchange ex) {
        Map<String, String> mapa = new LinkedHashMap<>();
        String q = ex.getRequestURI().getRawQuery();
        if (q != null) {
            for (String par : q.split("&")) {
                String[] kv = par.split("=", 2);
                mapa.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
            }
        }
        return mapa;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> lerCorpo(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            byte[] bytes = in.readNBytes(LIMITE_CORPO + 1);
            if (bytes.length > LIMITE_CORPO) {
                throw new RegraNegocioException("Corpo da requisição grande demais.");
            }
            String texto = new String(bytes, StandardCharsets.UTF_8).trim();
            if (texto.isEmpty()) {
                return new LinkedHashMap<>();
            }
            Object json = Json.ler(texto);
            if (!(json instanceof Map)) {
                throw new IllegalArgumentException("O corpo deve ser um objeto JSON.");
            }
            return (Map<String, Object>) json;
        }
    }

    private void responder(HttpExchange ex, int status, Object corpo) throws IOException {
        byte[] bytes = Json.escrever(corpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---------- arquivos estáticos (interface) ----------

    private void servirArquivo(HttpExchange ex, String caminho) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            responder(ex, 405, Map.of("erro", "Método não permitido."));
            return;
        }
        String relativo = caminho.equals("/") ? "index.html" : caminho.substring(1);
        Path arquivo = diretorioWeb.resolve(relativo).normalize();
        if (!arquivo.startsWith(diretorioWeb) || !Files.isRegularFile(arquivo)) {
            responder(ex, 404, Map.of("erro", "Arquivo não encontrado."));
            return;
        }
        byte[] bytes = Files.readAllBytes(arquivo);
        ex.getResponseHeaders().set("Content-Type", tipo(arquivo.getFileName().toString()));
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String tipo(String nome) {
        if (nome.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (nome.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (nome.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (nome.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
