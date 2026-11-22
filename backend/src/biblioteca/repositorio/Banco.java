package biblioteca.repositorio;

import biblioteca.excecao.ErroPersistenciaException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso JDBC ao PostgreSQL. Toda consulta usa PreparedStatement com parâmetros (nunca concatena
 * valores no SQL), o que impede injeção de SQL.
 */
public final class Banco {

    @FunctionalInterface
    public interface Mapeador<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    private final String url;
    private final String usuario;
    private final String senha;

    public Banco(String url, String usuario, String senha) {
        this.url = url;
        this.usuario = usuario;
        this.senha = senha;
    }

    /** Lê DB_URL, DB_USER e DB_PASSWORD. Sem DB_URL, a aplicação usa somente memória. */
    public static Banco daAmbiente() {
        String url = System.getenv("DB_URL");
        if (url == null || url.isBlank()) {
            return null;
        }
        return new Banco(url, System.getenv().getOrDefault("DB_USER", "biblioteca"),
                System.getenv().getOrDefault("DB_PASSWORD", ""));
    }

    /** Espera o banco aceitar conexões (o contêiner pode demorar) e cria as tabelas se não existirem. */
    public void iniciar() {
        SQLException ultimo = null;
        for (int tentativa = 1; tentativa <= 30; tentativa++) {
            try (Connection c = abrir(); Statement st = c.createStatement()) {
                for (String comando : ESQUEMA) {
                    st.execute(comando);
                }
                System.out.println("Banco de dados conectado: " + url);
                return;
            } catch (SQLException e) {
                ultimo = e;
                System.out.println("Aguardando o banco de dados (" + tentativa + "/30): " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new ErroPersistenciaException("Não foi possível conectar ao banco de dados.", ultimo);
    }

    private Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, usuario, senha);
    }

    /** INSERT que devolve o identificador gerado pelo banco. */
    public int inserir(String sql, Object... params) {
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
            parametros(ps, params);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new ErroPersistenciaException("Falha ao gravar no banco.", e);
        }
    }

    public void executar(String sql, Object... params) {
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            parametros(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ErroPersistenciaException("Falha ao gravar no banco.", e);
        }
    }

    public <T> List<T> consultar(String sql, Mapeador<T> mapeador, Object... params) {
        try (Connection c = abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> itens = new ArrayList<>();
                while (rs.next()) {
                    itens.add(mapeador.mapear(rs));
                }
                return itens;
            }
        } catch (SQLException e) {
            throw new ErroPersistenciaException("Falha ao ler do banco.", e);
        }
    }

    private static void parametros(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static final String[] ESQUEMA = {
        """
        CREATE TABLE IF NOT EXISTS categoria_leitor (
            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            nome VARCHAR(60) NOT NULL UNIQUE,
            prazo_dias INT NOT NULL CHECK (prazo_dias > 0),
            limite_obras INT NOT NULL CHECK (limite_obras > 0),
            valor_multa_diaria NUMERIC(8,2) NOT NULL CHECK (valor_multa_diaria >= 0),
            limite_renovacoes INT NOT NULL CHECK (limite_renovacoes >= 0)
        )""",
        """
        CREATE TABLE IF NOT EXISTS usuario (
            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('LEITOR', 'BIBLIOTECARIO')),
            nome VARCHAR(120) NOT NULL,
            email VARCHAR(120) NOT NULL UNIQUE,
            senha_hash VARCHAR(200) NOT NULL,
            matricula VARCHAR(40) UNIQUE,
            categoria_id INT REFERENCES categoria_leitor(id),
            registro_funcional VARCHAR(40)
        )""",
        """
        CREATE TABLE IF NOT EXISTS livro (
            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            titulo VARCHAR(200) NOT NULL,
            autor VARCHAR(160) NOT NULL,
            editora VARCHAR(120) NOT NULL,
            isbn VARCHAR(30) NOT NULL UNIQUE,
            assunto VARCHAR(80) NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS exemplar (
            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            livro_id INT NOT NULL REFERENCES livro(id),
            codigo_tombo VARCHAR(40) NOT NULL UNIQUE,
            situacao VARCHAR(20) NOT NULL CHECK (situacao IN ('DISPONIVEL','EMPRESTADO','RESERVADO','BAIXADO')),
            data_baixa DATE,
            motivo_baixa VARCHAR(200)
        )""",
        """
        CREATE TABLE IF NOT EXISTS emprestimo (
            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            leitor_id INT NOT NULL REFERENCES usuario(id),
            exemplar_id INT NOT NULL REFERENCES exemplar(id),
            bibliotecario_id INT NOT NULL REFERENCES usuario(id),
            data_emprestimo DATE NOT NULL,
            data_prevista DATE NOT NULL,
            data_devolucao DATE,
            qtd_renovacoes INT NOT NULL DEFAULT 0
        )""",
        // RF07 / RNF07: o próprio banco impede dois empréstimos em aberto do mesmo exemplar.
        "CREATE UNIQUE INDEX IF NOT EXISTS um_emprestimo_aberto_por_exemplar "
            + "ON emprestimo (exemplar_id) WHERE data_devolucao IS NULL",
        """
        CREATE TABLE IF NOT EXISTS multa (
            id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            emprestimo_id INT NOT NULL UNIQUE REFERENCES emprestimo(id),
            valor NUMERIC(8,2) NOT NULL,
            data_geracao DATE NOT NULL,
            paga BOOLEAN NOT NULL DEFAULT FALSE
        )"""
    };
}
