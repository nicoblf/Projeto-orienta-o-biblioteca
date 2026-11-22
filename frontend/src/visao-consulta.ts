import { api } from "./api.js";
import { criarFiltroAcervo } from "./filtros-acervo.js";
import type { Livro } from "./tipos.js";
import { $, aviso, esc, mensagemDe } from "./ui.js";

/** Consulta pública ao acervo (RF04): não exige login. */
export async function mostrarConsulta(raiz: HTMLElement): Promise<void> {
  raiz.innerHTML = `
    <section class="cartao">
      <h2>Consulta ao acervo</h2>
      <p class="ajuda">Pesquise e filtre por assunto, autor ou disponibilidade para ver quantos exemplares estão livres agora.</p>
      <div id="filtros"></div>
      <div id="resultado"></div>
    </section>`;

  const resultado = $("#resultado", raiz);
  let todos: Livro[] = [];

  function desenhar(): void {
    const livros = filtro.filtrar(todos);
    if (livros.length === 0) {
      resultado.innerHTML = `<p class="vazio">Nenhum título encontrado.</p>`;
      return;
    }
    resultado.innerHTML = `
      <p class="contagem">${livros.length} de ${todos.length} título(s)</p>
      <table>
        <thead><tr><th>Título</th><th>Autor</th><th>Assunto</th><th>ISBN</th><th>Disponibilidade</th></tr></thead>
        <tbody>${livros
          .map(
            (l) => `<tr>
              <td><strong>${esc(l.titulo)}</strong><br><small>${esc(l.editora)}</small></td>
              <td>${esc(l.autor)}</td>
              <td>${esc(l.assunto)}</td>
              <td>${esc(l.isbn)}</td>
              <td><span class="selo ${l.disponiveis > 0 ? "verde" : "vermelho"}">
                ${l.disponiveis} de ${l.total} disponível(is)</span></td>
            </tr>`,
          )
          .join("")}</tbody>
      </table>`;
  }

  const filtro = criarFiltroAcervo($("#filtros", raiz), desenhar);
  try {
    todos = await api.buscarLivros("");
    filtro.popular(todos);
    desenhar();
  } catch (e) {
    aviso(mensagemDe(e), "erro");
  }
}
