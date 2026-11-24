package biblioteca.modelo;

import java.time.LocalDateTime;

/** Uma linha do log de auditoria: quem fez o quê, quando, de onde e com qual resultado. */
public class RegistroLog implements Identificavel {

    private int id;
    private final LocalDateTime dataHora;
    private final String usuario;
    private final String perfil;
    private final String acao;
    private final String detalhe;
    private final String resultado;
    private final boolean sucesso;
    private final String ip;

    public RegistroLog(LocalDateTime dataHora, String usuario, String perfil, String acao, String detalhe,
                       String resultado, boolean sucesso, String ip) {
        this.dataHora = dataHora;
        this.usuario = corte(usuario, 160);
        this.perfil = corte(perfil, 20);
        this.acao = corte(acao, 200);
        this.detalhe = corte(detalhe, 400);
        this.resultado = corte(resultado, 400);
        this.sucesso = sucesso;
        this.ip = corte(ip, 60);
    }

    private static String corte(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getPerfil() {
        return perfil;
    }

    public String getAcao() {
        return acao;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public String getResultado() {
        return resultado;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public String getIp() {
        return ip;
    }
}
