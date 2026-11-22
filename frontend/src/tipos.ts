// Tipos que espelham as classes do modelo Java (Fase 2) como chegam da API.

export type Perfil = "LEITOR" | "BIBLIOTECARIO";

export interface Sessao {
  token: string;
  nome: string;
  perfil: Perfil;
}

export interface Livro {
  id: number;
  titulo: string;
  autor: string;
  editora: string;
  isbn: string;
  assunto: string;
  disponiveis: number;
  total: number;
}

export type SituacaoExemplar = "DISPONIVEL" | "EMPRESTADO" | "RESERVADO" | "BAIXADO";

export interface Exemplar {
  id: number;
  codigoTombo: string;
  livroId: number;
  titulo: string;
  situacao: SituacaoExemplar;
  dataBaixa: string | null;
  motivoBaixa: string | null;
}

export interface Categoria {
  id: number;
  nome: string;
  prazoDias: number;
  limiteObras: number;
  valorMultaDiaria: number;
  limiteRenovacoes: number;
}

export interface Leitor {
  id: number;
  nome: string;
  email: string;
  matricula: string;
  telefone: string | null;
  categoria: string;
  emprestimosAtivos: number;
  limiteObras: number;
  possuiPendencias: boolean;
  podeEmprestar: boolean;
}

export interface Multa {
  id: number;
  valor: number;
  dataGeracao: string;
  paga: boolean;
}

export interface Emprestimo {
  id: number;
  leitor: string;
  matricula: string;
  titulo: string;
  codigoTombo: string;
  bibliotecario: string;
  dataEmprestimo: string;
  dataPrevista: string;
  dataDevolucao: string | null;
  qtdRenovacoes: number;
  ativo: boolean;
  atrasado: boolean;
  diasAtraso: number;
  multaEstimada: number;
  telefone: string | null;
  podeRenovar: boolean;
  multa: Multa | null;
}

export interface Painel {
  emprestimosAtivos: number;
  emprestimosAtrasados: number;
  devolucoesHoje: number;
  multasEmAberto: number;
  valorMultasEmAberto: number;
  titulos: number;
  exemplares: number;
  exemplaresDisponiveis: number;
  exemplaresBaixados: number;
}
