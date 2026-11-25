# Obrix — Contexto técnico do projeto

Documento de referência da equipe. Descreve o sistema como ele está no código hoje.
Quando este texto e o código divergirem, vale o código, e este documento deve ser
corrigido.

## Sumário

1. [O que é o sistema](#1-o-que-é-o-sistema)
2. [A regra principal: comissão só na confirmação](#2-a-regra-principal-comissão-só-na-confirmação)
3. [Ciclo de vida da solicitação](#3-ciclo-de-vida-da-solicitação)
4. [Arquitetura](#4-arquitetura)
5. [Organização dos pacotes](#5-organização-dos-pacotes)
6. [Convenções de código](#6-convenções-de-código)
7. [Como rodar](#7-como-rodar)
8. [Como testar](#8-como-testar)
9. [O que já está pronto](#9-o-que-já-está-pronto)
10. [O que falta](#10-o-que-falta)

---

## 1. O que é o sistema

O Obrix é uma plataforma web que liga clientes a prestadores autônomos de serviços
residenciais, como eletricista, encanador, pintor, diarista e jardineiro.

- O **cliente** busca profissionais por categoria e região, envia uma solicitação,
  acompanha o andamento, confirma ou contesta a conclusão e avalia o serviço.
- O **prestador** recebe solicitações, aceita ou recusa, executa, registra a conclusão
  com o valor final e acompanha o extrato de comissões.

Buscar, receber solicitações e trocar contato não têm custo. A plataforma ganha uma
comissão de 10% sobre o valor do serviço, cobrada do prestador, e só depois que o cliente
confirma a conclusão informando o valor que pagou.

**Fora do escopo:** pagamento real (a cobrança é registrada, não movimenta dinheiro),
aplicativo nativo, chat em tempo real, verificação de antecedentes e perfil
administrativo. O catálogo de categorias e regiões entra por migration.

---

## 2. A regra principal: comissão só na confirmação

> A comissão de 10% só nasce quando o cliente confirma a conclusão do serviço, informando
> um valor pago igual ao valor final registrado pelo prestador.

Garantias que o código oferece:

1. **Ponto único de criação.** `Cobranca` só é instanciada em
   `SolicitacaoService.concluir`, chamado por `confirmar` (cliente, com valor igual) e por
   `confirmarAutomaticamente` (prazo). Nenhum outro fluxo cria cobrança.
2. **Registrar a conclusão não cobra.** O prestador informa que terminou e o valor final
   (`/concluir`), e a solicitação vai para `AGUARDANDO_CONFIRMACAO`, sem cobrança.
3. **O cliente confirma o valor (contra a subdeclaração).** Quem paga a comissão é o
   prestador, então o valor declarado por ele não basta. `POST /confirmar` exige
   `{"valorPago": ...}`, obrigatório e maior que zero (`ConfirmacaoRequest`; senão, 400
   `DADOS_INVALIDOS`). `Solicitacao.informarValorPago` compara com `compareTo`, então 280
   e 280.00 são iguais:
   - **Igual:** grava `valor_confirmado_cliente` e segue para `concluir`, que gera a
     cobrança sobre o `valorFinal`.
   - **Diferente:** a solicitação vai para `CONTESTADA` pela transição normal, com autor
     `CLIENTE` e motivo automático `Divergencia de valor: o valor informado pelo cliente
     (R$ X) nao confere com o registrado pelo profissional.` Nenhuma cobrança é gerada e
     o prestador é notificado. Como toda contestação, desliga a confirmação automática; a
     saída é o prestador registrar a conclusão de novo.
   - Fora de `AGUARDANDO_CONFIRMACAO`, responde `TRANSICAO_INVALIDA`.

   **Confirmação às cegas.** A checagem só vale se o cliente não puder copiar o número do
   prestador. Por isso:
   - `SolicitacaoMapper.paraResposta` devolve `valorFinal` e `valorConfirmadoCliente`
     nulos quando quem vê é o cliente e a solicitação não está `CONCLUIDA`. Vale para
     `GET /solicitacoes`, `GET /solicitacoes/{id}` e para as respostas das ações. O
     prestador sempre vê o próprio `valorFinal`. Depois de `CONCLUIDA`, o cliente vê os
     dois valores.
   - O motivo da divergência fica no histórico que o cliente lê, então mostra só o valor
     do cliente. Senão, errar de propósito revelaria o valor do prestador.
   - No painel do cliente, o cartão em `AGUARDANDO_CONFIRMACAO` e `CONTESTADA` mostra
     "Valor: você informa ao confirmar", e o modal de confirmação não mostra o valor do
     profissional nem a comissão em reais (que o revelaria por conta simples). O
     `demo.js` segue a mesma regra, e `Api.motivoDivergencia` gera o mesmo texto do
     back-end.
4. **Confirmação automática.** Se o cliente não responde em 7 dias
   (`obrix.confirmacao.prazo-dias`), a tarefa agendada confirma. Fica gravado
   `confirmacao_automatica = true` e o autor `SISTEMA` no histórico. A cobrança usa o
   `valorFinal`, e `valor_confirmado_cliente` fica nulo.
5. **Contestar desliga o prazo de vez.** `isFoiContestada()` é calculado a partir do
   histórico, sem coluna própria. Uma solicitação que já passou por `CONTESTADA` nunca
   mais é confirmada automaticamente.
6. **No máximo uma cobrança por solicitação.** A verificação existe no serviço e também
   como `UNIQUE (solicitacao_id)` na tabela `cobranca`, o que cobre duplo clique e
   repetição de requisição.
7. **O percentual fica gravado na cobrança.** `cobranca.percentual_comissao` guarda o
   valor vigente no momento da geração. Se a política mudar, as cobranças antigas não se
   alteram.
8. **Cálculo:** `valor × percentual ÷ 100`, em `BigDecimal`, escala 2, `HALF_UP`
   (`Cobranca.calcularComissao`). Exemplo de referência: 10% de R$ 280,00 = R$ 28,00.

O percentual vem de `obrix.comissao.percentual` (`10.00`) em `application.yml`.

### Riscos do modelo de negócio

| Risco | Situação |
|---|---|
| **Subdeclaração do valor** (o prestador registra menos do que recebeu para pagar menos comissão) | **Mitigado** pela confirmação com o valor pago, feita às cegas: até a conclusão o cliente não vê o valor do prestador, nem na API nem no motivo de uma divergência. Valor diferente vira `CONTESTADA`, sem cobrança. Limite: se cliente e prestador combinarem o valor por fora, a checagem não detecta. |
| **Reputação inflada** | **Mitigado.** Avaliação só em `CONCLUIDA`, só pelo cliente, uma por solicitação. |
| **Desintermediação** (as partes fecham os próximos serviços fora da plataforma) | **Não resolvido.** Depende de pagamento intermediado, hoje fora do escopo. |

---

## 3. Ciclo de vida da solicitação

São oito status, definidos em `StatusSolicitacao`:

| Status | Significado | Final? |
|---|---|---|
| `SOLICITADA` | Cliente enviou o pedido | não |
| `ACEITA` | Prestador aceitou; os contatos são liberados | não |
| `RECUSADA` | Prestador recusou | **sim** |
| `EM_EXECUCAO` | Prestador iniciou o serviço | não |
| `AGUARDANDO_CONFIRMACAO` | Prestador registrou a conclusão e o valor | não |
| `CONTESTADA` | Cliente contestou a conclusão | não |
| `CONCLUIDA` | Conclusão confirmada; a cobrança foi gerada | **sim** |
| `CANCELADA` | Cancelada por uma das partes | **sim** |

### Transições permitidas

O mapa fica em `Solicitacao.TRANSICOES`. Qualquer outra transição lança
`TransicaoInvalidaException` (código `TRANSICAO_INVALIDA`).

| De | Para | Ação / endpoint | Quem | Motivo obrigatório |
|---|---|---|---|---|
| — | `SOLICITADA` | `POST /solicitacoes` | Cliente | — |
| `SOLICITADA` | `ACEITA` | `/aceitar` | Prestador | não |
| `SOLICITADA` | `RECUSADA` | `/recusar` | Prestador | **sim** |
| `SOLICITADA` | `CANCELADA` | `/cancelar` | Cliente | **sim** |
| `ACEITA` | `EM_EXECUCAO` | `/iniciar` | Prestador | não |
| `ACEITA` | `CANCELADA` | `/cancelar` | Cliente ou prestador | **sim** |
| `EM_EXECUCAO` | `AGUARDANDO_CONFIRMACAO` | `/concluir` (com valor final) | Prestador | não |
| `AGUARDANDO_CONFIRMACAO` | `CONCLUIDA` | `/confirmar` com `valorPago` **igual** ao valor final | Cliente | não |
| `AGUARDANDO_CONFIRMACAO` | `CONCLUIDA` | tarefa agendada (7 dias) | Sistema | não |
| `AGUARDANDO_CONFIRMACAO` | `CONTESTADA` | `/contestar` | Cliente | **sim** |
| `AGUARDANDO_CONFIRMACAO` | `CONTESTADA` | `/confirmar` com `valorPago` **diferente** do valor final | Cliente | automático (divergência de valor) |
| `CONTESTADA` | `AGUARDANDO_CONFIRMACAO` | `/concluir` (novo registro) | Prestador | não |

```
SOLICITADA ──aceitar──> ACEITA ──iniciar──> EM_EXECUCAO ──concluir──> AGUARDANDO_CONFIRMACAO
    │                     │                                              │        ▲
    ├──recusar──> RECUSADA│                                       contestar      concluir
    └──cancelar─┐         └──cancelar──> CANCELADA                       ▼        │
                └──────────────────────> CANCELADA                    CONTESTADA ─┘

AGUARDANDO_CONFIRMACAO ──confirmar (valor pago igual, ou prazo)──> CONCLUIDA  ⇒  gera Cobranca
AGUARDANDO_CONFIRMACAO ──confirmar (valor pago diferente)──────> CONTESTADA  (sem cobrança)
```

Observações:

- **Toda transição grava um `HistoricoStatus`** com status anterior, status novo,
  motivo, autor (`CLIENTE`, `PRESTADOR` ou `SISTEMA`) e data. A solicitação já nasce com
  o primeiro registro, então o histórico nunca fica vazio.
- **Autorização por objeto:** além de estar logado, o usuário precisa ser o cliente dono
  (`carregarDoCliente`) ou o prestador destinatário (`carregarDoPrestador`) da
  solicitação.
- **Concorrência:** `Solicitacao` tem `@Version`. Se duas ações chegam ao mesmo tempo
  (por exemplo, o cliente cancela enquanto o prestador aceita), uma delas falha em vez de
  sobrescrever a outra.
- **Contato:** telefone e e-mail das partes só aparecem a partir de `ACEITA`
  (`contatoLiberado()`), e nunca em `RECUSADA` ou `CANCELADA`.
- **Valor às cegas:** para o cliente, `valorFinal` só aparece em `CONCLUIDA`; o
  prestador sempre vê o próprio valor (ver regra 3 da seção 2).
- **Ações disponíveis:** a resposta da API traz `acoesDisponiveis`, calculado em
  `SolicitacaoMapper.acoes`. O front-end só mostra os botões dessa lista e não recalcula
  a regra.
- **Avaliação:** só em `CONCLUIDA`, só pelo cliente, uma por solicitação
  (`UNIQUE (solicitacao_id)` em `avaliacao`). A nota média do prestador é recalculada na
  mesma transação.

---

## 4. Arquitetura

Monolito Spring Boot em camadas, organizado por funcionalidade. O front-end é estático
(HTML, CSS e JavaScript puro) e é servido pela própria aplicação, então o deploy gera um
único artefato.

```
Navegador ── js/api.js ──HTTP/JSON──> Controller ──> Service ──> Repository (JPA) ──> Banco
                                       (só HTTP)    (regra +      (Spring Data)     (H2 / Postgres)
                                                    transação)
```

| Camada | Responsabilidade | Não faz |
|---|---|---|
| Controller | Recebe DTO, valida (`@Valid`), identifica o usuário, chama o service, devolve DTO | Regra de negócio |
| Service | Regra de negócio, autorização por objeto, `@Transactional` | Lidar com HTTP |
| Entidade | Invariantes do próprio objeto (transições, cálculo) | Acessar repositório |
| Repository | Consultas Spring Data | Regra de negócio |

**Stack:** Java 21 · Spring Boot 3.3.5 · Spring Web · Spring Data JPA · Bean Validation ·
`spring-security-crypto` (só o BCrypt, sem o Spring Security completo) · Flyway ·
H2 (dev) · PostgreSQL 18 (prod) · Maven · JUnit 5 + AssertJ.

**Autenticação:** sessão HTTP com cookie `HttpOnly` e `SameSite=Lax`, que expira em
2 horas. O cookie é `Secure` apenas no perfil `prod`. O `AutenticacaoInterceptor` protege
`/api/v1/**`, exceto login, sessão, saída, cadastro, catálogo, busca e perfil público
(`WebConfig`). Senhas usam BCrypt com força 10. Depois de 5 tentativas erradas, a conta
fica bloqueada por 15 minutos. O login responde com a mesma mensagem para e-mail
inexistente e para senha errada.

**Erros:** `TratadorDeErros` converte exceções em `ApiError`
(`{ "codigo", "mensagem", "campos" }`), sem stack trace.

| Exceção | Uso |
|---|---|
| `RegraDeNegocioException` | Violação de regra; traz um código (`MOTIVO_OBRIGATORIO`, `FORA_DE_ATUACAO`, `COBRANCA_DUPLICADA`…) |
| `TransicaoInvalidaException` | Transição fora do mapa |
| `NaoEncontradoException` | Recurso inexistente |
| `NaoAutorizadoException` | Usuário não é parte do registro |

**Tarefas agendadas** (`TarefasAgendadas`):

| Tarefa | Quando | O que faz |
|---|---|---|
| `confirmarPorDecursoDePrazo` | de hora em hora (`0 0 * * * *`) | Confirma solicitações em `AGUARDANDO_CONFIRMACAO` com prazo vencido e sem contestação |
| `anonimizarContasEmExclusao` | 03:30 diariamente | Anonimiza contas em `EM_EXCLUSAO` há mais de 30 dias |

Essas tarefas só rodam com a aplicação no ar. Hospedagem que hiberna por inatividade
atrasa a confirmação automática e a anonimização.

**LGPD:** CPF é pedido só do prestador. A exclusão de conta anonimiza os dados em vez de
apagar, para manter o histórico dos serviços da outra parte. O aceite dos termos é
registrado com versão.

### API (`/api/v1`)

| Método | Rota | Acesso |
|---|---|---|
| `POST` | `/auth/login`, `/auth/sair` · `GET /auth/sessao` | público |
| `POST` | `/cadastro/cliente`, `/cadastro/prestador` | público |
| `GET` | `/catalogo` | público |
| `GET` | `/prestadores/busca`, `/prestadores/{id}/publico` | público |
| `POST` | `/solicitacoes` | cliente |
| `GET` | `/solicitacoes`, `/solicitacoes/{id}` | partes |
| `POST` | `/solicitacoes/{id}/aceitar` · `recusar` · `iniciar` · `concluir` | prestador |
| `POST` | `/solicitacoes/{id}/confirmar` · `contestar` · `avaliar` | cliente |
| `POST` | `/solicitacoes/{id}/cancelar` | partes |
| `GET` | `/solicitacoes/extrato` | prestador |
| `GET` `PUT` | `/conta` · `POST /conta/senha`, `/conta/desativar`, `/conta/excluir` | logado |

As mudanças de estado são sub-recursos com nome (`/aceitar`, `/confirmar`…), e não um
`PUT` genérico de status.

---

## 5. Organização dos pacotes

```
src/main/java/br/com/obrix/
├─ ObrixApplication.java
├─ config/        SegurancaConfig (BCrypt), WebConfig (interceptor),
│                 DadosDeDemonstracao (perfis dev e demo, idempotente)
├─ shared/        ApiError, exceções de domínio, TratadorDeErros
├─ seguranca/     AuthController, AutenticacaoService, AutenticacaoInterceptor,
│                 SessaoUsuario, UsuarioLogado
├─ usuario/       Usuario (abstrata), Cliente, Prestador, Endereco (embutido),
│                 StatusConta, TokenEmail, UsuarioService,
│                 Cadastro/Conta/PrestadorController, dto/Dtos
├─ catalogo/      CategoriaServico, RegiaoAtuacao, CatalogoController, dto/
├─ solicitacao/   Solicitacao (máquina de estados), StatusSolicitacao,
│                 HistoricoStatus, AutorTransicao, SolicitacaoService,
│                 SolicitacaoController, SolicitacaoMapper, TarefasAgendadas, dto/
├─ cobranca/      Cobranca (cálculo da comissão), CobrancaRepository
├─ avaliacao/     Avaliacao, AvaliacaoRepository
└─ notificacao/   Notificacao, NotificacaoService, NotificacaoRepository

src/main/resources/
├─ application.yml        comum (porta, sessão, comissão, prazo)
├─ application-dev.yml    H2 em memória + console /h2
├─ application-prod.yml   PostgreSQL por variável de ambiente, cookie Secure
├─ db/migration/          V1__schema.sql, V2__seed_catalogo.sql,
│                         V3__valor_confirmado_cliente.sql
└─ static/                front-end (html, css/obrix.css, js/)

src/test/java/br/com/obrix/
├─ solicitacao/   testes de unidade da entidade (sem Spring)
├─ api/           testes de integração da API (Spring + H2)
└─ shared/        TratadorDeErrosTest
src/test/js/      testes do front-end (node:test)
```

Regra de organização: cada funcionalidade tem seu pacote, com entidade, repositório,
serviço, controller e `dto/` juntos. O pacote `shared/` guarda apenas o que é
transversal.

**Front-end** (`static/`):

| Arquivo | Papel |
|---|---|
| `css/obrix.css` | Folha única, que começa pelos tokens de design |
| `js/api.js` | Único ponto de acesso a dados. Detecta se a API responde e, se não, usa `demo.js` |
| `js/demo.js` | Mesma interface do `api.js`, resolvida no navegador, para rodar sem servidor |
| `js/ui.js` | Partes comuns: cabeçalho, selo de status, modal, avisos, formatação |
| `js/pagina-*.js` | Um arquivo por tela |

Decisões de interface: o status nunca é indicado só pela cor; o telefone não aparece
antes do aceite; prestadores sem avaliação ficam em bloco separado; "Contestar" tem peso
visual menor que "Confirmar", mas fica sempre visível.

---

## 6. Convenções de código

**Idioma do domínio.** Classes, métodos, variáveis, tabelas, colunas, endpoints e
códigos de erro em português: `Solicitacao`, `registrarConclusao`, `valor_final`,
`/solicitacoes/{id}/contestar`, `MOTIVO_OBRIGATORIO`. Termos técnicos do framework
continuam em inglês (`Controller`, `Service`, `Repository`, `Request`, `Response`).

**Sem acento em identificadores.** Nada de `ç`, `ã` ou `é` em nomes de classe, método,
variável, coluna, enum ou rota: `Avaliacao`, `CONCLUIDA`, `regiao_atuacao`. Mensagens ao
usuário podem ter acento. No back-end hoje elas também saem sem acento, por consistência.

**Dinheiro em `BigDecimal`.** Nunca `double` ou `float`. Use escala 2 e
`RoundingMode.HALF_UP`. No banco, `DECIMAL(12,2)` para valores e `DECIMAL(5,2)` para
percentuais. Crie valores a partir de `String` (`new BigDecimal("280.00")`), não de
`double`.

**DTOs na API.** Entidades JPA não saem nem entram pelos controllers. Requests e
responses são `record`s agrupados por módulo (`usuario/dto/Dtos`,
`solicitacao/dto/SolicitacaoDtos`, `catalogo/dto/CatalogoDtos`), com validação por
anotação (`@NotBlank`, `@Size`, `@Email`, `@Pattern`, `@DecimalMin`…). A conversão fica em mappers
(`SolicitacaoMapper`).

**Banco versionado com Flyway.** `ddl-auto: validate`: o Hibernate só confere o esquema
e não altera nada. Toda mudança de estrutura entra como uma nova migration
`V{n}__descricao.sql` em `src/main/resources/db/migration`. **Uma migration já publicada
nunca é editada.** O SQL precisa rodar em H2 (modo PostgreSQL) e em PostgreSQL 18.

**Regras no lugar certo.**
- Transições e invariantes ficam na entidade (`Solicitacao.mover`, `exigirMotivo`).
- Orquestração, autorização e efeitos colaterais (cobrança, notificação) ficam no
  service.
- Restrições críticas existem também no banco (`UNIQUE`, `NOT NULL`), não só no Java.
- Status é sempre `enum` com `@Enumerated(EnumType.STRING)`.

**Outras convenções em uso.**
- Injeção por construtor e campos `final`.
- Construtor `protected` vazio nas entidades, só para o JPA.
- Configuração que muda por ambiente vai para `application*.yml` ou variável de ambiente.
  Credenciais nunca entram no código.
- Logs com SLF4J, sem dados pessoais.
- Comentários explicam o porquê da regra, não o que a linha faz.

---

## 7. Como rodar

Requisitos: **JDK 21** e **Maven**. O projeto não tem Maven Wrapper versionado.

### Desenvolvimento (padrão, perfil `dev`)

```bash
mvn spring-boot:run
```

- Aplicação: <http://localhost:8080>
- Banco H2 em memória, recriado a cada subida. Flyway aplica o esquema e o catálogo.
- `DadosDeDemonstracao` cria um cliente, cinco prestadores e solicitações em vários
  status:

| Perfil | E-mail | Senha |
|---|---|---|
| Cliente | `ana@exemplo.com` | `obrix123` |
| Prestador | `carlos@exemplo.com` | `obrix123` |

- Console do H2: <http://localhost:8080/h2>
  - JDBC URL: `jdbc:h2:mem:obrix;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`
  - Usuário `sa`, senha vazia

### Produção (perfil `prod`, PostgreSQL)

```bash
docker compose up -d db
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Variáveis de ambiente: `OBRIX_DB_URL`, `OBRIX_DB_USER` e `OBRIX_DB_PASSWORD`. Os valores
padrão batem com o `docker-compose.yml` e servem só para uso local. No perfil `prod` o
cookie de sessão é `Secure`, então é preciso HTTPS.

### Só o front-end

Abra `src/main/resources/static/index.html` direto no navegador. O `api.js` não encontra
a API e passa a usar o `demo.js`.

### Pacote

```bash
mvn clean package
java -jar target/obrix-0.1.0.jar
```

---

## 8. Como testar

```bash
mvn test
```

```bash
node --test src/test/js/*.test.js   # front-end, sem dependências
```

São **159 testes no back-end** (JUnit 5 + AssertJ), entre unidade e integração, e
**24 no front-end**. O `mvn test` também gera o relatório de cobertura do JaCoCo em
`target/site/jacoco/index.html`. Na última medição (25/11/2026), a cobertura do back-end
foi de 88,3% das linhas (864/979), 85,4% das instruções e 79,7% dos ramos; o pacote
`solicitacao` ficou em 98,8% das linhas.

**Unidade** (`solicitacao/`, sem contexto Spring e sem banco):

| Classe | Casos | O que cobre |
|---|---|---|
| `CicloDaSolicitacaoTest` | 25 | Máquina de estados (caminho normal, histórico com autor e motivo, transições proibidas, estado final imóvel, cancelamento, motivo obrigatório, valor positivo); contestação; confirmação automática e o prazo de 7 dias; contato só após o aceite; cálculo da comissão (R$ 280,00 → R$ 28,00, `HALF_UP`, escala 2, percentual gravado) |
| `ConfirmacaoComValorTest` | 6 | `informarValorPago`: valor igual libera a confirmação; 280 = 280.00; valor diferente contesta com autor `CLIENTE` e motivo automático; o prazo automático deixa de correr; valor inválido e status errado |
| `ConfirmacaoCegaTest` | 1 | O motivo da divergência traz o valor do cliente e não o do prestador |

**Integração** (`api/`, `@SpringBootTest` + MockMvc + H2 com as migrations do Flyway;
o login passa por `POST /auth/login` de verdade):

| Classe | Casos | O que cobre |
|---|---|---|
| `CicloApiTest` | 24 | Ciclo completo pela API, cobrança gerada só na confirmação, cobrança única, avaliação, extrato e confirmação automática pela tarefa agendada |
| `ConfirmacaoComValorApiTest` | 10 | Valor igual: `CONCLUIDA` com cobrança; 280 = 280.00; valor diferente: `CONTESTADA` sem cobrança, motivo no histórico e prestador notificado; revisão e nova confirmação; sem `valorPago` ou ≤ 0: 400; confirmação automática com `valorConfirmadoCliente` nulo |
| `ConfirmacaoCegaApiTest` | 6 | Confirmação às cegas: cliente não vê `valorFinal` em `AGUARDANDO_CONFIRMACAO` (detalhe, lista e resposta da ação); prestador vê; motivo da divergência sem o valor do prestador; depois de `CONCLUIDA` o cliente vê `valorFinal` e `valorConfirmadoCliente` |
| `ErrosApiTest` | 35 | 401 sem login, 403 por perfil e por dono, 404, 422 `TRANSICAO_INVALIDA`, 400 de validação e o formato do `ApiError` |
| `AutenticacaoApiTest` | 17 | Cadastro, login, sessão e bloqueio após tentativas erradas |
| `ContaApiTest` | 16 | Minha conta: perfil, senha, desativar e excluir |
| `PublicoApiTest` | 8 | Catálogo, busca e perfil público sem login |
| `CancelamentoPeloPrestadorApiTest` | 3 | Prestador não cancela antes de aceitar (usa recusar) |
| `PaginacaoApiTest` | 3 | Formato de página da busca, da lista e do extrato |
| `RegrasNoCatalogoApiTest` | 2 | Comissão e prazo expostos no catálogo |
| `DesativarEncerraSessaoApiTest` | 1 | Desativar a conta encerra a sessão |
| `TratadorDeErrosTest` (`shared/`) | 2 | Conflitos de banco e de concorrência viram 409 |

**Front-end** (`src/test/js`, `node:test`, sem dependências): os scripts reais de
`static/js` rodam num contexto isolado do Node (`ambiente.js`), com `fetch` simulado.
Cobrem a escolha entre API e demonstração, o formato de página, o "carregar mais", o
cálculo da comissão, a conta, a regra do valor pago e a confirmação às cegas no
`demo.js`.

Para testar manualmente, suba em `dev`, entre como cliente e como prestador em
navegadores diferentes (ou em janela anônima) e confira as tabelas `cobranca` e
`historico_status` no console do H2.

Ao criar testes novos, use o mesmo padrão: `@Nested` por tema e `@DisplayName` em
português descrevendo a regra.

---

## 9. O que já está pronto

**Back-end**
- Cadastro de cliente e de prestador, com categorias, regiões e aceite dos termos.
- Login por sessão, logout, consulta de sessão, BCrypt e bloqueio após tentativas
  erradas.
- Catálogo de categorias e regiões (carregado por migration).
- Busca pública de prestadores por categoria e região, e perfil público com avaliações.
- Criação de solicitação, com validação de categoria e região atendidas e de prestador
  disponível.
- Máquina de estados completa, com os 8 status, histórico e autorização por objeto.
- Geração da cobrança na confirmação, com unicidade garantida no banco.
- Confirmação com o valor pago pelo cliente: valor diferente do registrado pelo prestador
  vira contestação automática, sem cobrança (contra a subdeclaração).
- Confirmação às cegas: o cliente só vê o valor do prestador depois de `CONCLUIDA`, e o
  motivo da divergência não revela esse valor.
- Confirmação automática em 7 dias, desligada por contestação.
- Avaliação após a conclusão, com recálculo da nota média.
- Extrato de comissões do prestador.
- Minha conta: ver e editar perfil, alterar senha, desativar, solicitar exclusão.
- Anonimização agendada das contas em exclusão (30 dias).
- Notificações gravadas no banco a cada evento do ciclo.
- Tratamento padronizado de erros e controle de concorrência otimista.
- Perfis `dev` (H2 com dados de demonstração) e `prod` (PostgreSQL), e `docker-compose` do
  PostgreSQL.

**Front-end**
- Telas: início, resultados da busca, perfil do prestador, solicitar, entrar, criar
  conta, painel do cliente, painel do prestador, minha conta.
- Integrado à API real, com modo de demonstração sem servidor.
- Botões de ação guiados por `acoesDisponiveis`.

**Testes**
- Testes de unidade da máquina de estados, do cálculo da comissão e da confirmação com
  valor pago.
- Testes de integração da API com Spring e H2 (ciclo, cobrança, erros, autenticação,
  conta, busca pública, paginação).
- Testes do front-end com `node:test`.

---

## 10. O que falta

### Essencial

| Item | Situação no código |
|---|---|
| **Saída para solicitação parada em `EM_EXECUCAO`** | `EM_EXECUCAO` só vai para `AGUARDANDO_CONFIRMACAO`. Se o prestador abandona o serviço, não há como sair. Proposta: permitir `CANCELADA` a partir de `EM_EXECUCAO`, com motivo, e testar. |
| **Contestação sem desfecho** | Se o prestador não registra nova conclusão, a solicitação fica em `CONTESTADA` para sempre. Hoje o efeito é seguro (não cobra), mas falta definir uma saída, como cancelamento com motivo ou um prazo. |
| **Testes no PostgreSQL** | Os testes de integração rodam em H2 (modo PostgreSQL). Falta rodar as migrations e a constraint `UNIQUE` da cobrança contra um PostgreSQL real (por exemplo, com Testcontainers). |
| **Endpoint de notificações** | `NotificacaoService` já tem `doUsuario`, `naoLidas` e `marcarComoLida`, mas nenhum controller as expõe. As notificações são gravadas e o usuário não consegue vê-las. |
| **Exportação dos dados pessoais (LGPD)** | Não existe endpoint de exportação. O README cita essa função, mas ela não está implementada. |
| **Verificação de e-mail e recuperação de senha** | A entidade `TokenEmail` e a tabela `token_email` existem, mas nada as usa. Depende de configurar um provedor SMTP. |

### Desejável

- `Notificacao` passar a referenciar a solicitação de origem, para o front-end levar
  direto ao cartão correspondente.
- Versionar o Maven Wrapper (`mvnw`) para não depender de Maven instalado.
- Pipeline de CI rodando `mvn test` a cada push.
- Trava para as tarefas agendadas (por exemplo, ShedLock) caso a aplicação passe a rodar
  em mais de uma instância.
- Rever a proteção CSRF dos `POST` autenticados por cookie. `SameSite=Lax` reduz o risco,
  mas não substitui um token.
- Manter `demo.js` alinhado às regras do back-end, ou removê-lo do pacote de produção,
  porque ele duplica a máquina de estados.
- Testes automatizados do front-end (fluxos principais no navegador).
- Atualizar o README nos pontos em que ele adianta funções ainda não implementadas.
