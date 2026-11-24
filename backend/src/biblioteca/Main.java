package biblioteca;

import biblioteca.modelo.Bibliotecario;
import biblioteca.repositorio.Banco;
import java.nio.file.Path;

/**
 * Ponto de entrada.
 *   java -cp out:lib/postgresql.jar biblioteca.Main   -> servidor da API (usa o banco se DB_URL estiver definida)
 *   java -cp out biblioteca.Main --demo               -> demonstração em console (somente memória)
 * Variáveis: PORT, DB_URL, DB_USER, DB_PASSWORD e WEB_DIR (opcional: serve a interface a partir de uma pasta).
 */
public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--demo")) {
            Aplicacao app = new Aplicacao();
            Bibliotecario maria = DadosIniciais.carregar(app);
            Demonstracao.executar(app, maria);
            return;
        }

        Banco banco = Banco.daAmbiente();
        Aplicacao app = new Aplicacao(banco);
        app.carregarDoBanco();
        if (app.usuarios.listar().isEmpty()) {
            DadosIniciais.carregar(app);
            System.out.println("Banco vazio: dados de demonstração carregados.");
        }
        // Também cobre bancos criados antes de existir o perfil de administrador.
        app.administracao.garantirAdministrador(
                System.getenv().getOrDefault("ADMIN_EMAIL", DadosIniciais.EMAIL_ADMIN),
                System.getenv().getOrDefault("ADMIN_SENHA", DadosIniciais.SENHA_ADMIN));

        int porta = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Path web = Path.of(System.getenv().getOrDefault("WEB_DIR", "../frontend"));
        app.iniciarServidor(porta, web);
        System.out.println("API no ar na porta " + porta + (banco == null ? " (somente memória)" : " (PostgreSQL)"));
    }
}
