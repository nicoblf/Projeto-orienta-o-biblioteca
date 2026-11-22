package biblioteca.web;

import biblioteca.modelo.CategoriaLeitor;
import biblioteca.modelo.Emprestimo;
import biblioteca.modelo.Exemplar;
import biblioteca.modelo.Leitor;
import biblioteca.modelo.Livro;
import biblioteca.modelo.Multa;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converte objetos do modelo em mapas para JSON. Define o que cada perfil enxerga: a consulta pública
 * mostra só dados do acervo, nunca dados pessoais de leitores (LGPD).
 */
final class Apresentacao {

    private Apresentacao() {
    }

    static Map<String, Object> livro(Livro l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("titulo", l.getTitulo());
        m.put("autor", l.getAutor());
        m.put("editora", l.getEditora());
        m.put("isbn", l.getIsbn());
        m.put("assunto", l.getAssunto());
        m.put("disponiveis", l.qtdDisponiveis());
        m.put("total", l.qtdTotal());
        return m;
    }

    static Map<String, Object> exemplar(Exemplar e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("codigoTombo", e.getCodigoTombo());
        m.put("livroId", e.getLivro().getId());
        m.put("titulo", e.getLivro().getTitulo());
        m.put("situacao", e.getSituacao());
        m.put("dataBaixa", e.getDataBaixa());
        m.put("motivoBaixa", e.getMotivoBaixa());
        return m;
    }

    static Map<String, Object> categoria(CategoriaLeitor c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("nome", c.getNome());
        m.put("prazoDias", c.getPrazoDias());
        m.put("limiteObras", c.getLimiteObras());
        m.put("valorMultaDiaria", c.getValorMultaDiaria());
        m.put("limiteRenovacoes", c.getLimiteRenovacoes());
        return m;
    }

    static Map<String, Object> leitor(Leitor l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("nome", l.getNome());
        m.put("email", l.getEmail());
        m.put("matricula", l.getMatricula());
        m.put("telefone", l.getTelefone());
        m.put("categoria", l.getCategoria().getNome());
        m.put("emprestimosAtivos", l.qtdEmprestimosAtivos());
        m.put("limiteObras", l.getCategoria().getLimiteObras());
        m.put("possuiPendencias", l.possuiPendencias());
        m.put("podeEmprestar", l.podeEmprestar());
        return m;
    }

    static Map<String, Object> multa(Multa x) {
        if (x == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", x.getId());
        m.put("valor", x.getValor());
        m.put("dataGeracao", x.getDataGeracao());
        m.put("paga", x.isPaga());
        return m;
    }

    static Map<String, Object> emprestimo(Emprestimo e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("leitor", e.getLeitor().getNome());
        m.put("matricula", e.getLeitor().getMatricula());
        m.put("titulo", e.getExemplar().getLivro().getTitulo());
        m.put("codigoTombo", e.getExemplar().getCodigoTombo());
        m.put("bibliotecario", e.getBibliotecario().getNome());
        m.put("dataEmprestimo", e.getDataEmprestimo());
        m.put("dataPrevista", e.getDataPrevista());
        m.put("dataDevolucao", e.getDataDevolucao());
        m.put("qtdRenovacoes", e.getQtdRenovacoes());
        m.put("ativo", e.estaAtivo());
        m.put("atrasado", e.estaAtrasado());
        m.put("diasAtraso", e.diasAtraso());
        m.put("multaEstimada", e.multaEstimada());
        m.put("telefone", e.getLeitor().getTelefone());
        m.put("podeRenovar", e.motivoImpedimentoRenovacao() == null);
        m.put("multa", multa(e.getMulta()));
        return m;
    }
}
