import { api } from "./api.js";
import { $, aviso, esc, mensagemDe } from "./ui.js";

/**
 * Troca de senha. `obrigatoria`: primeiro acesso com senha provisória (o usuário não usa o
 * sistema enquanto não trocar).
 */
/** Campo de senha com botão "olhinho" para mostrar/ocultar o que foi digitado. */
function campoSenha(id: string, rotulo: string, autocomplete: string, minimo = 0): string {
  return `<label>${rotulo}
      <span class="campo-senha">
        <input id="${id}" type="password" required ${minimo ? `minlength="${minimo}"` : ""} autocomplete="${autocomplete}" />
        <button type="button" class="olho" data-olho="${id}" aria-label="Mostrar senha" title="Mostrar senha">${OLHO_ABERTO}</button>
      </span></label>`;
}

const OLHO_ABERTO = `<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>`;
const OLHO_FECHADO = `<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-7 0-11-7-11-7a18.45 18.45 0 0 1 5.06-5.94"/><path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 7 11 7a18.5 18.5 0 0 1-2.16 3.19"/><path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`;

export function mostrarTrocaSenha(raiz: HTMLElement, obrigatoria: boolean, aoConcluir: () => void): void {
  raiz.innerHTML = `
    <section class="cartao estreito">
      <h2>${obrigatoria ? "Defina sua nova senha" : "Trocar senha"}</h2>
      ${obrigatoria ? `<p class="ajuda">Você entrou com uma senha provisória. Para continuar, escolha uma senha pessoal.</p>` : ""}
      <form id="troca-senha" class="coluna">
        ${obrigatoria ? "" : campoSenha("ts-atual", "Senha atual", "current-password")}
        ${campoSenha("ts-nova", "Nova senha (mín. 6 caracteres)", "new-password", 6)}
        ${campoSenha("ts-confirma", "Confirme a nova senha", "new-password", 6)}
        <button type="submit">Salvar nova senha</button>
      </form>
    </section>`;

  raiz.querySelectorAll<HTMLButtonElement>("[data-olho]").forEach((b) =>
    b.addEventListener("click", () => {
      const campo = $<HTMLInputElement>(`#${b.dataset["olho"]}`, raiz);
      const mostrar = campo.type === "password";
      campo.type = mostrar ? "text" : "password";
      b.innerHTML = mostrar ? OLHO_FECHADO : OLHO_ABERTO;
      b.title = b.ariaLabel = mostrar ? "Ocultar senha" : "Mostrar senha";
    }),
  );

  $("#troca-senha", raiz).addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const atual = obrigatoria ? "" : $<HTMLInputElement>("#ts-atual", raiz).value;
    const nova = $<HTMLInputElement>("#ts-nova", raiz).value;
    if (nova !== $<HTMLInputElement>("#ts-confirma", raiz).value) {
      aviso("A confirmação não confere com a nova senha.", "erro");
      return;
    }
    try {
      await api.trocarSenha(atual, nova);
      aviso("Senha alterada.", "ok");
      aoConcluir();
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  });
}

/** Quadro com a senha provisória gerada pelo sistema (mostrada uma única vez). */
export function quadroSenhaProvisoria(nome: string, email: string, senha: string): string {
  return `<div class="senha-gerada">
      <p><strong>Senha provisória de ${esc(nome)}</strong> (${esc(email)}):</p>
      <p><code class="senha">${esc(senha)}</code>
        <button type="button" class="secundario" data-copiar="${esc(senha)}">Copiar</button></p>
      <p class="ajuda">Entregue esta senha ao usuário. Ela não será mostrada novamente e deverá ser trocada no primeiro login.</p>
    </div>`;
}

/** Liga os botões "Copiar" dos quadros de senha dentro de `raiz`. */
export function ligarCopiar(raiz: HTMLElement): void {
  raiz.addEventListener("click", (ev) => {
    const b = (ev.target as HTMLElement).closest<HTMLButtonElement>("[data-copiar]");
    if (b) {
      void navigator.clipboard
        ?.writeText(b.dataset["copiar"] ?? "")
        .then(() => aviso("Senha copiada.", "ok"))
        .catch(() => aviso("Não foi possível copiar; anote a senha.", "erro"));
    }
  });
}
