# Sistema de Controle de Biblioteca — Fase 3 (Implementação)

Projeto Interdisciplinar de Orientação a Objetos — UNIFACCAMP
José Francisco (RA 33759) · Nicolas Beltranelo (RA 33732) · Prof. Marcio Cruz

Backend em **Java 21** (só o JDK + driver JDBC) + interface em **TypeScript** + banco **PostgreSQL**, em 3 contêineres.
Implementa o diagrama de classes da Fase 2 e as regras RF01–RF10. Os dados ficam no PostgreSQL (volume `pgdata`).

## Como executar

### Opção 1 — Docker (recomendada; é o que as Fases 1 e 2 previram)

```bash
docker compose up --build
```


Abra <http://localhost:8080>.


São **3 contêineres** — `db` (PostgreSQL com volume `pgdata`), `backend` (API Java) e `frontend` (nginx servindo o TypeScript; única porta publicada, 8080).
Para apagar os dados e recomeçar: `docker compose down -v`.

### Demonstração em console e testes (sem navegador, só memória)

```bash
docker compose run --rm --no-deps backend java -cp out biblioteca.Main --demo     # regras no terminal
docker compose run --rm --no-deps backend java -cp out biblioteca.TesteRegras  # 29 verificações
```

## Contas de demonstração (dados fictícios)

| Perfil | E-mail | Senha |
|---|---|---|
| Bibliotecário | bibliotecario@biblioteca.com | admin123 |
| Leitor (aluna) | ana@aluno.com | leitor123 |
| Leitor com **atraso** | bruno@aluno.com | leitor123 |
| Leitora professora | carla@escola.com | leitor123 |
| Leitor com **multa em aberto** | diego@aluno.com | leitor123 |

Matrículas para o balcão: `RA2024001` (Ana), `RA2024002` (Bruno), `PROF0001` (Carla), `RA2024003` (Diego).
Códigos de tombo livres para testar: `TB-0008`, `TB-0009`, `TB-0010`.

## Estrutura

```
backend/src/biblioteca/
  modelo/       classes do diagrama da Fase 2 (Usuario, Leitor, Bibliotecario, Livro, Exemplar, Emprestimo, Multa, ...)
  repositorio/  repositórios + acesso JDBC ao PostgreSQL (Banco)
  servico/      regras de orquestração (acervo, leitores, circulação, autenticação)
  web/          controladores e roteador HTTP (API REST em JSON)
  util/         Json e SenhaUtil (PBKDF2)
frontend/       interface em TypeScript + nginx (Dockerfile, nginx.conf)
docs/           documentação da Fase 3 e evidências de execução
```

Documentação completa: [docs/Documentacao_Fase3.pdf](docs/Documentacao_Fase3.pdf) (fonte em `docs/Documentacao_Fase3.html`)

## Recursos de interface adicionados

- **Autocadastro de leitor** na tela de login (aba "Criar conta de leitor"; informa nome, RA, e-mail e senha) — `POST /api/cadastro`.
- **Painel** com gráficos (situação dos exemplares, empréstimos por situação, exemplares por assunto, empréstimos nos últimos 7 dias).
- **Balcão**: busca de leitores cadastrados por nome, RA ou e-mail, com o botão "Usar RA". **Leitores e multas** também tem busca.
- **Acervo** (consulta pública e área do bibliotecário): filtros por assunto, autor e disponibilidade, além da busca por texto.

