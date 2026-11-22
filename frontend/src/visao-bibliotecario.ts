import { api } from "./api.js";
import { barras, colunas, CORES, rosca } from "./graficos.js";
import { criarFiltroAcervo } from "./filtros-acervo.js";
import type { Emprestimo, Leitor, Livro, Painel } from "./tipos.js";
import { $, aviso, data, dinheiro, esc, mensagemDe, tentar } from "./ui.js";

// ---------- Painel ----------

function desenharGraficos(p: Painel, livros: Livro[], todos: Emprestimo[]): string {
  // 1) Situação dos exemplares em circulação.
  const emprestados = p.exemplares - p.exemplaresDisponiveis;
  const situacaoExemplares = rosca([
    { rotulo: "Disponíveis", valor: p.exemplaresDisponiveis, cor: CORES.verde },
    { rotulo: "Emprestados", valor: emprestados, cor: CORES.azul },
    { rotulo: "Baixados", valor: p.exemplaresBaixados, cor: CORES.cinza },
  ]);

  // 2) Empréstimos por situação.
  const ativos = todos.filter((e) => e.ativo);
  const emDia = ativos.filter((e) => !e.atrasado).length;
  const atrasados = ativos.filter((e) => e.atrasado).length;
  const devolvidos = todos.length - ativos.length;
  const porSituacao = barras([
    { rotulo: "Em dia", segmentos: [{ valor: emDia, classe: "verde", dica: "Em dia" }] },
    { rotulo: "Em atraso", segmentos: [{ valor: atrasados, classe: "vermelho", dica: "Em atraso" }] },
    { rotulo: "Devolvidos", segmentos: [{ valor: devolvidos, classe: "cinza", dica: "Devolvidos" }] },
  ]);

  // 3) Exemplares por assunto (disponíveis x emprestados).
  const assuntos = new Map<string, { livres: number; total: number }>();
  for (const l of livros) {
    const a = assuntos.get(l.assunto) ?? { livres: 0, total: 0 };
    a.livres += l.disponiveis;
    a.total += l.total;
    assuntos.set(l.assunto, a);
  }
  const porAssunto = barras(
    [...assuntos.entries()]
      .sort((a, b) => b[1].total - a[1].total)
      .map(([rotulo, a]) => ({
        rotulo,
        segmentos: [
          { valor: a.livres, classe: "verde", dica: "Disponíveis" },
          { valor: a.total - a.livres, classe: "azul", dica: "Emprestados" },
        ],
      })),
  );

  // 4) Empréstimos registrados nos últimos 7 dias.
  const dias: { rotulo: string; valor: number }[] = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    dias.push({
      rotulo: `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}`,
      valor: todos.filter((e) => e.dataEmprestimo === iso).length,
    });
  }

  return `
    <div class="grafico"><h3>Situação dos exemplares</h3>${situacaoExemplares}</div>
    <div class="grafico"><h3>Empréstimos por situação</h3>${porSituacao}</div>
    <div class="grafico"><h3>Exemplares por assunto <small>(verde = disponível, azul = emprestado)</small></h3>${porAssunto}</div>
    <div class="grafico"><h3>Empréstimos nos últimos 7 dias</h3>${colunas(dias)}</div>`;
}

export async function mostrarPainel(raiz: HTMLElement): Promise<void> {
  raiz.innerHTML = `
    <section class="cartao"><h2>Painel</h2><div id="cartoes" class="grade"></div></section>
    <section class="cartao"><h2>Gráficos</h2><div id="graficos" class="graficos"></div></section>`;
  try {
    const [p, livros, todos] = await Promise.all([api.painel(), api.buscarLivros(""), api.listarEmprestimos("todos")]);
    $("#graficos", raiz).innerHTML = desenharGraficos(p, livros, todos);
    const item = (rotulo: string, valor: string | number, destaque = false): string =>
      `<div class="indicador ${destaque ? "alerta" : ""}"><span>${rotulo}</span><strong>${valor}</strong></div>`;
    $("#cartoes", raiz).innerHTML = [
      item("Empréstimos ativos", p.emprestimosAtivos),
      item("Em atraso", p.emprestimosAtrasados, p.emprestimosAtrasados > 0),
      item("Devoluções hoje", p.devolucoesHoje),
      item("Multas em aberto", `${p.multasEmAberto} (${dinheiro(p.valorMultasEmAberto)})`, p.multasEmAberto > 0),
      item("Títulos", p.titulos),
      item("Exemplares em circulação", p.exemplares),
      item("Exemplares disponíveis", p.exemplaresDisponiveis),
      item("Exemplares baixados", p.exemplaresBaixados),
    ].join("");
  } catch (e) {
    aviso(mensagemDe(e), "erro");
  }
}

// ---------- Balcão: empréstimo e devolução ----------

function linhaEmprestimo(e: Emprestimo, comAcoes: boolean): string {
  const situacao = e.atrasado
    ? `<span class="selo vermelho">Atrasado (${e.diasAtraso} dia(s))</span>`
    : `<span class="selo verde">Em dia</span>`;
  const acoes = comAcoes
    ? `<td class="acoes">
         <button data-devolver="${e.id}">Devolver</button>
         <button data-renovar="${e.id}" class="secundario" ${e.podeRenovar ? "" : "disabled"}>Renovar</button>
       </td>`
    : "";
  return `<tr>
    <td>${esc(e.leitor)}<br><small>${esc(e.matricula)}</small></td>
    <td><strong>${esc(e.titulo)}</strong><br><small>Tombo ${esc(e.codigoTombo)}</small></td>
    <td>${data(e.dataEmprestimo)}</td>
    <td>${data(e.dataPrevista)}</td>
    <td>${situacao}</td>${acoes}</tr>`;
}

export async function mostrarBalcao(raiz: HTMLElement): Promise<void> {
  raiz.innerHTML = `
    <section class="cartao">
      <h2>Registrar empréstimo</h2>
      <p class="ajuda">O sistema confere se o exemplar está livre e se o leitor não tem atraso, multa em aberto ou limite atingido.</p>
      <form id="emprestar" class="linha">
        <input id="matricula" placeholder="Matrícula do leitor (ex.: RA2024001)" required />
        <input id="tombo" placeholder="Código de tombo (ex.: TB-0008)" required />
        <button type="submit">Emprestar</button>
      </form>
    </section>
    <section class="cartao">
      <details id="leitores-det">
      <summary class="titulo-recolhivel">Buscar leitor cadastrado</summary>
      <p class="ajuda">Digite o nome, o RA ou o e-mail e use “Usar RA” para preencher a matrícula do empréstimo.</p>
      <input id="busca-leitor" type="search" placeholder="Ex.: Ana, RA2024001" style="width:100%" />
      <div id="sugestoes" class="sugestoes"></div>
      </details>
    </section>
    <section class="cartao">
      <h2>Empréstimos em aberto</h2>
      <div id="lista"></div>
    </section>`;

  const lista = $("#lista", raiz);
  const sugestoes = $("#sugestoes", raiz);
  let leitores: Leitor[] = [];

  function mostrarSugestoes(): void {
    const termo = $<HTMLInputElement>("#busca-leitor", raiz).value;
    const achados = filtrarLeitores(leitores, termo);
    sugestoes.innerHTML = achados.length === 0
      ? `<p class="vazio">Nenhum leitor encontrado.</p>`
      : achados
          .map(
            (l) => `<div class="sugestao"><span><strong>${esc(l.nome)}</strong> — RA <strong>${esc(l.matricula)}</strong>
              <small>(${esc(l.categoria)}, ${l.emprestimosAtivos}/${l.limiteObras} obras)</small></span>
              <span><span class="selo ${l.podeEmprestar ? "verde" : "vermelho"}">${l.podeEmprestar ? "Pode emprestar" : l.possuiPendencias ? "Com pendências" : "Limite atingido"}</span>
              <button type="button" class="secundario" data-usar="${esc(l.matricula)}">Usar RA</button></span></div>`,
          )
          .join("");
  }
  $("#busca-leitor", raiz).addEventListener("input", mostrarSugestoes);
  sugestoes.addEventListener("click", (ev) => {
    const b = (ev.target as HTMLElement).closest<HTMLButtonElement>("[data-usar]");
    if (b) {
      const campo = $<HTMLInputElement>("#matricula", raiz);
      campo.value = b.dataset["usar"] ?? "";
      $<HTMLInputElement>("#tombo", raiz).focus();
    }
  });
  api.listarLeitores().then((l) => {
    leitores = l;
    mostrarSugestoes();
  }).catch((e) => aviso(mensagemDe(e), "erro"));

  async function carregar(): Promise<void> {
    try {
      const itens = await api.listarEmprestimos("ativos");
      lista.innerHTML =
        itens.length === 0
          ? `<p class="vazio">Nenhum empréstimo em aberto.</p>`
          : `<table><thead><tr><th>Leitor</th><th>Exemplar</th><th>Retirada</th><th>Devolver até</th><th>Situação</th><th></th></tr></thead>
             <tbody>${itens.map((e) => linhaEmprestimo(e, true)).join("")}</tbody></table>`;
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  }

  $("#emprestar", raiz).addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const matricula = $<HTMLInputElement>("#matricula", raiz);
    const tombo = $<HTMLInputElement>("#tombo", raiz);
    if (await tentar(() => api.emprestar(matricula.value, tombo.value), "Empréstimo registrado.")) {
      matricula.value = "";
      tombo.value = "";
      await carregar();
    }
  });

  lista.addEventListener("click", async (ev) => {
    const alvo = ev.target as HTMLElement;
    const dev = alvo.closest<HTMLButtonElement>("[data-devolver]");
    const ren = alvo.closest<HTMLButtonElement>("[data-renovar]");
    if (dev) {
      try {
        const e = await api.devolver(Number(dev.dataset["devolver"]));
        aviso(e.multa ? `Devolvido com atraso. Multa gerada: ${dinheiro(e.multa.valor)}.` : "Devolução registrada.", e.multa ? "erro" : "ok");
        await carregar();
      } catch (e) {
        aviso(mensagemDe(e), "erro");
      }
    } else if (ren && (await tentar(() => api.renovar(Number(ren.dataset["renovar"])), "Empréstimo renovado."))) {
      await carregar();
    }
  });
  await carregar();
}

// ---------- Atrasos ----------

export async function mostrarAtrasos(raiz: HTMLElement): Promise<void> {
  raiz.innerHTML = `<section class="cartao"><h2>Empréstimos em atraso</h2><div id="lista"></div></section>`;
  try {
    const itens = await api.listarEmprestimos("atrasados");
    $("#lista", raiz).innerHTML =
      itens.length === 0
        ? `<p class="vazio">Nenhum atraso hoje.</p>`
        : `<table><thead><tr><th>Leitor</th><th>Exemplar</th><th>Retirada</th><th>Devolver até</th><th>Situação</th></tr></thead>
           <tbody>${itens.map((e) => linhaEmprestimo(e, false)).join("")}</tbody></table>`;
  } catch (e) {
    aviso(mensagemDe(e), "erro");
  }
}

// ---------- Acervo: livros, exemplares, baixa ----------

export async function mostrarAcervo(raiz: HTMLElement): Promise<void> {
  raiz.innerHTML = `
    <section class="cartao">
      <h2>Cadastrar livro (título)</h2>
      <form id="novo-livro" class="grade-form">
        <input id="titulo" placeholder="Título" required />
        <input id="autor" placeholder="Autor" required />
        <input id="editora" placeholder="Editora" required />
        <input id="isbn" placeholder="ISBN" required />
        <input id="assunto" placeholder="Assunto" required />
        <button type="submit">Cadastrar livro</button>
      </form>
    </section>
    <section class="cartao">
      <h2>Acervo</h2>
      <div id="filtros"></div>
      <div id="livros"></div>
    </section>
    <section class="cartao" id="detalhe" hidden></section>`;

  const livrosEl = $("#livros", raiz);
  const detalhe = $("#detalhe", raiz);
  let todos: Livro[] = [];

  function desenhar(): void {
    const livros = filtro.filtrar(todos);
    livrosEl.innerHTML =
      livros.length === 0
        ? `<p class="vazio">Nenhum título encontrado com esses filtros.</p>`
        : `<p class="contagem">${livros.length} de ${todos.length} título(s)</p>
        <table><thead><tr><th>Título</th><th>Autor</th><th>Assunto</th><th>ISBN</th><th>Exemplares</th><th></th></tr></thead>
        <tbody>${livros
          .map(
            (l) => `<tr><td><strong>${esc(l.titulo)}</strong></td><td>${esc(l.autor)}</td><td>${esc(l.assunto)}</td><td>${esc(l.isbn)}</td>
            <td><span class="selo ${l.disponiveis > 0 ? "verde" : "vermelho"}">${l.disponiveis} livre(s) / ${l.total}</span></td>
            <td><button class="secundario" data-livro="${l.id}" data-titulo="${esc(l.titulo)}">Exemplares</button></td></tr>`,
          )
          .join("")}</tbody></table>`;
  }

  const filtro = criarFiltroAcervo($("#filtros", raiz), desenhar);

  async function carregar(): Promise<void> {
    try {
      todos = await api.buscarLivros("");
      filtro.popular(todos);
      desenhar();
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  }

  async function abrirExemplares(id: number, titulo: string): Promise<void> {
    try {
      const itens = await api.listarExemplares(id);
      const jaAberto = !detalhe.hidden;
      detalhe.hidden = false;
      detalhe.innerHTML = `
        <h2>Exemplares de “${esc(titulo)}”</h2>
        <form id="novo-exemplar" class="linha">
          <input id="tombo-novo" placeholder="Novo código de tombo (ex.: TB-0012)" required />
          <button type="submit">Adicionar exemplar</button>
        </form>
        <table><thead><tr><th>Tombo</th><th>Situação</th><th>Baixa</th><th></th></tr></thead>
        <tbody>${itens
          .map(
            (x) => `<tr><td>${esc(x.codigoTombo)}</td>
              <td><span class="selo ${x.situacao === "DISPONIVEL" ? "verde" : x.situacao === "BAIXADO" ? "cinza" : "amarelo"}">${esc(x.situacao)}</span></td>
              <td>${x.dataBaixa ? `${data(x.dataBaixa)} — ${esc(x.motivoBaixa)}` : "—"}</td>
              <td>${x.situacao === "BAIXADO" ? "" : `<button class="perigo" data-baixa="${x.id}">Dar baixa</button>`}</td></tr>`,
          )
          .join("")}</tbody></table>`;
      $("#novo-exemplar", detalhe).addEventListener("submit", async (ev) => {
        ev.preventDefault();
        if (await tentar(() => api.cadastrarExemplar(id, $<HTMLInputElement>("#tombo-novo", detalhe).value), "Exemplar cadastrado.")) {
          await abrirExemplares(id, titulo);
          await carregar();
        }
      });
      detalhe.querySelectorAll<HTMLButtonElement>("[data-baixa]").forEach((b) =>
        b.addEventListener("click", async () => {
          const motivo = window.prompt("Motivo da baixa (perda, dano...):");
          if (motivo && (await tentar(() => api.baixarExemplar(Number(b.dataset["baixa"]), motivo), "Baixa registrada."))) {
            await abrirExemplares(id, titulo);
            await carregar();
          }
        }),
      );
      if (!jaAberto) {
        detalhe.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  }

  $("#novo-livro", raiz).addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const v = (id: string): string => $<HTMLInputElement>(`#${id}`, raiz).value;
    const dados = { titulo: v("titulo"), autor: v("autor"), editora: v("editora"), isbn: v("isbn"), assunto: v("assunto") };
    if (await tentar(() => api.cadastrarLivro(dados), "Livro cadastrado.")) {
      ($("#novo-livro", raiz) as HTMLFormElement).reset();
      await carregar();
    }
  });

  livrosEl.addEventListener("click", (ev) => {
    const b = (ev.target as HTMLElement).closest<HTMLButtonElement>("[data-livro]");
    if (b) {
      void abrirExemplares(Number(b.dataset["livro"]), b.dataset["titulo"] ?? "");
    }
  });
  await carregar();
}

// ---------- Leitores e multas ----------

export async function mostrarLeitores(raiz: HTMLElement): Promise<void> {
  raiz.innerHTML = `
    <section class="cartao">
      <h2>Cadastrar leitor</h2>
      <form id="novo-leitor" class="grade-form">
        <input id="nome" placeholder="Nome completo" required />
        <input id="email" type="email" placeholder="E-mail" required />
        <input id="matricula" placeholder="Matrícula" required />
        <input id="senha" type="password" placeholder="Senha inicial (mín. 6)" minlength="6" required />
        <select id="categoria" required></select>
        <button type="submit">Cadastrar leitor</button>
      </form>
    </section>
    <section class="cartao"><h2>Multas em aberto</h2><div id="multas"></div></section>
    <section class="cartao"><h2>Leitores</h2>
      <input id="busca-leitores" type="search" placeholder="Buscar por nome, RA ou e-mail" style="width:100%;margin-bottom:12px" />
      <div id="leitores"></div></section>`;
  let cadastrados: Leitor[] = [];

  function desenharLeitores(): void {
    const lista = filtrarLeitores(cadastrados, $<HTMLInputElement>("#busca-leitores", raiz).value);
    $("#leitores", raiz).innerHTML = lista.length === 0
      ? `<p class="vazio">Nenhum leitor encontrado.</p>`
      : `<p class="contagem">${lista.length} de ${cadastrados.length} leitor(es)</p><table><thead><tr><th>Nome</th><th>RA / matrícula</th><th>Categoria</th><th>Empréstimos</th><th>Situação</th></tr></thead>
        <tbody>${lista
          .map(
            (l) => `<tr><td>${esc(l.nome)}<br><small>${esc(l.email)}</small></td><td>${esc(l.matricula)}</td><td>${esc(l.categoria)}</td>
            <td>${l.emprestimosAtivos} / ${l.limiteObras}</td>
            <td><span class="selo ${l.podeEmprestar ? "verde" : "vermelho"}">${l.podeEmprestar ? "Pode emprestar" : l.possuiPendencias ? "Com pendências" : "Limite atingido"}</span></td></tr>`,
          )
          .join("")}</tbody></table>`;
  }
  $("#busca-leitores", raiz).addEventListener("input", desenharLeitores);


  async function carregar(): Promise<void> {
    try {
      const [leitores, todos] = await Promise.all([api.listarLeitores(), api.listarEmprestimos("todos")]);
      cadastrados = leitores;
      desenharLeitores();

      const comMulta = todos.filter((e) => e.multa && !e.multa.paga);
      $("#multas", raiz).innerHTML =
        comMulta.length === 0
          ? `<p class="vazio">Nenhuma multa em aberto.</p>`
          : `<table><thead><tr><th>Leitor</th><th>Exemplar</th><th>Dias de atraso</th><th>Valor</th><th></th></tr></thead>
             <tbody>${comMulta
               .map(
                 (e) => `<tr><td>${esc(e.leitor)}<br><small>${esc(e.matricula)}</small></td><td>${esc(e.titulo)}</td>
                 <td>${e.diasAtraso}</td><td>${dinheiro(e.multa?.valor ?? 0)}</td>
                 <td><button data-multa="${e.multa?.id}">Registrar pagamento</button></td></tr>`,
               )
               .join("")}</tbody></table>`;
    } catch (e) {
      aviso(mensagemDe(e), "erro");
    }
  }

  try {
    const categorias = await api.listarCategorias();
    $("#categoria", raiz).innerHTML = categorias
      .map((c) => `<option value="${c.id}">${esc(c.nome)} — ${c.prazoDias} dias, ${c.limiteObras} obras</option>`)
      .join("");
  } catch (e) {
    aviso(mensagemDe(e), "erro");
  }

  $("#novo-leitor", raiz).addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const v = (id: string): string => $<HTMLInputElement>(`#${id}`, raiz).value;
    const dados = { nome: v("nome"), email: v("email"), senha: v("senha"), matricula: v("matricula"), categoriaId: Number(v("categoria")) };
    if (await tentar(() => api.cadastrarLeitor(dados), "Leitor cadastrado.")) {
      ($("#novo-leitor", raiz) as HTMLFormElement).reset();
      await carregar();
    }
  });

  $("#multas", raiz).addEventListener("click", async (ev) => {
    const b = (ev.target as HTMLElement).closest<HTMLButtonElement>("[data-multa]");
    if (b && (await tentar(() => api.quitarMulta(Number(b.dataset["multa"])), "Multa quitada."))) {
      await carregar();
    }
  });
  await carregar();
}

// ---------- Busca de leitores (nome, RA ou e-mail) ----------

function normalizarBusca(t: string): string {
  return t.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().trim();
}

export function filtrarLeitores(leitores: Leitor[], termo: string): Leitor[] {
  const t = normalizarBusca(termo);
  return t ? leitores.filter((l) => normalizarBusca(`${l.nome} ${l.matricula} ${l.email}`).includes(t)) : leitores;
}
