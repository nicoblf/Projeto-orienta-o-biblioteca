package biblioteca.modelo;

import biblioteca.excecao.RegraNegocioException;

/**
 * Regras que variam por tipo de leitor (aluno, professor...): prazo, limite de obras,
 * multa diária e renovações. Alterar a regra da biblioteca não exige mudar o código.
 */
public class CategoriaLeitor implements Identificavel {

    private int id;
    private final String nome;
    private final int prazoDias;
    private final int limiteObras;
    private final double valorMultaDiaria;
    private final int limiteRenovacoes;

    public CategoriaLeitor(String nome, int prazoDias, int limiteObras, double valorMultaDiaria, int limiteRenovacoes) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome da categoria é obrigatório.");
        }
        if (prazoDias <= 0 || limiteObras <= 0 || valorMultaDiaria < 0 || limiteRenovacoes < 0) {
            throw new RegraNegocioException("Valores inválidos para a categoria " + nome + ".");
        }
        this.nome = nome.trim();
        this.prazoDias = prazoDias;
        this.limiteObras = limiteObras;
        this.valorMultaDiaria = valorMultaDiaria;
        this.limiteRenovacoes = limiteRenovacoes;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public int getLimiteObras() {
        return limiteObras;
    }

    public double getValorMultaDiaria() {
        return valorMultaDiaria;
    }

    public int getLimiteRenovacoes() {
        return limiteRenovacoes;
    }
}
