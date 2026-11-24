package biblioteca.servico;

import biblioteca.modelo.RegistroLog;
import biblioteca.repositorio.LogRepositorio;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Log de auditoria: registra as ações feitas no sistema para consulta do administrador. */
public class LogServico {

    private final LogRepositorio logs;

    public LogServico(LogRepositorio logs) {
        this.logs = logs;
    }

    public void registrar(String usuario, String perfil, String acao, String detalhe, String resultado,
                          boolean sucesso, String ip) {
        try {
            logs.salvar(new RegistroLog(LocalDateTime.now(), usuario, perfil, acao, detalhe, resultado, sucesso, ip));
        } catch (RuntimeException e) {
            // Falha ao gravar o log não pode derrubar a operação do usuário.
            System.err.println("Falha ao gravar log de auditoria: " + e.getMessage());
        }
    }

    /** Registros mais recentes primeiro. */
    public List<RegistroLog> listarRecentes(int limite) {
        return logs.listar().stream()
                .sorted(Comparator.comparing(RegistroLog::getId).reversed())
                .limit(limite)
                .toList();
    }
}
