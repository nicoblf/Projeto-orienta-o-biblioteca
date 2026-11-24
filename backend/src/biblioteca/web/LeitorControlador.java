package biblioteca.web;

import biblioteca.modelo.Leitor;
import biblioteca.servico.AdministracaoServico;
import biblioteca.servico.LeitorServico;
import java.util.LinkedHashMap;
import java.util.Map;

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

        // O bibliotecário não escolhe senha: o sistema gera uma provisória, trocada no 1º login.
        r.adicionar("POST", "/api/leitores", Perfil.BIBLIOTECARIO, ctx -> {
            AdministracaoServico.ComSenha<Leitor> novo = leitores.cadastrarComSenhaProvisoria(ctx.texto("nome"),
                    ctx.texto("email"), ctx.texto("matricula"), ctx.inteiro("categoriaId"), ctx.texto("telefone"));
            Map<String, Object> corpo = new LinkedHashMap<>(Apresentacao.leitor(novo.usuario()));
            corpo.put("senhaProvisoria", novo.senhaProvisoria());
            return Resposta.criado(corpo);
        });
    }
}
