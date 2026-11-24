package biblioteca;

import biblioteca.repositorio.Banco;
import biblioteca.repositorio.CategoriaRepositorio;
import biblioteca.repositorio.EmprestimoRepositorio;
import biblioteca.repositorio.ExemplarRepositorio;
import biblioteca.repositorio.LivroRepositorio;
import biblioteca.repositorio.LogRepositorio;
import biblioteca.repositorio.MultaRepositorio;
import biblioteca.repositorio.UsuarioRepositorio;
import biblioteca.servico.AcervoServico;
import biblioteca.servico.AdministracaoServico;
import biblioteca.servico.AutenticacaoServico;
import biblioteca.servico.CirculacaoServico;
import biblioteca.servico.LeitorServico;
import biblioteca.servico.LogServico;
import biblioteca.web.AcervoControlador;
import biblioteca.web.AdministracaoControlador;
import biblioteca.web.AutenticacaoControlador;
import biblioteca.web.CirculacaoControlador;
import biblioteca.web.LeitorControlador;
import biblioteca.web.Roteador;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/** Monta as camadas (repositórios -> serviços -> controladores) e liga o servidor HTTP. */
public class Aplicacao {

    private final Banco banco;

    public final UsuarioRepositorio usuarios;
    public final CategoriaRepositorio categorias;
    public final LivroRepositorio livros;
    public final ExemplarRepositorio exemplares;
    public final EmprestimoRepositorio emprestimos;
    public final MultaRepositorio multas;
    public final LogRepositorio logs;

    public final AutenticacaoServico autenticacao;
    public final AcervoServico acervo;
    public final LeitorServico leitores;
    public final CirculacaoServico circulacao;
    public final AdministracaoServico administracao;
    public final LogServico log;

    /** Somente memória (testes e demonstração em console). */
    public Aplicacao() {
        this(null);
    }

    /** Com banco: cada alteração é gravada no PostgreSQL. Com banco null: somente memória. */
    public Aplicacao(Banco banco) {
        this.banco = banco;
        usuarios = new UsuarioRepositorio(banco);
        categorias = new CategoriaRepositorio(banco);
        livros = new LivroRepositorio(banco);
        exemplares = new ExemplarRepositorio(banco);
        emprestimos = new EmprestimoRepositorio(banco);
        multas = new MultaRepositorio(banco);
        logs = new LogRepositorio(banco);

        autenticacao = new AutenticacaoServico(usuarios);
        acervo = new AcervoServico(livros, exemplares, emprestimos);
        leitores = new LeitorServico(usuarios, categorias);
        circulacao = new CirculacaoServico(emprestimos, multas, acervo, leitores);
        administracao = new AdministracaoServico(usuarios, emprestimos, autenticacao);
        log = new LogServico(logs);
    }

    /** Cria as tabelas (se preciso) e recarrega os dados gravados, respeitando as dependências entre eles. */
    public void carregarDoBanco() {
        if (banco == null) {
            return;
        }
        banco.iniciar();
        categorias.carregar();
        usuarios.carregar(categorias);
        livros.carregar();
        exemplares.carregar(livros);
        emprestimos.carregar(usuarios, exemplares);
        multas.carregar(emprestimos);
        logs.carregar();
    }

    public HttpServer iniciarServidor(int porta, Path diretorioWeb) throws IOException {
        Roteador roteador = new Roteador(autenticacao, log, diretorioWeb);
        new AdministracaoControlador(administracao, log).registrar(roteador);
        new AutenticacaoControlador(autenticacao, leitores).registrar(roteador);
        new AcervoControlador(acervo).registrar(roteador);
        new LeitorControlador(leitores).registrar(roteador);
        new CirculacaoControlador(circulacao).registrar(roteador);

        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext("/", roteador);
        servidor.setExecutor(Executors.newFixedThreadPool(8));
        servidor.start();
        return servidor;
    }
}
