# Avaliação heurística (Nielsen)

Avaliação do sistema implementado, tela a tela, com as 10 heurísticas de Nielsen. Para
cada heurística: como o Obrix a aplica, com evidência no código ou na tela, e os
problemas encontrados, com severidade e recomendação.

## Método

- **Objeto:** o front-end real (`src/main/resources/static/`), executado no perfil `dev`
  com os dados de demonstração, como cliente (Ana) e como prestador (Carlos). As capturas
  estão em [prototipos/alta-fidelidade/](prototipos/alta-fidelidade/).
- **Avaliadores:** a avaliação foi feita uma vez, com base no código e nas telas. Nielsen
  recomenda de 3 a 5 avaliadores independentes; os demais membros da equipe devem
  revisar esta lista antes da consolidação.
- **Escala de severidade de Nielsen:**

| Nota | Significado |
|---|---|
| 0 | Não é um problema de usabilidade |
| 1 | Problema cosmético: corrigir se houver tempo |
| 2 | Problema menor: baixa prioridade |
| 3 | Problema maior: importante corrigir, alta prioridade |
| 4 | Catástrofe: corrigir antes de lançar |

Os problemas têm um código (`H-nn`) para serem citados no CR de correção.

---

## H1. Visibilidade do status do sistema

**Como o Obrix aplica**

- Todo cartão tem um selo com o status escrito (`UI.selo` em `js/ui.js`), em cor e em
  texto: "AGUARDANDO CONFIRMACAO", "CONTESTADA" etc.
- A aba "Aguardando voce" mostra um contador (`atualizarContadorDaAba` em
  `pagina-painel-cliente.js`), e o cartão que espera o cliente ganha borda de destaque e
  o prazo "Responda ate …".
- Botões mostram o processamento: "Enviando...", "Entrando...", "Carregando..."
  (`UI.ocupado`).
- Sem conexão, uma faixa fixa avisa "Sem conexao com o servidor." (`UI.montarFaixaSemConexao`).
- O prestador vê o prazo do cliente: "Cliente tem ate … para confirmar".

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-01 | Ninguém é avisado dos eventos do ciclo. As notificações são gravadas no banco, mas nenhuma tela as mostra: o prestador só descobre uma nova solicitação, e o cliente só descobre um aceite ou uma conclusão registrada, se abrirem o painel. | `NotificacaoService` sem controller; nenhuma tela lê notificações | 3 | Expor as notificações (endpoint e indicador no cabeçalho, já previsto no CSS como `.sino`) |
| H-02 | O resultado das ações aparece só num aviso flutuante que some em 4,2 segundos. Quem desvia o olhar perde a confirmação ou o erro. | `setTimeout(… caixa.remove(), 4200)` em `UI.toast` | 2 | Manter os erros até serem fechados; para sucesso, refletir o resultado também no próprio cartão |
| H-03 | O contador da aba "Aguardando voce" conta só `AGUARDANDO_CONFIRMACAO`, mas a aba também lista `CONTESTADA`. Com os dados de demonstração, o selo mostra 1 e a aba tem 2 cartões. | `GRUPOS.aguardando` e `atualizarContadorDaAba` em `pagina-painel-cliente.js` | 1 | Mostrar na aba só o que espera ação do cliente, ou alinhar o contador ao filtro |
| H-04 | "Exportar meus dados" diz "Arquivo gerado com os seus dados.", mas o arquivo traz só os dados cadastrais carregados na tela, sem solicitações nem avaliações. | `ligarPrivacidade` em `pagina-minha-conta.js` | 2 | Dizer exatamente o que foi exportado, ou implementar a exportação completa (pendência da LGPD em `docs/CONTEXTO.md`) |

## H2. Correspondência entre o sistema e o mundo real

**Como o Obrix aplica**

- Vocabulário do domínio do usuário: "Solicitar servico", "Valor do servico", "Contato
  liberado", e não termos técnicos.
- Valores em reais no formato brasileiro (`UI.moeda`: "R$ 1.500,50") e datas
  `dd/MM/aaaa`.
- A metáfora das etapas (Busca → Resultados → Perfil → Solicitação) em `solicitar.html`.

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-05 | Quase toda a interface está sem acentuação ("Minhas solicitacoes", "voce", "servico", "comissao"), enquanto alguns textos novos têm acento ("Quanto você pagou pelo serviço?"). Para o público brasileiro, parece descuido, atrapalha a leitura e faz leitores de tela pronunciarem errado. | Todas as telas; ex.: `painel-cliente.html`, `<title>` "Minhas solicitacoes — Obrix" | 2 | Acentuar todos os textos da interface (os identificadores do código continuam sem acento, como manda a convenção) |
| H-06 | A mensagem de transição inválida mostra nomes internos de status: "Nao e possivel passar de CONCLUIDA para ACEITA." | `TransicaoInvalidaException` | 2 | Mensagem orientada ao usuário: "Este serviço mudou de situação. Atualize a página para ver as ações disponíveis." |

## H3. Controle e liberdade do usuário

**Como o Obrix aplica**

- Todo modal tem "Voltar", o botão "×" e fecha com Esc ou clique fora (`UI.abrirModal`).
- O cliente pode cancelar enquanto o serviço não começou, e contestar a conclusão.
- A avaliação pode ser adiada ("Agora nao").
- A conta desativada é reativada só de entrar de novo.

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-07 | Não há saída para um serviço parado: em `EM_EXECUCAO` ninguém pode cancelar, e em `CONTESTADA` o cliente não tem nenhuma ação (o cartão fica sem botões até o prestador agir). | Tabela de `acoesDisponiveis`; "O que falta" em `docs/CONTEXTO.md` | 2 | Permitir cancelamento com motivo em `EM_EXECUCAO` e definir um desfecho para contestações paradas |
| H-08 | "Minha conta" não oferece troca de senha, embora a API tenha `POST /conta/senha`. | `minha-conta.html` não tem o formulário | 2 | Incluir o bloco "Alterar senha" em Minha conta |

## H4. Consistência e padrões

**Como o Obrix aplica**

- Um componente para cada coisa: o mesmo selo, o mesmo modal e o mesmo aviso em todas as
  telas (`js/ui.js`), e uma única folha de estilo com tokens (`css/obrix.css`).
- Hierarquia de botões estável: verde para a ação principal, contorno para a secundária
  e vermelho para cancelamentos.

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-09 | A mesma ideia com nomes diferentes: "Cancelar" (prestador) e "Cancelar solicitacao" (cliente); "Valor acordado", "Valor final acordado", "Valor informado" e "Valor do servico"; abas "Concluidas" (cliente) e "Concluidos" (prestador). | `pagina-painel-*.js`, `painel-*.html` | 1 | Glossário de rótulos e revisão dos textos |
| H-10 | O cabeçalho não recebe o estilo previsto: o elemento é `<header data-topo>`, sem a classe `topo`, então o fundo branco, a borda e o cabeçalho fixo de `.topo` nunca são aplicados, e o botão "Sair" aparece com a borda padrão do navegador. | `<header data-topo>` nas 9 telas; `.topo` em `obrix.css`; capturas 07 e 10 | 1 | Adicionar a classe `topo` ao cabeçalho e o reset de borda em `.btn-texto` |
| H-11 | "Termos de uso", "Politica de privacidade" e "Contato" no rodapé parecem links, mas são texto (`<span>`). | Rodapé de todas as telas | 2 | Criar as páginas e transformá-los em links, ou retirá-los |

## H5. Prevenção de erros

**Como o Obrix aplica**

- No formulário de solicitação, só aparecem as categorias e os bairros que o profissional
  atende ("a regra aparece como ausência de opção inválida", `pagina-solicitar.js`).
- Cidade e bairro vêm da região escolhida e são somente leitura; a data mínima é hoje.
- Ações com consequência pedem confirmação em modal, com o efeito descrito antes do
  clique (aceitar, registrar conclusão, confirmar, contestar, cancelar, excluir conta).
- A confirmação às cegas impede que o cliente apenas copie o valor do profissional.

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-12 | "Iniciar execucao" age num toque, sem confirmação, e é irreversível: depois dele, ninguém pode mais cancelar. Um toque acidental no celular tira essa saída do cliente e do prestador. | `executar(s.id, 'iniciar', …)` direto em `aoClicar`, `pagina-painel-prestador.js` | 2 | Pedir confirmação, como nas outras ações |
| H-13 | A avaliação começa com 5 estrelas já marcadas: quem envia sem escolher dá nota máxima sem querer, o que distorce a reputação. | `(n === 5 ? ' checked' : '')` em `abrirAvaliacao` | 2 | Começar sem nota e exigir a escolha |
| H-14 | Telefone, CEP e CPF são texto livre, sem máscara nem validação na tela; os campos de valor são `type="number"`, que em alguns navegadores não aceita a vírgula decimal ("280,50"). | `criar-conta.html`, `solicitar.html`, modais de valor | 2 | Máscaras e validação no front-end; campo de valor que aceite vírgula |

## H6. Reconhecimento em vez de memorização

**Como o Obrix aplica**

- O formulário de solicitação mostra o resumo do profissional escolhido e o endereço da
  conta já preenchido.
- Cada modal repete o serviço, o profissional e as regras daquela ação; o usuário não
  precisa lembrar o percentual nem o prazo.
- O indicador de etapas mostra onde o cliente está no fluxo de contratação.

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-15 | Para confirmar, o cliente precisa lembrar quanto pagou, às vezes dias depois. É consequência intencional da confirmação às cegas, mas o sistema não ajuda a recuperar a informação. | Modal "Confirmar a conclusao do servico" | 2 | Texto de ajuda sugerindo consultar o comprovante (Pix, recibo); validar com usuários se a dificuldade aparece |

## H7. Flexibilidade e eficiência de uso

**Como o Obrix aplica**

- Ações direto no cartão, sem abrir outra página; abas para filtrar; "Carregar mais"
  nas listas longas.
- Busca sem conta; após o login, o sistema volta à página pedida.
- Botões "Entrar como Ana" e "Entrar como Carlos" no modo de demonstração.

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-16 | A lista "Todas" do cliente não prioriza o que espera ação: o cartão a confirmar pode ficar no fim da lista. | Captura `12-painel-cliente-683px-zoom-200.png` (o cartão em destaque é o último) | 2 | Ordenar primeiro o que espera o usuário, ou abrir o painel na aba "Aguardando voce" quando houver pendência |
| H-17 | Os resultados não podem ser ordenados (por nota, por quantidade de avaliações). | `resultados.html` tem só os filtros de categoria e bairro | 1 | Opção de ordenação |

## H8. Estética e design minimalista

**Como o Obrix aplica**

- Telas com uma ação principal clara, muito espaço em branco, paleta curta e tipografia
  consistente (tokens no início de `obrix.css`).
- Informação mostrada quando é útil: o contato só após o aceite e o valor só quando
  existe.

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-18 | O modal de confirmação tem seis blocos de texto; no celular, o botão "Confirmar conclusao" fica abaixo da dobra. | Captura `08-modal-confirmacao.png` | 1 | Encurtar: manter o campo, uma linha sobre a comissão e o prazo; o resto num "Saiba mais" |
| H-19 | Os cartões em grade ficam desalinhados: a regra `.cartao + .cartao { margin-top }` vale também dentro de grades, e o 2º e o 3º cartão descem 12 px (indicadores do prestador, "Como funciona" no início). | `obrix.css`; capturas 01 e 10 | 1 | Anular a margem dentro de grades |

## H9. Ajudar a reconhecer, diagnosticar e recuperar erros

**Como o Obrix aplica**

- Mensagens em português, sem stack trace (`TratadorDeErros`), e com o que fazer: "Este
  registro foi alterado por outra acao. Atualize a pagina e tente de novo."
- Erros por campo no formulário de solicitação (`UI.marcarErro`).
- O modal continua aberto quando a validação local falha ("Informe o motivo.").

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-20 | Os modais de ação fecham assim que o usuário confirma, antes da resposta da API. Se a API recusar, o erro aparece num aviso que some, e o que foi digitado (o motivo, o valor pago) se perde. | `UI.abrirModal` fecha quando `aoConfirmar` não devolve `false`; as telas chamam a API sem esperar | 3 | Manter o modal aberto até a resposta, mostrar o erro dentro dele e só fechar no sucesso |
| H-21 | No cadastro e no login, a validação local mostra um aviso genérico ("Preencha nome, e-mail, telefone e senha.") sem marcar quais campos faltam; na solicitação, só o primeiro erro é mostrado de cada vez. | `pagina-criar-conta.js`, `pagina-entrar.js`, `pagina-solicitar.js` | 2 | Marcar todos os campos com erro ao mesmo tempo, junto ao campo |

## H10. Ajuda e documentação

**Como o Obrix aplica**

- Seção "Como funciona" na página inicial, com os três passos e a regra da comissão.
- Textos de ajuda junto aos campos ("Minimo de 8 caracteres, com letras e numeros.",
  "O CPF e pedido somente ao profissional…").
- Cada modal explica o que vai acontecer antes do clique.

**Problemas**

| Código | Problema | Evidência | Sev. | Recomendação |
|---|---|---|---|---|
| H-22 | Não há página de ajuda ou de perguntas frequentes, e os termos de uso e a política de privacidade não existem como página, embora o cadastro exija "Li e aceito os termos de uso e a politica de privacidade". | `criar-conta.html`; rodapé sem links | 2 | Publicar termos, política e uma página de dúvidas (como confirmar, o que é contestar, como a comissão é calculada) |

---

## Resumo

| Severidade | Quantidade | Problemas |
|---|---|---|
| 4 (catástrofe) | 0 | — |
| 3 (maior) | 2 | H-01 (sem notificações na interface), H-20 (modal fecha antes da resposta e perde o que foi digitado) |
| 2 (menor) | 14 | H-02, H-04, H-05, H-06, H-07, H-08, H-11, H-12, H-13, H-14, H-15, H-16, H-21, H-22 |
| 1 (cosmético) | 6 | H-03, H-09, H-10, H-17, H-18, H-19 |
| **Total** | **22** | |

Pontos fortes que a avaliação confirmou: o status nunca depende só da cor; as regras de
negócio aparecem no momento da ação (modais); a busca não exige conta; e a hierarquia
dos botões protege o cliente sem esconder a contestação.

As correções devem entrar por Change Request. Os problemas de acessibilidade estão em
[acessibilidade.md](acessibilidade.md) e vão para o CR-011; os de usabilidade acima
pedem um CR próprio.
