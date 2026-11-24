import { api } from "./api.js";
import type { Perfil, RegistroLog, UsuarioAdmin } from "./tipos.js";
import { $, aviso, esc, mensagemDe, tentar } from "./ui.js";
import { ligarCopiar, quadroSenhaProvisoria } from "./visao-senha.js";

const NOME_PERFIL: Record<Perfil, string> = { LEITOR: "Leitor", BIBLIOTECARIO: "Bibliotecário", ADMIN: "Administrador" };

function normalizar(t: string): string {
  return t.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
}

/** Pede confirmação e exclui (ou desativa) um leitor/funcionário. Retorna true se algo mudou. */
export async function confirmarExclusaoUsuario(id: number, nome: string): Promise<boolean> {
  if (!window.confirm(`Excluir ${nome}? A pessoa perde o acesso imediatamente. Se já tiver histórico de empréstimos, o cadastro é mantido como "Desativado" para preservar o histórico.`)) {
    return false;
  }
  try {
    const r = await api.excluirUsuario(id);
    aviso(r.mensagem, "ok");
    return true;
  } catch (e) {
    aviso(mensagemDe(e), "erro");
    return false;
  }
}

// ---------- Usuários: funcionários e redefinição de senha ----------

export async function mostrarUsuariosAdmin(raiz: HTMLElement): Promise<void> {
  raiz.innerHTML = `
    <section class="cartao">
      <h2>Adicionar funcionário (bibliotecário)</h2>
      <p class="ajuda">O sistema gera uma senha provisória, que o funcionário troca no primeiro login.</p>
      <form id="novo-func" class="grade-form" autocomplete="off">
        <input id="f-nome" name="f-nome" placeholder="Nome completo" required autocomplete="off" />
        <input id="f-email" name="f-email" type="email" placeholder="E-mail" required autocomplete="off" />
        <input id="f-reg" name="f-reg" placeholder="Registro funcional (ex.: REG-002)" required autocomplete="off" />
        <button type="submit">Adicionar funcionário</button>
      </form>
      <div id="senha-func"></div>
    </section>
    <section class="cartao">
      <h2>Usuários e senhas</h2>
      <div class="filtros">
        <label>Busca <input id="u-busca" type="search" placeholder="Nome, e-mail, RA ou registro" autocomplete="off" /></label>
        <label>Perfil
          <select id="u-perfil">
            <option value="">Todos</option>
            <option value="LEITOR">Leitores</option>
            <option value="BIBLIOTECARIO">Bibliotecários</option>
            <option value="ADMIN">Administradores</option>
          </select></label>
      </div>
      <div id="senha-reset"></div>
      <div id="usuarios"></div>
    </section>`;

  ligarCopiar(raiz);
  let todos: UsuarioAdmin[] = [];

  function desenhar(): void {
    const termo = normalizar($<HTMLInputElement>("#u-busca", raiz).value);
    const perfil = $<HTMLSelectElement>("#u-perfil", raiz).value;
    const lista = todos.filter(
      (u) =>
        (!perfil || u.perfil === perfil) &&
        (!termo || normalizar(`${u.nome} ${u.email} ${u.identificacao ?? ""}`).includes(termo)),
    );
    $("#usuarios", raiz).innerHTML =
      lista.length === 0
        ? `<p class="vazio">Nenhum usuário encontrado.</p>`
        : `<p class="contagem">${lista.length} de ${todos.length} usuário(s)</p>
        <table><thead><tr><th>Nome</th><th>Perfil</th><th>RA / registro</th><th>Senha</th><th></th></tr></thead>
        <tbody>${lista
          .map(
            (u) => `<tr><td>${esc(u.nome)}<br><small>${esc(u.email)}</small></td>
            <td>${esc(NOME_PERFIL[u.perfil] ?? u.perfil)}</td>
            <td>${esc(u.identificacao ?? "—")}</td>
            <td>${
              !u.ativo
                ? `<span class="selo cinza">Desativado</span>`
                : `<span class="selo ${u.senhaProvisoria ? "amarelo" : "verde"}">${u.senhaProvisoria ? "Provisória" : "Definida"}</span>`
            }</td>
            <td class="acoes">${
              !u.ativo
                ? ""
                : `<button class="secundario" data-reset="${u.id}" data-nome="${esc(u.nome)}">Redefinir senha</button>` +
                  (u.perfil !== "ADMIN"
                    ? `<button class="perigo" data-excluir="${u.id}" data-nome="${esc(u.nome)}">Excluir</button>`
                    : "")
            }</td></tr>`,
          )
          .join("")}</tbody></table>`;
  }

  async function carregar(): Promise<void> {
    try {
      todos = await api.listarUsuarios();
      desenhar();
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  }

  $("#u-busca", raiz).addEventListener("input", desenhar);
  $("#u-perfil", raiz).addEventListener("change", desenhar);

  $("#novo-func", raiz).addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const v = (id: string): string => $<HTMLInputElement>(id, raiz).value;
    try {
      const r = await api.cadastrarFuncionario({ nome: v("#f-nome"), email: v("#f-email"), registroFuncional: v("#f-reg") });
      aviso("Funcionário cadastrado.", "ok");
      $("#senha-func", raiz).innerHTML = quadroSenhaProvisoria(r.usuario.nome, r.usuario.email, r.senhaProvisoria);
      ($("#novo-func", raiz) as HTMLFormElement).reset();
      await carregar();
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  });

  $("#usuarios", raiz).addEventListener("click", async (ev) => {
    const ex = (ev.target as HTMLElement).closest<HTMLButtonElement>("[data-excluir]");
    if (ex) {
      if (await confirmarExclusaoUsuario(Number(ex.dataset["excluir"]), ex.dataset["nome"] ?? "")) {
        await carregar();
      }
      return;
    }
    const b = (ev.target as HTMLElement).closest<HTMLButtonElement>("[data-reset]");
    if (!b) {
      return;
    }
    const nome = b.dataset["nome"] ?? "";
    if (!window.confirm(`Gerar uma nova senha provisória para ${nome}? A senha atual deixa de funcionar e as sessões abertas são encerradas.`)) {
      return;
    }
    try {
      const r = await api.redefinirSenha(Number(b.dataset["reset"]));
      aviso("Senha redefinida.", "ok");
      $("#senha-reset", raiz).innerHTML = quadroSenhaProvisoria(r.usuario.nome, r.usuario.email, r.senhaProvisoria);
      await carregar();
      $("#senha-reset", raiz).scrollIntoView({ behavior: "smooth", block: "center" });
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  });

  await carregar();
}

// ---------- Logs de auditoria ----------

function dataHora(iso: string): string {
  const [d, h] = iso.split("T");
  const [a, m, dia] = (d ?? "").split("-");
  return `${dia}/${m}/${a} ${h ?? ""}`;
}

export async function mostrarLogs(raiz: HTMLElement): Promise<void> {
  raiz.innerHTML = `
    <section class="cartao">
      <h2>Log de auditoria</h2>
      <p class="ajuda">Ações que alteram dados (logins, cadastros, empréstimos, devoluções, senhas...). Mais recentes primeiro; senhas nunca são registradas.</p>
      <div class="filtros">
        <label>Busca <input id="l-busca" type="search" placeholder="Usuário, ação ou detalhe" autocomplete="off" /></label>
        <label>Ação <select id="l-acao"><option value="">Todas</option></select></label>
        <label>Resultado
          <select id="l-res"><option value="">Todos</option><option value="ok">Sucesso</option><option value="erro">Falha</option></select></label>
        <label>&nbsp;<button type="button" id="l-atualizar" class="secundario">Atualizar</button></label>
      </div>
      <div id="logs"></div>
    </section>`;

  let todos: RegistroLog[] = [];

  function desenhar(): void {
    const termo = normalizar($<HTMLInputElement>("#l-busca", raiz).value);
    const acao = $<HTMLSelectElement>("#l-acao", raiz).value;
    const res = $<HTMLSelectElement>("#l-res", raiz).value;
    const lista = todos.filter(
      (r) =>
        (!acao || r.acao === acao) &&
        (!res || (res === "ok") === r.sucesso) &&
        (!termo || normalizar(`${r.usuario ?? ""} ${r.acao} ${r.detalhe ?? ""} ${r.resultado ?? ""}`).includes(termo)),
    );
    $("#logs", raiz).innerHTML =
      lista.length === 0
        ? `<p class="vazio">Nenhum registro encontrado.</p>`
        : `<p class="contagem">${lista.length} de ${todos.length} registro(s)</p>
        <table><thead><tr><th>Data/hora</th><th>Usuário</th><th>Ação</th><th>Detalhe</th><th>Resultado</th><th>IP</th></tr></thead>
        <tbody>${lista
          .map(
            (r) => `<tr><td style="white-space:nowrap">${esc(dataHora(r.dataHora))}</td>
            <td>${esc(r.usuario ?? "—")}${r.perfil ? `<br><small>${esc(NOME_PERFIL[r.perfil] ?? r.perfil)}</small>` : ""}</td>
            <td>${esc(r.acao)}</td><td><small>${esc(r.detalhe ?? "")}</small></td>
            <td><span class="selo ${r.sucesso ? "verde" : "vermelho"}">${r.sucesso ? "OK" : "Falha"}</span>
              ${r.sucesso ? "" : `<br><small>${esc(r.resultado ?? "")}</small>`}</td>
            <td><small>${esc(r.ip ?? "")}</small></td></tr>`,
          )
          .join("")}</tbody></table>`;
  }

  async function carregar(): Promise<void> {
    try {
      todos = await api.listarLogs();
      const sel = $<HTMLSelectElement>("#l-acao", raiz);
      const escolhida = sel.value;
      const acoes = [...new Set(todos.map((r) => r.acao))].sort((a, b) => a.localeCompare(b, "pt-BR"));
      sel.innerHTML = `<option value="">Todas</option>` + acoes.map((a) => `<option>${esc(a)}</option>`).join("");
      sel.value = acoes.includes(escolhida) ? escolhida : "";
      desenhar();
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  }

  $("#l-busca", raiz).addEventListener("input", desenhar);
  $("#l-acao", raiz).addEventListener("change", desenhar);
  $("#l-res", raiz).addEventListener("change", desenhar);
  $("#l-atualizar", raiz).addEventListener("click", () => void tentar(carregar, "Log atualizado."));
  await carregar();
}
