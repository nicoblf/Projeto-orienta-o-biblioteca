package biblioteca;

import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.Bibliotecario;
import biblioteca.modelo.Emprestimo;
import biblioteca.modelo.Exemplar;
import biblioteca.modelo.Leitor;
import biblioteca.modelo.Livro;

/**
 * Demonstração em console, no estilo do roteiro do professor: cria objetos, relaciona-os e executa
 * as regras principais, imprimindo o resultado. Rode com: java -cp out biblioteca.Main --demo
 */
public final class Demonstracao {

    private Demonstracao() {
    }

    public static void executar(Aplicacao app, Bibliotecario maria) {
        titulo("1. Consulta ao acervo (RF04) - busca por \"programacao\"");
        for (Livro l : app.acervo.buscar("programacao")) {
            System.out.printf("  %-42s %d de %d exemplar(es) disponivel(is)%n", l.getTitulo(), l.qtdDisponiveis(), l.qtdTotal());
        }

        Leitor ana = app.leitores.buscarPorMatricula("RA2024001");
        Leitor bruno = app.leitores.buscarPorMatricula("RA2024002");
        Leitor diego = app.leitores.buscarPorMatricula("RA2024003");
        Exemplar umlLivre = app.acervo.buscarExemplarPorTombo("TB-0008");
        Exemplar domEmprestado = app.acervo.buscarExemplarPorTombo("TB-0001");

        titulo("2. Empréstimo válido (RF05): " + ana.getNome() + " leva o exemplar " + umlLivre.getCodigoTombo());
        Emprestimo e1 = app.circulacao.registrarEmprestimo(maria, ana.getMatricula(), umlLivre.getCodigoTombo());
        System.out.println("  Registrado. Devolver ate " + e1.getDataPrevista()
                + " | situacao do exemplar: " + umlLivre.getSituacao());

        titulo("3. Exemplar já emprestado (RF07)");
        tentar(() -> app.circulacao.registrarEmprestimo(maria, "RA2024001", domEmprestado.getCodigoTombo()));

        titulo("4. Leitor com atraso (RF08): " + bruno.getNome());
        tentar(() -> app.circulacao.registrarEmprestimo(maria, "RA2024002", "TB-0009"));

        titulo("5. Leitor com multa em aberto (RF08): " + diego.getNome());
        tentar(() -> app.circulacao.registrarEmprestimo(maria, "RA2024003", "TB-0009"));

        titulo("6. Limite de obras (RF08): " + ana.getNome() + " (aluna, limite 3)");
        app.circulacao.registrarEmprestimo(maria, "RA2024001", "TB-0009");
        System.out.println("  Ana agora tem " + ana.qtdEmprestimosAtivos() + " emprestimos ativos.");
        tentar(() -> app.circulacao.registrarEmprestimo(maria, "RA2024001", "TB-0010"));

        titulo("7. Devolução com atraso gera multa automática (RF06, RF09): " + bruno.getNome());
        Emprestimo atrasado = app.circulacao.listarAtrasados().get(0);
        System.out.println("  Dias de atraso: " + atrasado.diasAtraso());
        app.circulacao.registrarDevolucao(maria, atrasado.getId());
        System.out.printf("  Multa gerada: R$ %.2f (paga: %s)%n", atrasado.getMulta().getValor(), atrasado.getMulta().isPaga());

        titulo("8. Quitar a multa libera o leitor");
        System.out.println("  Bruno possui pendencias? " + bruno.possuiPendencias());
        app.circulacao.quitarMulta(atrasado.getMulta().getId());
        System.out.println("  Apos quitar: possui pendencias? " + bruno.possuiPendencias()
                + " | pode emprestar? " + bruno.podeEmprestar());

        titulo("9. Renovação (RF06)");
        Emprestimo renov = e1;
        System.out.println("  Prazo antes: " + renov.getDataPrevista());
        renov.renovar();
        System.out.println("  Prazo depois: " + renov.getDataPrevista() + " (" + renov.getQtdRenovacoes() + " renovacao)");

        titulo("10. Baixa de exemplar (RF10)");
        app.acervo.registrarBaixa(app.acervo.buscarExemplarPorTombo("TB-0011").getId(), "Extraviado");
        Livro goodrich = app.acervo.buscar("goodrich").get(0);
        System.out.println("  " + goodrich.getTitulo() + ": " + goodrich.qtdDisponiveis() + " disponivel(is), "
                + goodrich.qtdTotal() + " em circulacao");

        titulo("Painel");
        app.circulacao.painel().forEach((k, v) -> System.out.println("  " + k + ": " + v));
    }

    private static void tentar(Runnable acao) {
        try {
            acao.run();
            System.out.println("  (!) deveria ter sido bloqueado");
        } catch (RegraNegocioException e) {
            System.out.println("  BLOQUEADO: " + e.getMessage());
        }
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println(texto);
        System.out.println("-".repeat(Math.min(texto.length(), 78)));
    }
}
