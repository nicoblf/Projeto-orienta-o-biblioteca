import type { Categoria, Emprestimo, Exemplar, Leitor, Livro, Painel, Sessao } from "./tipos.js";

/** Erro devolvido pela API (mensagem em português vinda das regras do backend). */
export class ErroApi extends Error {
  constructor(mensagem: string, readonly status: number) {
    super(mensagem);
  }
}

const CHAVE_SESSAO = "biblioteca.sessao";

export function sessaoAtual(): Sessao | null {
  const bruto = sessionStorage.getItem(CHAVE_SESSAO);
  return bruto ? (JSON.parse(bruto) as Sessao) : null;
}

function guardarSessao(s: Sessao | null): void {
  if (s) {
    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(s));
  } else {
    sessionStorage.removeItem(CHAVE_SESSAO);
  }
}

async function chamar<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const cabecalhos: Record<string, string> = {};
  const sessao = sessaoAtual();
  if (sessao) {
    cabecalhos["Authorization"] = `Bearer ${sessao.token}`;
  }
  if (corpo !== undefined) {
    cabecalhos["Content-Type"] = "application/json";
  }
  const resposta = await fetch(caminho, {
    method: metodo,
    headers: cabecalhos,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });
  const dados = (await resposta.json().catch(() => ({}))) as { erro?: string };
  if (!resposta.ok) {
    if (resposta.status === 401 && sessao) {
      guardarSessao(null); // sessão expirou (ex.: servidor reiniciado)
    }
    throw new ErroApi(dados.erro ?? "Erro inesperado.", resposta.status);
  }
  return dados as T;
}

export const api = {
  // sessão
  async login(email: string, senha: string): Promise<Sessao> {
    const s = await chamar<Sessao>("POST", "/api/login", { email, senha });
    guardarSessao(s);
    return s;
  },
  async autocadastrar(d: { nome: string; email: string; matricula: string; senha: string }): Promise<Sessao> {
    const s = await chamar<Sessao>("POST", "/api/cadastro", d);
    guardarSessao(s);
    return s;
  },
  async logout(): Promise<void> {
    try {
      await chamar("POST", "/api/logout");
    } finally {
      guardarSessao(null);
    }
  },

  // acervo (público)
  buscarLivros: (q: string) => chamar<Livro[]>("GET", `/api/livros?q=${encodeURIComponent(q)}`),

  // acervo (bibliotecário)
  cadastrarLivro: (d: Pick<Livro, "titulo" | "autor" | "editora" | "isbn" | "assunto">) =>
    chamar<Livro>("POST", "/api/livros", d),
  listarExemplares: (livroId: number) => chamar<Exemplar[]>("GET", `/api/livros/${livroId}/exemplares`),
  cadastrarExemplar: (livroId: number, codigoTombo: string) =>
    chamar<Exemplar>("POST", `/api/livros/${livroId}/exemplares`, { codigoTombo }),
  baixarExemplar: (exemplarId: number, motivo: string) =>
    chamar<Exemplar>("POST", `/api/exemplares/${exemplarId}/baixa`, { motivo }),

  // leitores
  listarCategorias: () => chamar<Categoria[]>("GET", "/api/categorias"),
  listarLeitores: () => chamar<Leitor[]>("GET", "/api/leitores"),
  cadastrarLeitor: (d: { nome: string; email: string; senha: string; matricula: string; categoriaId: number }) =>
    chamar<Leitor>("POST", "/api/leitores", d),

  // circulação
  listarEmprestimos: (status: "ativos" | "atrasados" | "todos") =>
    chamar<Emprestimo[]>("GET", `/api/emprestimos?status=${status}`),
  emprestar: (matricula: string, codigoTombo: string) =>
    chamar<Emprestimo>("POST", "/api/emprestimos", { matricula, codigoTombo }),
  devolver: (id: number) => chamar<Emprestimo>("POST", `/api/emprestimos/${id}/devolucao`),
  renovar: (id: number) => chamar<Emprestimo>("POST", `/api/emprestimos/${id}/renovacao`),
  meusEmprestimos: () => chamar<Emprestimo[]>("GET", "/api/meus-emprestimos"),
  quitarMulta: (id: number) => chamar<{ mensagem: string }>("POST", `/api/multas/${id}/quitar`),
  painel: () => chamar<Painel>("GET", "/api/painel"),
};
