# Auditoria de acessibilidade (WCAG 2.2, nível AA)

Revisão do HTML, do CSS e do JavaScript reais do front-end
(`src/main/resources/static/`), feita na leitura do código e nas telas do sistema
rodando no perfil `dev` (capturas em [prototipos/alta-fidelidade/](prototipos/alta-fidelidade/)).

**Este documento não corrige nada.** As correções estão listadas no fim e entram pelo
**CR-011** (Acessibilidade: auditoria WCAG e correções).

Limites desta auditoria: não houve teste com leitor de tela (NVDA, TalkBack) nem com
usuários com deficiência, nem varredura automatizada (axe, Lighthouse). Esses testes
fazem parte do CR-011.

Legenda: ✅ atende · ⚠️ atende parcialmente · ❌ não atende.

## Resumo

| # | Tema | Critérios WCAG | Resultado |
|---|---|---|---|
| 1 | Idioma da página | 3.1.1 | ✅ |
| 2 | Títulos de página e cabeçalhos | 2.4.2, 2.4.6 | ✅ |
| 3 | Rótulos de formulário | 1.3.1, 3.3.2, 4.1.2 | ⚠️ |
| 4 | Textos alternativos | 1.1.1 | ✅ |
| 5 | Navegação por teclado | 2.1.1, 2.1.2 | ✅ |
| 6 | Foco visível | 2.4.7, 2.4.11 | ⚠️ |
| 7 | Pular para o conteúdo | 2.4.1 | ⚠️ |
| 8 | Contraste de texto | 1.4.3 | ❌ |
| 9 | Contraste de componentes | 1.4.11 | ❌ |
| 10 | Status não dependente só de cor | 1.4.1 | ✅ |
| 11 | Mensagens de status (aria-live) | 4.1.3, 3.3.1 | ⚠️ |
| 12 | Modais | 2.4.3, 4.1.2 | ⚠️ |
| 13 | Movimento reduzido | 2.3.3 (AAA), 2.2.2 | ✅ |
| 14 | Zoom de 200% e reflow | 1.4.4, 1.4.10 | ✅ |
| 15 | Tamanho dos alvos | 2.5.8 | ✅ |
| 16 | Finalidade dos campos e dos links | 1.3.5, 2.4.4 | ⚠️ |
| 17 | Prevenção de erros e entrada redundante | 3.3.4, 3.3.7, 3.3.8 | ✅ |

Total: 9 atendem, 6 atendem parcialmente, 2 não atendem.

---

## 1. Idioma da página: ✅ atende (3.1.1)

As nove páginas declaram `<html lang="pt-BR">`.

*Observação:* a falta de acentuação em quase todos os textos (ver H-05 em
[heuristicas-nielsen.md](heuristicas-nielsen.md)) não viola o critério, mas faz o leitor
de tela pronunciar palavras erradas ("solicitacoes", "voce"). A correção dos acentos
melhora a acessibilidade.

## 2. Títulos de página e cabeçalhos: ✅ atende (2.4.2, 2.4.6)

Cada página tem um `<title>` próprio ("Entrar — Obrix", "Minhas solicitacoes — Obrix",
"Nova solicitacao — Obrix"…) e um único `<h1>`. Os modais usam `<h2 id="modal-titulo">`.

## 3. Rótulos de formulário: ⚠️ atende parcialmente (1.3.1, 3.3.2, 4.1.2)

**Atende:** todos os campos das páginas HTML têm `<label for>` associado (verificado nas
9 páginas); os grupos de endereço, categorias e regiões usam `<fieldset>` com `<legend>`;
os campos dos modais de motivo e de valor também têm `<label for>`.

**Falhas:**

- No modal de avaliação, "Sua nota" é um `<label>` sem `for`, e os cinco botões de rádio
  não estão num `<fieldset>` com `<legend>`: o leitor de tela anuncia "1", "2"… sem dizer
  que é a nota (`abrirAvaliacao`, em `pagina-painel-cliente.js`).
- Campos obrigatórios não são indicados: os formulários usam `novalidate`, e só os
  motivos trazem "— obrigatorio" no rótulo.
- As abas dos painéis têm `role="tablist"` e `role="tab"`, mas não há `role="tabpanel"`,
  `aria-controls`, nem navegação pelas setas (`painel-cliente.html`, `painel-prestador.html`).
- O indicador de etapas é um `<ol>` com `<span class="traco">` como filho direto, o que é
  HTML inválido; leitores de tela podem anunciar a lista de forma errada (`solicitar.html`).

## 4. Textos alternativos: ✅ atende (1.1.1)

Não há `<img>`. Os ícones SVG decorativos têm `aria-hidden="true"` (marca no cabeçalho,
ícones da página inicial). As estrelas têm `aria-hidden="true"`, e a nota aparece em
texto ao lado ("5,0 (1 avaliacao)" ou "Ainda sem avaliacao", em `UI.estrelas`). O link
da marca tem o texto "OBRIX".

## 5. Navegação por teclado: ✅ atende (2.1.1, 2.1.2)

Todos os controles são elementos nativos (`<a>`, `<button>`, `<select>`, `<input>`),
alcançáveis por Tab e acionáveis por Enter ou Espaço. As abas e as ações dos cartões são
`<button>`. Não há armadilha de teclado: o modal fecha com Esc (`UI.abrirModal`).

## 6. Foco visível: ⚠️ atende parcialmente (2.4.7, 2.4.11)

**Atende:** `:focus-visible { outline: 3px solid var(--info) }` em `obrix.css`. O azul
`#1d4ed8` tem contraste de 6,70:1 sobre branco. O cabeçalho não é fixo, então não cobre
o elemento focado.

**Falha:** nos campos de formulário, a regra `.campo input:focus` (mais específica)
troca esse contorno por `outline: 2px solid var(--verde-claro)`. O `#e6f2f1` sobre
branco tem **1,15:1**, ou seja, é praticamente invisível. O foco fica indicado só pela
borda de 1 px que muda para verde (5,90:1). É perceptível, mas fraco.

## 7. Pular para o conteúdo: ⚠️ atende parcialmente (2.4.1)

Não há link "Pular para o conteúdo". Quem usa teclado passa por até 5 itens do
cabeçalho em toda página. O critério é parcialmente atendido porque as páginas têm
`<main>` e `<nav aria-label="Navegacao principal">`, regiões que leitores de tela
permitem pular.

## 8. Contraste de texto: ❌ não atende (1.4.3)

Razões de contraste calculadas pela fórmula da WCAG (luminância relativa), com as cores
de `obrix.css`. O mínimo é 4,5:1 para texto normal e 3:1 para texto grande (a partir de
24 px, ou 18,66 px em negrito).

| Par de cores | Uso | Razão | Resultado |
|---|---|---|---|
| `--texto` #16233f / branco | Texto principal | 15,60:1 | ✅ |
| `--texto` #16233f / `--fundo` #f6f8fa | Texto sobre o fundo | 14,65:1 | ✅ |
| `--texto-2` #51607a / branco | Texto de apoio | 6,36:1 | ✅ |
| `--texto-2` #51607a / `--fundo` | Texto de apoio sobre o fundo | 5,97:1 | ✅ |
| **`--texto-3` #7a8699 / branco** | **Rótulos dos campos (12 px), ajudas (13 px), "Valor: você informa ao confirmar" (11,5 px), datas dos cartões, rodapé, "×" do modal** | **3,69:1** | ❌ |
| **`--texto-3` #7a8699 / `--fundo`** | **Etapas do fluxo, rodapé** | **3,46:1** | ❌ |
| `--verde` #15706b / branco | Links | 5,90:1 | ✅ |
| Branco / `--verde` #15706b | Botão primário | 5,90:1 | ✅ |
| Branco / `--verde-escuro` #0f5651 | Botão primário (hover), aviso de sucesso | 8,50:1 | ✅ |
| Branco / `--navy` #16233f | Botão escuro, aviso, cabeçalho do extrato | 15,60:1 | ✅ |
| `--erro` #b42318 / branco | Botão de perigo | 6,57:1 | ✅ |
| `--info` #1d4ed8 / `--info-fundo` #e8eefc | Selos "Solicitada" e "Em execucao" | 5,77:1 | ✅ |
| `--verde-escuro` / `--verde-claro` #e6f2f1 | Selos "Aceita" e "Concluida", aviso ok | 7,42:1 | ✅ |
| `--erro` / `--erro-fundo` #fdeceb | Selos "Recusada" e "Contestada", faixa de erro | 5,75:1 | ✅ |
| `--alerta` #b45309 / `--alerta-fundo` #fef3e2 | Selo "Aguardando confirmacao" | 4,58:1 | ✅ (no limite) |
| `--texto-2` / #eef0f3 | Selo "Cancelada" | 5,57:1 | ✅ |
| #1b3a8c / `--info-fundo` | Aviso informativo | 8,90:1 | ✅ |
| #8a4409 / `--alerta-fundo` | Aviso de alerta, faixa de demonstração | 6,57:1 | ✅ |
| Branco / `--alerta` | Contador da aba | 5,02:1 | ✅ |
| #c6d0e2 / `--navy` | Texto de apoio do topo da página inicial | 10,04:1 | ✅ |
| #1c4f4c / `--verde-claro` | Destaque "Diferente dos aplicativos…" | 8,07:1 | ✅ |
| `--texto-2` / `--linha` #dde3ea | Número das etapas | 4,92:1 | ✅ |

**Falha:** `--texto-3` é usado em textos pequenos com 3,46:1 a 3,69:1. O caso mais
grave é o rótulo "Valor: você informa ao confirmar", em 11,5 px e maiúsculas: é a
instrução central da confirmação às cegas.

## 9. Contraste de componentes: ❌ não atende (1.4.11)

O mínimo é 3:1 para a borda ou o contorno que identifica um componente.

| Elemento | Cores | Razão | Resultado |
|---|---|---|---|
| Borda dos campos (`--linha-forte` #c3ccd8 / branco) | campo branco sobre cartão branco: a borda é o único limite | 1,62:1 | ❌ |
| Borda do botão secundário e das abas não selecionadas | #c3ccd8 / branco | 1,62:1 | ⚠️ (o texto identifica o botão) |
| Borda do botão de perigo (#f0b4b0 / branco) | | 1,77:1 | ⚠️ (o texto vermelho identifica) |
| Contorno de foco geral (`--info` / branco) | | 6,70:1 | ✅ |
| Contorno de foco dos campos (`--verde-claro` / branco) | | 1,15:1 | ❌ (ver item 6) |
| Estrela cheia (#d9a406 / branco) | | 2,27:1 | ✅ (a nota está em texto ao lado) |

## 10. Status não dependente só de cor: ✅ atende (1.4.1)

O selo sempre escreve o status ("AGUARDANDO CONFIRMACAO"), além da cor e do ponto
(`UI.selo`; comentário "O status nunca e comunicado so pela cor" em `js/ui.js` e em
`obrix.css`). A aba selecionada muda de cor, de peso da fonte e de `aria-selected`. O
cartão em destaque tem, além da borda laranja, o texto "Responda ate …". Os links no
meio do texto mantêm o sublinhado.

## 11. Mensagens de status (aria-live): ⚠️ atende parcialmente (4.1.3, 3.3.1)

**Atende:** a área de avisos flutuantes é criada com `role="status"` e
`aria-live="polite"` (`UI.toast`), então sucesso e erro das ações são anunciados. A
faixa de falta de conexão usa `role="alert"`.

**Falhas:**

- Os erros de campo (`UI.marcarErro`) inserem um `<span class="erro">` sem
  `aria-describedby` no campo, sem `aria-invalid` e fora de região viva: o leitor de
  tela não anuncia o erro nem o associa ao campo.
- Os erros também saem como `polite`, quando deveriam interromper (`role="alert"`).
- Os avisos somem em 4,2 segundos: quem lê devagar ou usa ampliação pode não ter tempo
  de lê-los.
- "Carregando..." nas listas não fica numa região viva.

## 12. Modais: ⚠️ atende parcialmente (2.4.3, 4.1.2)

**Atende:** `role="dialog"`, `aria-modal="true"` e `aria-labelledby="modal-titulo"`; o
foco vai para o primeiro campo (ou para o botão de confirmar) ao abrir e volta para o
elemento de origem ao fechar; Esc fecha; o botão "×" tem `aria-label="Fechar"`; a
página de fundo não rola.

**Falha:** o foco não fica preso no modal. Tab a partir do último botão vai para a
página de fundo, que continua alcançável por teclado e leitor de tela (sem `inert`).

## 13. Movimento reduzido: ✅ atende (2.3.3 AAA, 2.2.2)

Não há animações, carrosséis nem conteúdo que se mova sozinho. As únicas transições são
mudanças de cor em 0,15 s nos botões (`transition: background-color .15s…`). Não existe
regra `@media (prefers-reduced-motion)`, mas também não há o que reduzir. Se forem
criadas animações, elas precisam dessa regra.

## 14. Zoom de 200% e reflow: ✅ atende (1.4.4, 1.4.10)

O viewport não bloqueia o zoom (`width=device-width, initial-scale=1`, sem
`user-scalable=no`). O layout é fluido, com pontos de quebra em 560, 640, 680, 760 e
880 px. O cabeçalho quebra em duas linhas no celular sem esconder itens. A captura
`12-painel-cliente-683px-zoom-200.png` simula uma tela de 1366 px a 200%: não há rolagem
horizontal e nenhum conteúdo é cortado. A tabela do extrato rola na horizontal dentro
do próprio contêiner (`.tabela-envolvente`), exceção que o critério admite para tabelas.

*Pendente:* testar em 320 px de largura (400%), como pede o 1.4.10.

## 15. Tamanho dos alvos: ✅ atende (2.5.8)

`.btn { min-height: 44px }` e campos com `min-height: 44px`; o botão pequeno tem 38 px e
as abas cerca de 36 px, todos acima dos 24 px exigidos. Nas caixas de seleção, o rótulo
inteiro é clicável.

## 16. Finalidade dos campos e dos links: ⚠️ atende parcialmente (1.3.5, 2.4.4)

- `autocomplete` existe em e-mail, senha e nome, mas falta em telefone (`tel`), CEP
  (`postal-code`), rua (`address-line1`) e cidade (`address-level2`) no cadastro e na
  solicitação.
- Nos resultados, todos os cartões têm o link "Ver perfil", sem o nome do profissional
  no texto acessível. O contexto do cartão ajuda (2.4.4 aceita), mas a lista de links do
  leitor de tela mostra vários "Ver perfil" iguais.
- "Voltar aos resultados" usa `javascript:history.back()`: se o perfil foi aberto por um
  link direto, o destino é imprevisível.

## 17. Prevenção de erros e entrada redundante: ✅ atende (3.3.4, 3.3.7, 3.3.8)

- Ações com efeito financeiro ou definitivo pedem confirmação em modal, com o efeito
  descrito (confirmar, contestar, registrar conclusão, excluir conta).
- O endereço da conta já vem preenchido na solicitação, e cidade e bairro vêm da região
  (3.3.7).
- O login usa e-mail e senha com `autocomplete`, permite colar e não tem teste
  cognitivo (3.3.8).

---

## Correções necessárias (para o CR-011)

| ID | Correção | Critério | Onde | Prioridade |
|---|---|---|---|---|
| A-01 | Escurecer `--texto-3` para no mínimo 4,5:1 (por exemplo, #5f6b7f: 5,39:1 sobre branco e 5,06:1 sobre o fundo) ou usar `--texto-2` nos textos pequenos | 1.4.3 | `obrix.css` (token) | Alta |
| A-02 | Aumentar o rótulo "Valor: você informa ao confirmar" (tamanho, peso e contraste) | 1.4.3 | `painel-cliente.html` (`.valor-rotulo`) | Alta |
| A-03 | Borda dos campos com no mínimo 3:1 (por exemplo, #8792a6: 3,14:1) | 1.4.11 | `obrix.css` (`--linha-forte` ou um token novo para campos) | Alta |
| A-04 | Contorno de foco dos campos visível (3:1 ou mais), sem sobrescrever o `:focus-visible` | 2.4.7 | `obrix.css` (`.campo input:focus`) | Alta |
| A-05 | Prender o foco no modal e tornar o fundo `inert` enquanto ele estiver aberto | 2.4.3 | `js/ui.js` (`UI.abrirModal`) | Alta |
| A-06 | Erros de campo com `aria-invalid="true"` e `aria-describedby` apontando para a mensagem; foco no primeiro campo inválido | 3.3.1, 4.1.3 | `js/ui.js` (`UI.marcarErro`) | Alta |
| A-07 | Erros em `role="alert"`; avisos de erro só somem quando fechados | 4.1.3 | `js/ui.js` (`UI.toast`) | Média |
| A-08 | Link "Pular para o conteúdo" no início de cada página, visível ao receber foco, apontando para `<main id="conteudo">` | 2.4.1 | as 9 páginas e `obrix.css` | Média |
| A-09 | Nota da avaliação em `<fieldset>` com `<legend>Sua nota</legend>` | 1.3.1 | `pagina-painel-cliente.js` (`abrirAvaliacao`) | Média |
| A-10 | Abas completas: `aria-controls`, `role="tabpanel"` e setas; ou trocar por botões de filtro com `aria-pressed` | 4.1.2 | painéis | Média |
| A-11 | Indicar campos obrigatórios no rótulo e com `aria-required` | 3.3.2 | formulários | Média |
| A-12 | Corrigir o `<ol>` das etapas (traços dentro dos `<li>` ou por CSS) | 1.3.1 | `solicitar.html` | Baixa |
| A-13 | `autocomplete` em telefone, CEP e endereço | 1.3.5 | `criar-conta.html`, `solicitar.html`, `minha-conta.html` | Baixa |
| A-14 | "Ver perfil" com o nome do profissional no texto acessível (`aria-label="Ver perfil de Carlos Mendes"`) | 2.4.4 | `pagina-resultados.js` | Baixa |
| A-15 | Acentuar os textos da interface (pronúncia no leitor de tela) | 3.1 (boa prática) | todas as telas | Média |
| A-16 | Regra `@media (prefers-reduced-motion: reduce)` como proteção para animações futuras | 2.3.3 | `obrix.css` | Baixa |
| A-17 | Testar reflow em 320 px, varredura automatizada (axe ou Lighthouse) e leitor de tela (NVDA e TalkBack) depois das correções | 1.4.10, geral | — | Alta |
