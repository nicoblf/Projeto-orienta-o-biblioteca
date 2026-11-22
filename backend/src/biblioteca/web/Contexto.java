package biblioteca.web;

import biblioteca.excecao.RegraNegocioException;
import biblioteca.modelo.Bibliotecario;
import biblioteca.modelo.Leitor;
import biblioteca.modelo.Usuario;
import java.util.Map;

/** Dados de uma requisição já interpretados: parâmetros de rota/consulta, corpo JSON e usuário logado. */
public class Contexto {

    private final Map<String, String> rota;
    private final Map<String, String> consulta;
    private final Map<String, Object> corpo;
    private final Usuario usuario;
    private final String token;

    public Contexto(Map<String, String> rota, Map<String, String> consulta, Map<String, Object> corpo,
                    Usuario usuario, String token) {
        this.rota = rota;
        this.consulta = consulta;
        this.corpo = corpo;
        this.usuario = usuario;
        this.token = token;
    }

    public String parametroRota(String nome) {
        return rota.get(nome);
    }

    public int idRota(String nome) {
        try {
            return Integer.parseInt(rota.get(nome));
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Identificador inválido.");
        }
    }

    public String consulta(String nome) {
        return consulta.get(nome);
    }

    /** Campo de texto do corpo JSON (null se ausente). */
    public String texto(String campo) {
        Object v = corpo.get(campo);
        return v == null ? null : v.toString();
    }

    public int inteiro(String campo) {
        Object v = corpo.get(campo);
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("O campo " + campo + " deve ser numérico.");
        }
    }

    public Usuario usuario() {
        return usuario;
    }

    public Leitor leitor() {
        return (Leitor) usuario;
    }

    public Bibliotecario bibliotecario() {
        return (Bibliotecario) usuario;
    }

    public String token() {
        return token;
    }
}
