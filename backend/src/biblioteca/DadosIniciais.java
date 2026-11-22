package biblioteca;

import biblioteca.modelo.Bibliotecario;
import biblioteca.modelo.CategoriaLeitor;
import biblioteca.modelo.Emprestimo;
import biblioteca.modelo.Exemplar;
import biblioteca.modelo.Leitor;
import biblioteca.modelo.Livro;
import java.time.LocalDate;

/**
 * Carga de dados FICTÍCIOS para demonstração (a Fase 3 usa memória, sem banco).
 * Inclui um empréstimo em atraso e uma multa em aberto para exibir as regras funcionando.
 */
public final class DadosIniciais {

    public static final String EMAIL_BIBLIOTECARIO = "bibliotecario@biblioteca.com";
    public static final String SENHA_BIBLIOTECARIO = "admin123";
    public static final String SENHA_LEITORES = "leitor123";

    private DadosIniciais() {
    }

    public static Bibliotecario carregar(Aplicacao app) {
        CategoriaLeitor aluno = app.categorias.salvar(new CategoriaLeitor("Aluno", 7, 3, 1.00, 2));
        CategoriaLeitor professor = app.categorias.salvar(new CategoriaLeitor("Professor", 14, 5, 0.50, 3));

        Bibliotecario maria = new Bibliotecario("Maria Souza", EMAIL_BIBLIOTECARIO, SENHA_BIBLIOTECARIO, "REG-001");
        app.usuarios.salvar(maria);

        Leitor ana = leitor(app, "Ana Lima", "ana@aluno.com", "RA2024001", aluno, "(11) 91111-0001");
        Leitor bruno = leitor(app, "Bruno Costa", "bruno@aluno.com", "RA2024002", aluno, "(11) 92222-0002");
        Leitor carla = leitor(app, "Carla Dias", "carla@escola.com", "PROF0001", professor, "(11) 93333-0003");
        Leitor diego = leitor(app, "Diego Rocha", "diego@aluno.com", "RA2024003", aluno, "(11) 94444-0004");

        Livro domCasmurro = livro(app, "Dom Casmurro", "Machado de Assis", "Editora Exemplo", "978-0-00-000001-1", "Literatura", "TB-0001", "TB-0002", "TB-0003");
        Livro cleanCode = livro(app, "Clean Code", "Robert C. Martin", "Prentice Hall", "978-0-13-235088-4", "Programação", "TB-0004", "TB-0005");
        Livro javaComoProgramar = livro(app, "Java: Como Programar", "Paul Deitel", "Pearson", "978-0-00-000003-5", "Programação", "TB-0006", "TB-0007");
        livro(app, "UML Essencial", "Martin Fowler", "Bookman", "978-0-00-000004-2", "Engenharia de Software", "TB-0008");
        livro(app, "O Pequeno Príncipe", "Antoine de Saint-Exupéry", "Editora Exemplo", "978-0-00-000005-9", "Literatura", "TB-0009", "TB-0010");
        livro(app, "Estruturas de Dados e Algoritmos em Java", "Michael Goodrich", "Bookman", "978-0-00-000006-6", "Programação", "TB-0011");

        // Títulos INDISPONÍVEIS, para demonstrar "0 de N disponíveis" na consulta pública:
        Livro algoritmos = livro(app, "Algoritmos: Teoria e Prática", "Thomas Cormen", "Elsevier", "978-0-00-000007-3", "Programação", "TB-0012");
        Livro bancoDados = livro(app, "Sistemas de Banco de Dados", "Ramez Elmasri", "Pearson", "978-0-00-000008-0", "Banco de Dados", "TB-0013", "TB-0014");
        Livro redes = livro(app, "Redes de Computadores", "Andrew Tanenbaum", "Pearson", "978-0-00-000009-7", "Redes", "TB-0015");

        LocalDate hoje = LocalDate.now();

        // Ana: empréstimo normal, em dia.
        emprestar(app, maria, ana, domCasmurro.getExemplares().get(0), hoje.minusDays(2));
        // Bruno: empréstimo em ATRASO (retirado há 20 dias, prazo de 7).
        emprestar(app, maria, bruno, cleanCode.getExemplares().get(0), hoje.minusDays(20));
        // Carla (professora): dois empréstimos em dia.
        emprestar(app, maria, carla, javaComoProgramar.getExemplares().get(0), hoje.minusDays(5));
        emprestar(app, maria, carla, domCasmurro.getExemplares().get(1), hoje.minusDays(1));
        // Diego: devolveu com 3 dias de atraso -> multa de R$ 3,00 em aberto.
        Emprestimo antigo = emprestar(app, maria, diego, cleanCode.getExemplares().get(1), hoje.minusDays(25));
        app.circulacao.registrarDevolucao(maria, antigo, hoje.minusDays(15));

        // Carla (professora) retira todos os exemplares de dois títulos (chega ao limite de 5 obras).
        emprestar(app, maria, carla, algoritmos.getExemplares().get(0), hoje.minusDays(3));
        emprestar(app, maria, carla, bancoDados.getExemplares().get(0), hoje.minusDays(4));
        emprestar(app, maria, carla, bancoDados.getExemplares().get(1), hoje.minusDays(2));
        // Único exemplar de "Redes de Computadores" foi baixado (dano): título sem nenhum exemplar em circulação.
        app.acervo.registrarBaixa(redes.getExemplares().get(0).getId(), "Danificado");

        return maria;
    }

    private static Leitor leitor(Aplicacao app, String nome, String email, String matricula, CategoriaLeitor categoria, String telefone) {
        Leitor l = new Leitor(nome, email, SENHA_LEITORES, matricula, categoria, telefone);
        app.usuarios.salvar(l);
        return l;
    }

    private static Livro livro(Aplicacao app, String titulo, String autor, String editora, String isbn,
                               String assunto, String... tombos) {
        Livro livro = app.acervo.cadastrarLivro(titulo, autor, editora, isbn, assunto);
        for (String tombo : tombos) {
            app.acervo.cadastrarExemplar(livro.getId(), tombo);
        }
        return livro;
    }

    private static Emprestimo emprestar(Aplicacao app, Bibliotecario b, Leitor l, Exemplar e, LocalDate data) {
        return app.circulacao.registrarEmprestimo(b, l, e, data);
    }
}
