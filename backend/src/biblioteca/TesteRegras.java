package biblioteca;

import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.Bibliotecario;
import biblioteca.modelo.Emprestimo;
import biblioteca.modelo.Exemplar;
import biblioteca.modelo.Leitor;
import biblioteca.modelo.SituacaoExemplar;
import biblioteca.util.Json;
import java.time.LocalDate;
import java.util.Map;

/** Testes simples das regras de negócio, sem biblioteca externa. Rode: java -cp out biblioteca.TesteRegras */
public final class TesteRegras {

    private static int ok = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        Aplicacao app = new Aplicacao();
        Bibliotecario maria = DadosIniciais.carregar(app);
        Leitor ana = app.leitores.buscarPorMatricula("RA2024001");
        Leitor bruno = app.leitores.buscarPorMatricula("RA2024002");
        Leitor diego = app.leitores.buscarPorMatricula("RA2024003");
        Leitor carla = app.leitores.buscarPorMatricula("PROF0001");

        // Prazo e data prevista por categoria
        Emprestimo eAna = ana.consultarEmprestimos().get(0);
        igual("prazo aluno = 7 dias", eAna.getDataEmprestimo().plusDays(7), eAna.getDataPrevista());
        igual("prazo professor = 14 dias", carla.consultarEmprestimos().get(0).getDataEmprestimo().plusDays(14),
                carla.consultarEmprestimos().get(0).getDataPrevista());

        // Atraso e multa
        Emprestimo eBruno = bruno.consultarEmprestimos().get(0);
        verdadeiro("empréstimo de Bruno está atrasado", eBruno.estaAtrasado());
        igual("dias de atraso de Bruno = 13", 13, eBruno.diasAtraso());
        verdadeiro("Bruno possui pendências", bruno.possuiPendencias());
        verdadeiro("Diego possui multa em aberto", diego.possuiPendencias());
        igual("multa de Diego = 3.00", 3.0, diego.consultarEmprestimos().get(0).getMulta().getValor());

        // Bloqueios
        bloqueia("exemplar emprestado não pode ser emprestado de novo",
                () -> app.circulacao.registrarEmprestimo(maria, "RA2024001", "TB-0001"));
        bloqueia("leitor com atraso não pode emprestar",
                () -> app.circulacao.registrarEmprestimo(maria, "RA2024002", "TB-0009"));
        bloqueia("leitor com multa não pode emprestar",
                () -> app.circulacao.registrarEmprestimo(maria, "RA2024003", "TB-0009"));

        // Limite de obras (aluno = 3)
        app.circulacao.registrarEmprestimo(maria, "RA2024001", "TB-0008");
        app.circulacao.registrarEmprestimo(maria, "RA2024001", "TB-0009");
        bloqueia("limite de 3 obras do aluno", () -> app.circulacao.registrarEmprestimo(maria, "RA2024001", "TB-0010"));

        // Disponibilidade por título
        igual("Dom Casmurro: 1 de 3 disponível", 1, app.acervo.buscar("casmurro").get(0).qtdDisponiveis());

        // Devolução libera o exemplar e atraso gera multa; renovação
        Exemplar tb4 = app.acervo.buscarExemplarPorTombo("TB-0004");
        app.circulacao.registrarDevolucao(maria, eBruno.getId());
        igual("exemplar devolvido fica DISPONIVEL", SituacaoExemplar.DISPONIVEL, tb4.getSituacao());
        verdadeiro("devolução atrasada gerou multa", eBruno.getMulta() != null);
        igual("multa de Bruno = 13 x 1.00", 13.0, eBruno.getMulta().getValor());
        bloqueia("não devolve duas vezes", () -> app.circulacao.registrarDevolucao(maria, eBruno.getId()));
        app.circulacao.quitarMulta(eBruno.getMulta().getId());
        verdadeiro("após quitar, Bruno pode emprestar", bruno.podeEmprestar());

        Emprestimo eCarla = carla.consultarEmprestimos().get(0);
        LocalDate antes = eCarla.getDataPrevista();
        verdadeiro("renovação permitida", eCarla.renovar());
        igual("renovação soma o prazo", antes.plusDays(14), eCarla.getDataPrevista());
        verdadeiro("2ª renovação", eCarla.renovar());
        verdadeiro("3ª renovação (limite do professor = 3)", eCarla.renovar());
        falso("4ª renovação negada", eCarla.renovar());

        // Baixa
        Exemplar tb11 = app.acervo.buscarExemplarPorTombo("TB-0011");
        app.acervo.registrarBaixa(tb11.getId(), "Extraviado");
        igual("exemplar baixado", SituacaoExemplar.BAIXADO, tb11.getSituacao());
        bloqueia("exemplar baixado não é emprestado",
                () -> app.circulacao.registrarEmprestimo(maria, "PROF0001", "TB-0011"));

        // Senha e perfis
        verdadeiro("senha correta autentica", maria.autenticar("admin123"));
        falso("senha errada não autentica", maria.autenticar("errada"));
        String token = app.autenticacao.login("ana@aluno.com", "leitor123");
        verdadeiro("login devolve o usuário", app.autenticacao.usuarioDoToken(token) == ana);

        // JSON
        @SuppressWarnings("unchecked")
        Map<String, Object> lido = (Map<String, Object>) Json.ler("{\"a\":\"ç \\\"x\\\"\",\"n\":3,\"l\":[1,true,null]}");
        igual("JSON: texto com aspas e acento", "ç \"x\"", lido.get("a"));
        Map<String, Object> saida = new java.util.LinkedHashMap<>();
        saida.put("k", "v\n");
        saida.put("n", java.util.List.of(1, 2.5));
        igual("JSON: escrita", "{\"k\":\"v\\n\",\"n\":[1,2.5]}", Json.escrever(saida));

        System.out.println();
        System.out.println(ok + " verificacoes OK, " + falhas + " falha(s).");
        if (falhas > 0) {
            System.exit(1);
        }
    }

    private static void igual(String nome, Object esperado, Object obtido) {
        if (esperado.equals(obtido)) {
            ok++;
            System.out.println("OK   " + nome);
        } else {
            falhas++;
            System.out.println("FALHA " + nome + " (esperado " + esperado + ", obtido " + obtido + ")");
        }
    }

    private static void verdadeiro(String nome, boolean valor) {
        igual(nome, true, valor);
    }

    private static void falso(String nome, boolean valor) {
        igual(nome, false, valor);
    }

    private static void bloqueia(String nome, Runnable acao) {
        try {
            acao.run();
            falhas++;
            System.out.println("FALHA " + nome + " (nada foi bloqueado)");
        } catch (RegraNegocioException e) {
            ok++;
            System.out.println("OK   " + nome + " -> \"" + e.getMessage() + "\"");
        }
    }
}
