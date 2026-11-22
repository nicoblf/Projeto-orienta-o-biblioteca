import type { Livro } from "./tipos.js";
import { $, esc } from "./ui.js";

/**
 * Filtros do acervo (texto, assunto, autor e disponibilidade), usados na consulta pública e na tela do bibliotecário.
 * Os filtros trabalham sobre a lista já carregada; as opções de assunto/autor vêm dos próprios livros.
 */
export interface FiltroAcervo {
  /** Atualiza as opções de assunto/autor a partir dos livros carregados (mantém a seleção atual). */
  popular(livros: Livro[]): void;
  filtrar(livros: Livro[]): Livro[];
}

function normalizar(t: string): string {
  return t.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
}

function opcoes(valores: string[], atual: string): string {
  const unicos = [...new Set(valores)].sort((a, b) => a.localeCompare(b, "pt-BR"));
  return [`<option value="">Todos</option>`]
    .concat(unicos.map((v) => `<option value="${esc(v)}" ${v === atual ? "selected" : ""}>${esc(v)}</option>`))
    .join("");
}

export function criarFiltroAcervo(destino: HTMLElement, aoMudar: () => void): FiltroAcervo {
  destino.innerHTML = `
    <div class="filtros">
      <label>Busca
        <input id="f-texto" type="search" placeholder="Título, autor, assunto ou ISBN" />
      </label>
      <label>Assunto
        <select id="f-assunto"><option value="">Todos</option></select>
      </label>
      <label>Autor
        <select id="f-autor"><option value="">Todos</option></select>
      </label>
      <label>Disponibilidade
        <select id="f-disp">
          <option value="">Todas</option>
          <option value="sim">Disponíveis agora</option>
          <option value="nao">Indisponíveis</option>
        </select>
      </label>
    </div>`;

  const texto = $<HTMLInputElement>("#f-texto", destino);
  const assunto = $<HTMLSelectElement>("#f-assunto", destino);
  const autor = $<HTMLSelectElement>("#f-autor", destino);
  const disp = $<HTMLSelectElement>("#f-disp", destino);
  [texto, assunto, autor, disp].forEach((el) => el.addEventListener("input", aoMudar));

  return {
    popular(livros) {
      assunto.innerHTML = opcoes(livros.map((l) => l.assunto), assunto.value);
      autor.innerHTML = opcoes(livros.map((l) => l.autor), autor.value);
    },
    filtrar(livros) {
      const t = normalizar(texto.value);
      return livros.filter(
        (l) =>
          (!t || normalizar(`${l.titulo} ${l.autor} ${l.assunto} ${l.isbn} ${l.editora}`).includes(t)) &&
          (!assunto.value || l.assunto === assunto.value) &&
          (!autor.value || l.autor === autor.value) &&
          (!disp.value || (disp.value === "sim" ? l.disponiveis > 0 : l.disponiveis === 0)),
      );
    },
  };
}
