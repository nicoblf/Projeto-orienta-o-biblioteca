import type { Emprestimo } from "./tipos.js";
import { dinheiro, esc } from "./ui.js";

/** "11912345678" -> "(11) 91234-5678" */
export function telefoneBR(t: string | null): string {
  if (!t) {
    return "—";
  }
  const d = t.replace(/\D/g, "");
  return d.length === 11
    ? `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`
    : d.length === 10
      ? `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`
      : t;
}

/** Multa de um empréstimo: a gerada (devolvido) ou a estimada (ainda atrasado). */
export function celulaMulta(e: Emprestimo): string {
  if (e.multa) {
    return `${dinheiro(e.multa.valor)} <span class="selo ${e.multa.paga ? "verde" : "vermelho"}">${e.multa.paga ? "paga" : "em aberto"}</span>`;
  }
  if (e.ativo && e.atrasado) {
    return `<strong style="color:var(--vermelho)">${dinheiro(e.multaEstimada)}</strong><br><small>estimada — cresce a cada dia</small>`;
  }
  return "—";
}

/**
 * Faixa de aviso sobre atrasos e multas. `visaoLeitor` = texto para o próprio leitor ("você");
 * caso contrário, texto para a equipe do acervo.
 */
export function faixaAvisos(itens: Emprestimo[], visaoLeitor: boolean): string {
  const atrasados = itens.filter((e) => e.ativo && e.atrasado);
  const multasAbertas = itens.filter((e) => e.multa && !e.multa.paga);
  const estimada = atrasados.reduce((s, e) => s + e.multaEstimada, 0);
  const emAberto = multasAbertas.reduce((s, e) => s + (e.multa?.valor ?? 0), 0);
  const partes: string[] = [];

  if (atrasados.length > 0) {
    partes.push(
      visaoLeitor
        ? `<strong>Você tem ${atrasados.length} empréstimo(s) em atraso.</strong> Devolva o quanto antes: a multa estimada hoje é de
           <strong>${dinheiro(estimada)}</strong> e aumenta a cada dia. Enquanto houver atraso, você não pode pegar novos livros nem renovar.<ul>${atrasados
             .map((e) => `<li>${esc(e.titulo)} — ${e.diasAtraso} dia(s) de atraso (${dinheiro(e.multaEstimada)})</li>`)
             .join("")}</ul>`
        : `<strong>${atrasados.length} empréstimo(s) em atraso.</strong> Multa estimada somada: <strong>${dinheiro(estimada)}</strong>
           (gerada só na devolução). Entre em contato com os leitores:<ul>${atrasados
             .map((e) => `<li>${esc(e.leitor)} (${esc(e.matricula)}) — tel. ${esc(telefoneBR(e.telefone))} — ${esc(e.titulo)}, ${e.diasAtraso} dia(s)</li>`)
             .join("")}</ul>`,
    );
  }
  if (multasAbertas.length > 0) {
    partes.push(
      visaoLeitor
        ? `<strong>Você tem ${multasAbertas.length} multa(s) em aberto: ${dinheiro(emAberto)}.</strong> Procure o balcão da biblioteca para pagar; até lá, novos empréstimos ficam bloqueados.`
        : `<strong>${multasAbertas.length} multa(s) em aberto: ${dinheiro(emAberto)}.</strong> Registre o pagamento em “Leitores e multas”.`,
    );
  }
  return partes.length === 0 ? "" : `<div class="alerta-faixa" role="alert">${partes.map((p) => `<div>${p}</div>`).join("")}</div>`;
}
