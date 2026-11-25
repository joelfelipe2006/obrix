# Análise contextual e modelo de usuário

## Natureza desta análise

**Até agora não houve pesquisa com usuários.** Nenhuma entrevista, observação ou teste
com clientes ou prestadores reais foi feito. O que segue é uma análise baseada em:

- **dados secundários**: segundo o IBGE, o Brasil tinha 26,1 milhões de trabalhadores
  por conta própria em 2025, cerca de 73% deles sem CNPJ;
- **o escopo do sistema** como está implementado (README e `docs/CONTEXTO.md`);
- **as decisões de interface já tomadas no código**, que revelam as hipóteses da equipe
  sobre quem usa o sistema.

Por isso, os perfis de [personas.md](personas.md) são **proto-personas**: hipóteses a
validar. A etapa seguinte é a validação com usuários reais, com o questionário SUS
(*System Usability Scale*) aplicado após tarefas do [modelo de tarefas](modelo-de-tarefas.md)
(ver "Validação prevista", no fim deste documento).

## Dois usuários, dois contextos

O Obrix liga **clientes** que precisam de um serviço residencial a **prestadores
autônomos** (eletricista, encanador, pintor, diarista, jardineiro). Os dois usam o mesmo
sistema, mas em situações muito diferentes.

### Cliente

| Dimensão | Contexto provável | Evidência ou origem |
|---|---|---|
| **Onde** | Em casa, muitas vezes diante do problema (chuveiro queimado, vazamento). Também no trabalho, no intervalo. | Natureza do serviço residencial; a busca é por bairro de execução (`index.html`) |
| **Quando** | Em dois momentos distintos: (1) na necessidade, com alguma urgência, para buscar e solicitar; (2) dias depois, quando o serviço termina, para confirmar e avaliar. | Ciclo da solicitação; prazo de 7 dias para confirmar |
| **Dispositivo** | Principalmente celular; computador ocasional. | Hipótese da equipe; o layout é responsivo, com alvos de toque de 44 px (`.btn { min-height: 44px }` em `obrix.css`) |
| **Familiaridade digital** | Média: usa aplicativos de mensagem, banco e compras, mas não necessariamente plataformas de serviço. Faixa etária ampla, incluindo pessoas mais velhas. | Hipótese; reforça a necessidade de textos explicativos e contraste adequado |
| **Objetivo central** | Resolver o problema com alguém confiável, sem pagar para isso e sem se expor antes de decidir. | Busca sem conta; contato só após o aceite; nenhum custo para o cliente |
| **Restrições** | Pressa; desconfiança de desconhecidos que vão entrar em casa; receio de golpes; interrupções (o celular é usado no meio de outras tarefas); conexão móvel instável. | A busca é pública e não exige conta (`pagina-inicio.js`); faixa "Sem conexão com o servidor" (`ui.js`) |
| **Momento crítico** | A confirmação da conclusão: o cliente precisa informar quanto pagou sem ver o valor registrado pelo profissional (confirmação às cegas). | `pagina-painel-cliente.js`, modal de confirmação |

### Prestador

| Dimensão | Contexto provável | Evidência ou origem |
|---|---|---|
| **Onde** | Na rua, entre um serviço e outro, ou no próprio local do serviço. | Natureza do trabalho autônomo de campo |
| **Quando** | Ao longo do dia, em intervalos curtos: responder a uma nova solicitação, iniciar e registrar a conclusão logo ao terminar o serviço. À noite, para conferir o extrato. | Ações do painel (`pagina-painel-prestador.js`) |
| **Dispositivo** | Quase sempre celular, às vezes com as mãos ocupadas ou sujas e sob sol. | Hipótese; pesa a favor de botões grandes e de alto contraste |
| **Familiaridade digital** | Variada. Muitos dependem de indicação boca a boca e do WhatsApp; parte pode ter baixa escolaridade formal. | IBGE: cerca de 73% dos conta própria sem CNPJ, o que indica informalidade e pouca estrutura administrativa |
| **Objetivo central** | Conseguir mais clientes sem pagar para receber contatos, e pagar só quando o serviço de fato aconteceu. | Comissão de 10% só na confirmação; "Receber solicitações não tem custo" (`painel-prestador.html`) |
| **Restrições** | Informalidade (sem CNPJ; o sistema pede CPF ao profissional); pouco tempo; receio de taxas escondidas; renda variável, que torna a comissão sensível. | Cadastro do prestador com CPF (`criar-conta.html`); extrato de comissões |
| **Momento crítico** | Registrar a conclusão com o valor final, ciente de que o cliente vai informar o valor pago às cegas e de que uma divergência volta o serviço para ele. | Modal "Registrar conclusão" (`pagina-painel-prestador.js`) |

## Ambiente social e de confiança

- **Confiança entre desconhecidos** é a principal barreira do domínio. O sistema responde
  com: avaliações só de serviços concluídos, perfis sem avaliação separados para não
  parecerem mal avaliados (`pagina-resultados.js`) e contato liberado só depois do
  aceite (`pagina-perfil.js`).
- **Assimetria financeira**: quem paga a comissão é o prestador. Daí a confirmação com o
  valor pago pelo cliente e a confirmação às cegas.
- **Informalidade**: a plataforma não movimenta dinheiro (o pagamento é acertado
  direto entre as partes), o que reduz a barreira de entrada para quem não tem CNPJ ou
  conta empresarial.

## Modelo de usuário (síntese)

| Característica | Cliente | Prestador |
|---|---|---|
| Frequência de uso | Esporádica (quando surge um problema) | Diária ou semanal |
| Conhecimento do domínio | Baixo (não sabe o preço justo nem o tempo do serviço) | Alto (conhece o trabalho e os preços) |
| Conhecimento do sistema | Baixo: cada uso pode ser "o primeiro" | Cresce com o uso: torna-se usuário frequente |
| Implicação de projeto | Interface autoexplicativa, poucas telas, linguagem simples, regras explicadas no momento da ação | Eficiência: ações diretas nos cartões, indicadores no topo, extrato claro |

O fluxo de contratação do cliente foi limitado a quatro telas (busca, resultados,
perfil, solicitação), com o indicador de etapas em `solicitar.html`, justamente porque o
cliente é um usuário esporádico.

## Validação prevista

| Etapa | O que fazer | Instrumento |
|---|---|---|
| 1. Recrutamento | 5 clientes e 5 prestadores, incluindo pessoas acima de 50 anos e prestadores sem CNPJ | Convite direto |
| 2. Teste de tarefas | Executar no celular as tarefas do [modelo de tarefas](modelo-de-tarefas.md), observando erros e hesitações | Roteiro de tarefas e observação |
| 3. Questionário | SUS (10 itens, escala de 1 a 5) ao fim das tarefas; meta inicial: pontuação acima de 68 | Questionário SUS |
| 4. Revisão das personas | Confirmar ou corrigir as hipóteses desta análise e de [personas.md](personas.md) | Síntese dos resultados |

Até essa validação, todas as afirmações sobre contexto, dispositivo e familiaridade
digital devem ser lidas como hipóteses.
