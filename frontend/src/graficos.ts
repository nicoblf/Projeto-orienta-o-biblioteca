import { esc } from "./ui.js";

// Gráficos simples em HTML/SVG, sem biblioteca externa.

export interface Fatia {
  rotulo: string;
  valor: number;
  cor: string;
}

const CORES = { azul: "#1f3a5f", verde: "#1e7a4a", vermelho: "#b3261e", cinza: "#8d98a6", amarelo: "#c58a00" };
export { CORES };

/** Rosca (donut) com legenda. */
export function rosca(fatias: Fatia[]): string {
  const total = fatias.reduce((s, f) => s + f.valor, 0);
  const r = 15.915; // circunferência = 100
  let acumulado = 0;
  const arcos = total === 0
    ? ""
    : fatias
        .filter((f) => f.valor > 0)
        .map((f) => {
          const pct = (f.valor / total) * 100;
          const arco = `<circle r="${r}" cx="21" cy="21" fill="none" stroke="${f.cor}" stroke-width="7"
            stroke-dasharray="${pct} ${100 - pct}" stroke-dashoffset="${25 - acumulado}"><title>${esc(f.rotulo)}: ${f.valor}</title></circle>`;
          acumulado += pct;
          return arco;
        })
        .join("");
  const legenda = fatias
    .map((f) => `<div><i style="background:${f.cor}"></i>${esc(f.rotulo)}: <strong>${f.valor}</strong></div>`)
    .join("");
  return `<div class="rosca-bloco">
    <svg viewBox="0 0 42 42" width="150" height="150" role="img" aria-label="Gráfico em rosca">
      <circle r="${r}" cx="21" cy="21" fill="none" stroke="#e8eef6" stroke-width="7"/>${arcos}
      <text x="21" y="22.5" text-anchor="middle" font-size="7" font-weight="700" fill="#1f3a5f">${total}</text>
    </svg>
    <div class="legenda">${legenda}</div></div>`;
}

/** Barras horizontais; cada linha pode ter segmentos (ex.: disponíveis + indisponíveis). */
export interface LinhaBarra {
  rotulo: string;
  segmentos: { valor: number; classe: string; dica: string }[];
}

export function barras(linhas: LinhaBarra[]): string {
  const max = Math.max(1, ...linhas.map((l) => l.segmentos.reduce((s, x) => s + x.valor, 0)));
  if (linhas.length === 0) {
    return `<p class="vazio">Sem dados.</p>`;
  }
  return linhas
    .map((l) => {
      const soma = l.segmentos.reduce((s, x) => s + x.valor, 0);
      const trechos = l.segmentos
        .map((x) => `<div class="preenchido ${x.classe}" style="width:${(x.valor / max) * 100}%" title="${esc(x.dica)}: ${x.valor}"></div>`)
        .join("");
      return `<div class="barra"><span title="${esc(l.rotulo)}">${esc(l.rotulo)}</span><div class="trilho">${trechos}</div><span class="valor">${soma}</span></div>`;
    })
    .join("");
}

/** Colunas verticais (ex.: empréstimos por dia). */
export function colunas(itens: { rotulo: string; valor: number }[]): string {
  const max = Math.max(1, ...itens.map((i) => i.valor));
  return `<div class="colunas">${itens
    .map(
      (i) => `<div class="coluna-g"><strong>${i.valor}</strong><div class="pilar" style="height:${(i.valor / max) * 100}px" title="${esc(i.rotulo)}: ${i.valor}"></div><span>${esc(i.rotulo)}</span></div>`,
    )
    .join("")}</div>`;
}
