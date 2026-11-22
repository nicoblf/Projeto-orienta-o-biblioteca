// Funções de apoio para montar a interface com segurança (sempre escapar texto vindo do servidor).

export function esc(valor: unknown): string {
  return String(valor ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

/** "2026-11-22" -> "22/11/2026" */
export function data(iso: string | null): string {
  if (!iso) {
    return "—";
  }
  const [a, m, d] = iso.split("-");
  return `${d}/${m}/${a}`;
}

export function dinheiro(valor: number): string {
  return valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

export function $<T extends HTMLElement>(seletor: string, raiz: ParentNode = document): T {
  const el = raiz.querySelector<T>(seletor);
  if (!el) {
    throw new Error(`Elemento não encontrado: ${seletor}`);
  }
  return el;
}

let temporizador: number | undefined;

/** Mensagem rápida no canto da tela. */
export function aviso(texto: string, tipo: "ok" | "erro" = "ok"): void {
  const el = $("#aviso");
  el.textContent = texto;
  el.className = `aviso ${tipo} visivel`;
  window.clearTimeout(temporizador);
  temporizador = window.setTimeout(() => el.classList.remove("visivel"), 4500);
}

export function mensagemDe(e: unknown): string {
  return e instanceof Error ? e.message : "Erro inesperado.";
}

/** Executa uma ação e mostra o resultado (sucesso ou a mensagem de regra do servidor). */
export async function tentar(acao: () => Promise<unknown>, sucesso: string): Promise<boolean> {
  try {
    await acao();
    aviso(sucesso, "ok");
    return true;
  } catch (e) {
    aviso(mensagemDe(e), "erro");
    return false;
  }
}
