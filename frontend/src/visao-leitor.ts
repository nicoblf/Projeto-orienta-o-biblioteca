import { api } from "./api.js";
import { $, aviso, data, dinheiro, esc, mensagemDe, tentar } from "./ui.js";

/** Área do leitor: seus empréstimos, prazos, multas e renovação (RF04, RF06). */
export async function mostrarMeusEmprestimos(raiz: HTMLElement): Promise<void> {
  raiz.innerHTML = `<section class="cartao"><h2>Meus empréstimos</h2><div id="lista"></div></section>`;
  const lista = $("#lista", raiz);

  async function carregar(): Promise<void> {
    try {
      const itens = await api.meusEmprestimos();
      if (itens.length === 0) {
        lista.innerHTML = `<p class="vazio">Você ainda não tem empréstimos.</p>`;
        return;
      }
      lista.innerHTML = `
        <table>
          <thead><tr><th>Título</th><th>Retirada</th><th>Devolver até</th><th>Situação</th><th>Multa</th><th></th></tr></thead>
          <tbody>${itens
            .map((e) => {
              const situacao = !e.ativo
                ? `<span class="selo cinza">Devolvido em ${data(e.dataDevolucao)}</span>`
                : e.atrasado
                  ? `<span class="selo vermelho">Atrasado (${e.diasAtraso} dia(s))</span>`
                  : `<span class="selo verde">Em dia</span>`;
              const multa = e.multa
                ? `${dinheiro(e.multa.valor)} <span class="selo ${e.multa.paga ? "verde" : "vermelho"}">${e.multa.paga ? "paga" : "em aberto"}</span>`
                : "—";
              const acao = e.ativo
                ? `<button data-renovar="${e.id}" ${e.podeRenovar ? "" : "disabled"} class="secundario">Renovar</button>`
                : "";
              return `<tr>
                <td><strong>${esc(e.titulo)}</strong><br><small>Tombo ${esc(e.codigoTombo)}</small></td>
                <td>${data(e.dataEmprestimo)}</td>
                <td>${data(e.dataPrevista)}${e.qtdRenovacoes ? `<br><small>${e.qtdRenovacoes} renovação(ões)</small>` : ""}</td>
                <td>${situacao}</td><td>${multa}</td><td>${acao}</td></tr>`;
            })
            .join("")}</tbody>
        </table>`;
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  }

  lista.addEventListener("click", async (ev) => {
    const alvo = (ev.target as HTMLElement).closest<HTMLButtonElement>("[data-renovar]");
    if (alvo && (await tentar(() => api.renovar(Number(alvo.dataset["renovar"])), "Empréstimo renovado."))) {
      await carregar();
    }
  });
  await carregar();
}
