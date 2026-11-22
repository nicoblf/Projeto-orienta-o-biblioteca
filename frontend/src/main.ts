import { api, sessaoAtual } from "./api.js";
import { $, aviso, esc, mensagemDe } from "./ui.js";
import { mostrarConsulta } from "./visao-consulta.js";
import { mostrarLogin } from "./visao-login.js";
import { mostrarMeusEmprestimos } from "./visao-leitor.js";
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
  if (sessao.perfil === "LEITOR") {
    return [{ id: "meus", titulo: "Meus empréstimos", abrir: mostrarMeusEmprestimos }, consulta];
  }
  return [
    { id: "painel", titulo: "Painel", abrir: mostrarPainel },
    { id: "balcao", titulo: "Balcão", abrir: mostrarBalcao },
    { id: "atrasos", titulo: "Atrasos", abrir: mostrarAtrasos },
    { id: "acervo-adm", titulo: "Acervo", abrir: mostrarAcervo },
    { id: "leitores", titulo: "Leitores e multas", abrir: mostrarLeitores },
    consulta,
  ];
}

function desenharMenu(ativa: string): void {
  const sessao = sessaoAtual();
  const telas = telasDisponiveis();
  $("#menu").innerHTML = telas
    .filter((t) => !t.oculta)
    .map((t) => `<button data-tela="${t.id}" class="${t.id === ativa ? "ativo" : ""}">${esc(t.titulo)}</button>`)
    .join("");
  $("#usuario").innerHTML = sessao
    ? `<span>${esc(sessao.nome)} <small>(${sessao.perfil === "LEITOR" ? "leitor" : "bibliotecário"})</small></span><button id="sair" class="secundario">Sair</button>`
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
  if ((ev.target as HTMLElement).closest("#entrar")) {
    await navegar("login");
  } else if ((ev.target as HTMLElement).closest("#sair")) {
    await api.logout().catch(() => undefined);
    await navegar("acervo");
  }
});

void navegar("inicio");
