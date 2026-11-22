package biblioteca.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Aluno ou professor que retira obras. Sabe dizer se possui pendências e se pode fazer um novo empréstimo. */
public class Leitor extends Usuario {

    private final String matricula;
    private final CategoriaLeitor categoria;
    private final List<Emprestimo> emprestimos = new ArrayList<>();

    public Leitor(String nome, String email, String senha, String matricula, CategoriaLeitor categoria) {
        super(nome, email, senha);
        if (matricula == null || matricula.isBlank()) {
            throw new biblioteca.excecao.RegraNegocioException("A matrícula é obrigatória.");
        }
        if (categoria == null) {
            throw new biblioteca.excecao.RegraNegocioException("A categoria do leitor é obrigatória.");
        }
        this.matricula = matricula.trim().toUpperCase();
        this.categoria = categoria;
    }

    private Leitor(int id, String nome, String email, String senhaHash, String matricula, CategoriaLeitor categoria) {
        super(id, nome, email, senhaHash);
        this.matricula = matricula;
        this.categoria = categoria;
    }

    /** Reconstrói um leitor gravado no banco. */
    public static Leitor restaurar(int id, String nome, String email, String senhaHash,
                                   String matricula, CategoriaLeitor categoria) {
        return new Leitor(id, nome, email, senhaHash, matricula, categoria);
    }

    /** Pendência = empréstimo atrasado ou multa em aberto (RF08). */
    public boolean possuiPendencias() {
        for (Emprestimo e : emprestimos) {
            if (e.estaAtrasado()) {
                return true;
            }
            if (e.getMulta() != null && !e.getMulta().isPaga()) {
                return true;
            }
        }
        return false;
    }

    public boolean possuiMultaEmAberto() {
        for (Emprestimo e : emprestimos) {
            if (e.getMulta() != null && !e.getMulta().isPaga()) {
                return true;
            }
        }
        return false;
    }

    public int qtdEmprestimosAtivos() {
        int total = 0;
        for (Emprestimo e : emprestimos) {
            if (e.estaAtivo()) {
                total++;
            }
        }
        return total;
    }

    public boolean podeEmprestar() {
        return !possuiPendencias() && qtdEmprestimosAtivos() < categoria.getLimiteObras();
    }

    public List<Emprestimo> consultarEmprestimos() {
        return Collections.unmodifiableList(emprestimos);
    }

    /** Chamado pelo Bibliotecario ao registrar um empréstimo (relacionamento Leitor 1 — 0..* Emprestimo). */
    void adicionarEmprestimo(Emprestimo emprestimo) {
        emprestimos.add(emprestimo);
    }

    @Override
    public String getPerfil() {
        return "LEITOR";
    }

    public String getMatricula() {
        return matricula;
    }

    public CategoriaLeitor getCategoria() {
        return categoria;
    }
}
