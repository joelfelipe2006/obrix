# Obrix

Plataforma web de intermediação entre clientes e prestadores autônomos de serviços
residenciais — eletricista, encanador, pintor, diarista, jardineiro.

**O diferencial que define o sistema:** o profissional só paga comissão à plataforma
quando o cliente confirma que o serviço foi concluído. Buscar, receber solicitações e
trocar contato não custam nada. Essa regra não é um slogan: ela é a razão da máquina de
estados, da multiplicidade `0..1` entre solicitação e cobrança, e do ponto único do
código que cria uma cobrança.

Projeto Integrador do curso de Análise e Desenvolvimento de Sistemas — Faculdade SENAI
FATESG, 2026.

## Entregas

| Entrega | Repositório |
|---|---|
| Front-end — Parte 1 | [obrix-frontend](https://github.com/joelfelipe2006/obrix-frontend): as telas rodando sozinhas no navegador |
| Front-end — Parte 2 + Back-end — Parte 1 | este repositório |

Na Parte 1, o front-end rodava sem servidor: a camada `js/api.js` resolvia tudo no
navegador, com as regras replicadas em `js/demo.js`. Nesta entrega entra o back-end em
Spring Boot, e **as mesmas telas passam a falar com a API REST de verdade** — login com
sessão, dados gravados no banco e a comissão calculada no servidor. Nenhuma tela precisou
ser reescrita para isso: a troca acontece dentro do `api.js`, que detecta que existe uma
API respondendo e passa a usá-la.

---

## Como rodar

Requisito: **JDK 21**. O banco de desenvolvimento é em memória, então não é preciso
instalar nada além disso.

```bash
mvn spring-boot:run
```

Abra <http://localhost:8080>.

A aplicação sobe com dados de demonstração já criados — um cliente, cinco profissionais
e cinco serviços, um em cada estado do ciclo:

| Perfil       | E-mail               | Senha      |
|--------------|----------------------|------------|
| Cliente      | `ana@exemplo.com`    | `obrix123` |
| Profissional | `carlos@exemplo.com` | `obrix123` |

### Ver o banco de dados

Com a aplicação rodando, o console do H2 fica em <http://localhost:8080/h2>.

| Campo | Valor |
|---|---|
| JDBC URL | `jdbc:h2:mem:obrix;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE` |
| User Name | `sa` |
| Password | (vazio) |

Consultas úteis:

```sql
SELECT * FROM cobranca;
SELECT * FROM historico_status WHERE solicitacao_id = 1;
```

### Ver só o front-end, sem servidor

Abra `src/main/resources/static/index.html` direto no navegador. A camada de API detecta
a ausência do back-end e passa a resolver tudo no próprio navegador, com os mesmos dados
e as mesmas regras. Serve para ver as telas sem subir nada.

### Com PostgreSQL, como previsto para produção

```bash
docker compose up -d db
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Testes

```bash
mvn test
```

---

## Arquitetura

Monolito Spring Boot em camadas, organizado por funcionalidade. O front-end é estático e
servido pela própria aplicação, de modo que o deploy seja um artefato único.

```
Controller (só HTTP)  →  Service (regra + transação)  →  Repository (JPA)  →  Banco
```

```
src/main/java/br/com/obrix/
├─ config/        Configuração de segurança, web e dados de demonstração
├─ shared/        Exceções de domínio e tratador de erros da API
├─ seguranca/     Autenticação por sessão, BCrypt, interceptor
├─ usuario/       Usuario (abstrata), Cliente, Prestador, Endereco
├─ catalogo/      CategoriaServico, RegiaoAtuacao
├─ solicitacao/   Solicitacao, HistoricoStatus, máquina de estados, tarefas agendadas
├─ avaliacao/     Avaliacao
├─ cobranca/      Cobranca e cálculo da comissão
└─ notificacao/   Notificacao

src/main/resources/
├─ db/migration/  Flyway: esquema e carga inicial do catálogo
└─ static/        Front-end em HTML, CSS e JavaScript puro
```

### Front-end

Sem framework, por exigência da disciplina. A organização é a seguinte:

| Arquivo | Papel |
|---|---|
| `css/obrix.css` | Folha única. Começa por tokens de design: trocar a identidade visual é trocar a primeira seção. |
| `js/api.js` | Camada de acesso a dados. Toda comunicação passa por aqui; as telas não sabem a origem da resposta. |
| `js/demo.js` | Implementação da mesma interface resolvendo no navegador, para rodar sem back-end. |
| `js/ui.js` | O que se repete: cabeçalho, selo de status, modal, aviso, formatação. |
| `js/pagina-*.js` | Um arquivo por tela, só com o que é daquela tela. |

Decisões de interface que vale conhecer antes de ler o código:

- **O status nunca é comunicado só pela cor.** O texto vem junto, sempre — quem não
  distingue cores continua entendendo a tela.
- **Nenhum telefone aparece antes do aceite.** O contato é liberado para os dois lados
  no momento em que o profissional aceita, e sem custo.
- **Profissionais sem avaliação aparecem em bloco separado e sinalizado**, para que um
  cadastro novo não seja lido como mal avaliado.
- **Na tela de confirmação, "Contestar" tem peso visual menor que "Confirmar", mas nunca
  fica escondido.** Esconder a proteção do cliente esvaziaria o requisito que ela atende.
- **A lista de ações permitidas vem do back-end**, e não é recalculada no JavaScript:
  a regra de quem pode fazer o quê em cada status mora num lugar só.

---

## O ciclo da solicitação

Os oito estados são uma enumeração fechada. Com texto livre, `concluido`, `Concluída` e
`CONCLUIDO` seriam três valores diferentes para o sistema.

```
                        ┌──────────► RECUSADA
                        │
SOLICITADA ──► ACEITA ──┼──► EM_EXECUCAO ──► AGUARDANDO_CONFIRMACAO ──► CONCLUIDA
     │           │      │                            │     ▲                 │
     │           │      │                            ▼     │                 ▼
     └───────────┴──────┴──► CANCELADA          CONTESTADA ┘             Cobrança
```

As regras que o código garante:

1. **A cobrança nasce em um único ponto:** `SolicitacaoService.confirmar`. Nenhum outro
   evento cria uma `Cobranca`.
2. **Somente o cliente confirma.** O profissional registra que executou e informa o
   valor final; isso não fecha o ciclo financeiro.
3. **Se o cliente não responde em 7 dias**, o sistema confirma sozinho — e grava que a
   confirmação foi automática, com autor `SISTEMA` no histórico.
4. **Contestar desliga esse prazo para sempre** naquele serviço. O atributo derivado
   `foiContestada` é calculado a partir do histórico, não guardado.
5. **A única saída de uma contestação** é um novo registro de conclusão, que devolve o
   serviço para `AGUARDANDO_CONFIRMACAO`.
6. **Uma solicitação gera no máximo uma cobrança** — garantido por restrição de unicidade
   no banco, não apenas por verificação no código.
7. **Toda transição grava motivo e autor** no `HistoricoStatus`. Recusar, cancelar e
   contestar exigem motivo.
8. **Transição proibida falha explicitamente**, nunca deixa estado inconsistente.

---

## API

Prefixo `/api/v1`. As ações de estado são sub-recursos explícitos, e não um `PUT`
genérico: o nome do endpoint diz qual transição está sendo pedida.

| Método | Rota | O que faz |
|---|---|---|
| `POST` | `/auth/login` · `/auth/sair` | Sessão por cookie `HttpOnly` |
| `POST` | `/cadastro/cliente` · `/cadastro/prestador` | Cadastro |
| `GET` | `/catalogo` | Categorias e regiões (público) |
| `GET` | `/prestadores/busca` | Busca por categoria e região (público) |
| `GET` | `/prestadores/{id}/publico` | Perfil público (público) |
| `POST` | `/solicitacoes` | Cria a solicitação |
| `GET` | `/solicitacoes` | Painel do perfil que está logado |
| `POST` | `/solicitacoes/{id}/aceitar` | Transições do ciclo |
| | `/recusar` `/iniciar` `/concluir` | |
| | `/confirmar` `/contestar` `/cancelar` | |
| `POST` | `/solicitacoes/{id}/avaliar` | Avaliação, só após a confirmação |
| `GET` | `/solicitacoes/extrato` | Extrato de comissões do profissional |
| `GET` `PUT` | `/conta` | Minha conta |

Erros saem padronizados, sem stack trace:

```json
{ "codigo": "TRANSICAO_INVALIDA", "mensagem": "Nao e possivel passar de CONCLUIDA para ACEITA.", "campos": [] }
```

---

## Segurança

- Senhas com **BCrypt** (força 10). Nunca em texto legível.
- **Sessão por cookie** `HttpOnly` + `SameSite=Lax`, expirando em 2 horas. `Secure`
  apenas no perfil de produção — em `http://localhost` ele quebraria o login.
- **Autorização por objeto**: autenticar não basta. Cada ação verifica se o usuário é o
  cliente dono ou o profissional destinatário daquele registro.
- **Resposta genérica no login**: e-mail inexistente e senha errada dizem a mesma coisa.
- Bloqueio de 15 minutos após 5 tentativas malsucedidas.
- `ddl-auto=validate` — o esquema vem do Flyway, nunca do Hibernate.
- Credenciais por variável de ambiente, nunca no código.

## LGPD

- **CPF apenas do prestador**, por ser quem recebe cobrança. Do cliente não é pedido.
- **Exclusão é anonimização**, não apagamento: os dados pessoais saem em até 30 dias e o
  histórico dos serviços permanece. Apagar de verdade quebraria o registro da outra parte.
- Aceite dos termos registrado com data e versão.
- Exportação dos dados pessoais em arquivo estruturado.

---

## O que está fora do escopo

Declarado na abertura do projeto, e por isso ausente do código:

- Processamento real de pagamento. **A cobrança é simulada**: o sistema registra valor,
  percentual e data; não movimenta dinheiro.
- Aplicativo mobile nativo — a versão é web, responsiva de 320 a 1920 px.
- Chat em tempo real. O contato é liberado e a conversa acontece fora da plataforma.
- Verificação de antecedentes e garantia financeira.
- Perfil administrativo. O catálogo entra por carga inicial no banco.

## Pontos conhecidos para a próxima etapa

Anotados aqui porque são reais, e não para serem descobertos depois:

- Uma solicitação abandonada em `EM_EXECUCAO` não tem saída prevista. O caminho seria
  permitir cancelamento também nesse estado, com motivo.
- Uma contestação sem acordo fica suspensa indefinidamente — não há árbitro, porque não
  há perfil administrativo no escopo. O lado seguro é o adotado: na dúvida, não cobra.
- A `Notificacao` ainda não referencia a solicitação de origem.
- Verificação de e-mail e recuperação de senha dependem de um provedor SMTP, ainda não
  configurado.

---

## Equipe

- **Joel Felipe dos Santos** — gerência do projeto e back-end
- **Gabriel Ribeiro** — front-end e UX/UI
- **Luiz Leite** — testes e documentação

## Stack

Java 21 · Spring Boot 3.3 · Spring Data JPA · Flyway · H2 (desenvolvimento) ·
PostgreSQL 17 (produção) · HTML5, CSS3 e JavaScript sem framework · Maven
