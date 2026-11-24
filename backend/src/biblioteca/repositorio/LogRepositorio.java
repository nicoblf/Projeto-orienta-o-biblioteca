package biblioteca.repositorio;

import biblioteca.modelo.RegistroLog;
import java.time.LocalDateTime;

public class LogRepositorio extends RepositorioMemoria<RegistroLog> {

    private final Banco banco;

    public LogRepositorio() {
        this(null);
    }

    public LogRepositorio(Banco banco) {
        this.banco = banco;
    }

    @Override
    protected int inserir(RegistroLog r) {
        if (banco == null) {
            return super.inserir(r);
        }
        return banco.inserir("INSERT INTO log_auditoria (data_hora, usuario, perfil, acao, detalhe, resultado, sucesso, ip) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", r.getDataHora(), r.getUsuario(), r.getPerfil(), r.getAcao(),
                r.getDetalhe(), r.getResultado(), r.isSucesso(), r.getIp());
    }

    public void carregar() {
        banco.consultar("SELECT * FROM log_auditoria ORDER BY id", rs -> {
            RegistroLog r = new RegistroLog(rs.getObject("data_hora", LocalDateTime.class), rs.getString("usuario"),
                    rs.getString("perfil"), rs.getString("acao"), rs.getString("detalhe"),
                    rs.getString("resultado"), rs.getBoolean("sucesso"), rs.getString("ip"));
            r.setId(rs.getInt("id"));
            adicionarCarregado(r);
            return r;
        });
    }
}
