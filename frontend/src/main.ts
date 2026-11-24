import { api, sessaoAtual } from "./api.js";
import { $, aviso, esc, mensagemDe } from "./ui.js";
import { mostrarConsulta } from "./visao-consulta.js";
import { mostrarLogin } from "./visao-login.js";
import { mostrarMeusEmprestimos } from "./visao-leitor.js";
import { mostrarLogs, mostrarUsuariosAdmin } from "./visao-admin.js";
import { mostrarTrocaSenha } from "./visao-senha.js";
import {
  mostrarAcervo,
  mostrarAtrasos,
  mostrarBalcao,
  mostrarLeitores,
  mostrarPainel,
} from "./visao-bibliotecario.js";

/** `oculta`: a tela existe mas não aparece no menu (visitante usa o botão "Entrar" à direita). */
type Tela = { id: string; titulo: string; abrir: (raiz: HTMLElement) => Promise<void> | void; oculta?: boolean };

const raiz = $("#conteudo");

const ROTULO_PERFIL = { LEITOR: "leitor", BIBLIOTECARIO: "bibliotecário", ADMIN: "administrador" } as const;

function telasDisponiveis(): Tela[] {
  const sessao = sessaoAtual();
  const consulta: Tela = { id: "acervo", titulo: "Consultar acervo", abrir: mostrarConsulta };
  if (!sessao) {
    return [
      { ...consulta, oculta: true },
      {
        id: "login",
        titulo: "Entrar",
        oculta: true,
        abrir: (r) => mostrarLogin(r, () => navegar("inicio"), () => navegar("acervo")),
      },
    ];
  }
  // Senha provisória: nada além da troca de senha até que ela seja feita.
  if (sessao.trocarSenha) {
    return [{ id: "senha", titulo: "Definir nova senha", abrir: (r) => mostrarTrocaSenha(r, true, () => navegar("inicio")) }];
  }
  const trocarSenha: Tela = {
    id: "senha",
    titulo: "Trocar senha",
    oculta: true,
    abrir: (r) => mostrarTrocaSenha(r, false, () => navegar("inicio")),
  };
  if (sessao.perfil === "LEITOR") {
    return [{ id: "meus", titulo: "Meus empréstimos", abrir: mostrarMeusEmprestimos }, consulta, trocarSenha];
  }
  const balcao: Tela[] = [
    { id: "painel", titulo: "Painel", abrir: mostrarPainel },
    { id: "balcao", titulo: "Balcão", abrir: mostrarBalcao },
    { id: "atrasos", titulo: "Atrasos", abrir: mostrarAtrasos },
    { id: "acervo-adm", titulo: "Acervo", abrir: mostrarAcervo },
    { id: "leitores", titulo: "Leitores e multas", abrir: mostrarLeitores },
  ];
  // Administrador: acesso total (tudo do bibliotecário + administração).
  if (sessao.perfil === "ADMIN") {
    return [
      ...balcao,
      { id: "usuarios", titulo: "Usuários e senhas", abrir: mostrarUsuariosAdmin },
      { id: "logs", titulo: "Logs", abrir: mostrarLogs },
      consulta,
      trocarSenha,
    ];
  }
  return [...balcao, consulta, trocarSenha];
}

function desenharMenu(ativa: string): void {
  const sessao = sessaoAtual();
  const telas = telasDisponiveis();
  $("#menu").innerHTML = telas
    .filter((t) => !t.oculta)
    .map((t) => `<button data-tela="${t.id}" class="${t.id === ativa ? "ativo" : ""}">${esc(t.titulo)}</button>`)
    .join("");
  $("#usuario").innerHTML = sessao
    ? `<span>${esc(sessao.nome)} <small>(${ROTULO_PERFIL[sessao.perfil] ?? sessao.perfil})</small></span>` +
      (sessao.trocarSenha ? "" : `<button id="minha-senha" class="secundario ${ativa === "senha" ? "ativo" : ""}">Trocar senha</button>`) +
      `<button id="sair" class="secundario">Sair</button>`
    : `<button id="entrar" class="${ativa === "login" ? "ativo" : ""}">Entrar</button>`;
}

async function navegar(id: string): Promise<void> {
  const telas = telasDisponiveis();
  const tela = telas.find((t) => t.id === id) ?? telas[0];
  if (!tela) {
    return;
  }
  desenharMenu(tela.id);
  try {
    await tela.abrir(raiz);
  } catch (e) {
    aviso(mensagemDe(e), "erro");
  }
}

$("#menu").addEventListener("click", (ev) => {
  const b = (ev.target as HTMLElement).closest<HTMLButtonElement>("[data-tela]");
  if (b?.dataset["tela"]) {
    void navegar(b.dataset["tela"]);
  }
});

$("#usuario").addEventListener("click", async (ev) => {
  const alvo = ev.target as HTMLElement;
  if (alvo.closest("#entrar")) {
    await navegar("login");
  } else if (alvo.closest("#minha-senha")) {
    await navegar("senha");
  } else if (alvo.closest("#sair")) {
    await api.logout().catch(() => undefined);
    await navegar("acervo");
  }
});

void navegar("inicio");
