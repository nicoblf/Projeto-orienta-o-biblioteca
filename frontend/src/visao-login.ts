import { api } from "./api.js";
import type { Sessao } from "./tipos.js";
import { $, aviso, mensagemDe } from "./ui.js";

export function mostrarLogin(raiz: HTMLElement, aoEntrar: (s: Sessao) => void, aoVoltar: () => void): void {
  raiz.innerHTML = `
    <section class="cartao estreito">
      <div class="abas">
        <button type="button" id="aba-entrar" class="aba ativa">Entrar</button>
        <button type="button" id="aba-cadastro" class="aba">Criar conta de leitor</button>
      </div>

      <form id="login" class="coluna">
        <label>E-mail <input id="email" type="email" required autocomplete="username" /></label>
        <label>Senha <input id="senha" type="password" required autocomplete="current-password" /></label>
        <button type="submit">Entrar</button>
        <button type="button" id="voltar" class="secundario">Voltar à consulta do acervo</button>
        <details class="ajuda">
          <summary>Contas de demonstração</summary>
          <p>Bibliotecário: <code>bibliotecario@biblioteca.com</code> / <code>admin123</code></p>
          <p>Leitor: <code>ana@aluno.com</code> / <code>leitor123</code></p>
          <p>Administrador: <code>admin@biblioteca.com</code> / <code>admin123</code></p>
        </details>
      </form>

      <form id="cadastro" class="coluna" hidden>
        <p class="ajuda">Cadastre-se como leitor (prazo de 7 dias, até 3 obras). Seu RA é a matrícula usada no balcão.</p>
        <label>Nome completo <input id="c-nome" required autocomplete="name" /></label>
        <label>RA (matrícula) <input id="c-ra" required placeholder="Ex.: RA2024010" /></label>
        <label>Telefone (com DDD) <input id="c-tel" type="tel" required placeholder="(11) 91234-5678" autocomplete="tel" /></label>
        <label>E-mail <input id="c-email" type="email" required autocomplete="email" /></label>
        <label>Senha (mín. 6 caracteres) <input id="c-senha" type="password" minlength="6" required autocomplete="new-password" /></label>
        <button type="submit">Criar conta e entrar</button>
        <button type="button" id="voltar2" class="secundario">Voltar à consulta do acervo</button>
      </form>
    </section>`;

  const formLogin = $<HTMLFormElement>("#login", raiz);
  const formCadastro = $<HTMLFormElement>("#cadastro", raiz);
  const abaEntrar = $("#aba-entrar", raiz);
  const abaCadastro = $("#aba-cadastro", raiz);

  function alternar(cadastro: boolean): void {
    formLogin.hidden = cadastro;
    formCadastro.hidden = !cadastro;
    abaEntrar.classList.toggle("ativa", !cadastro);
    abaCadastro.classList.toggle("ativa", cadastro);
  }
  abaEntrar.addEventListener("click", () => alternar(false));
  abaCadastro.addEventListener("click", () => alternar(true));
  $("#voltar", raiz).addEventListener("click", aoVoltar);
  $("#voltar2", raiz).addEventListener("click", aoVoltar);

  formLogin.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    try {
      aoEntrar(await api.login($<HTMLInputElement>("#email", raiz).value, $<HTMLInputElement>("#senha", raiz).value));
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  });

  formCadastro.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const v = (id: string): string => $<HTMLInputElement>(id, raiz).value;
    try {
      const sessao = await api.autocadastrar({
        nome: v("#c-nome"),
        matricula: v("#c-ra"),
        email: v("#c-email"),
        telefone: v("#c-tel"),
        senha: v("#c-senha"),
      });
      aviso("Cadastro realizado. Bem-vindo(a)!", "ok");
      aoEntrar(sessao);
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  });
}
