# Interação humano-computador

Artefatos de IHC do Obrix, produzidos a partir do sistema implementado.

| Artefato | Conteúdo |
|---|---|
| [Análise contextual](analise-contextual.md) | Contexto de uso do cliente e do prestador (onde, quando, dispositivo, familiaridade digital, restrições) e modelo de usuário |
| [Proto-personas](personas.md) | Ana (cliente) e Carlos (eletricista autônomo), com objetivos, frustrações, uso de tecnologia e cenários |
| [Modelagem de tarefas](modelo-de-tarefas.md) | Análise hierárquica de tarefas (HTA) do cliente e do prestador, com diagramas e planos |
| [Projeto de diálogo](projeto-de-dialogo.md) | Mapa de navegação entre as telas, botões por status (`acoesDisponiveis`) e o diálogo de cada ação, com as mensagens reais |
| [Prototipação](prototipos.md) | Wireframes de baixa e média fidelidade (SVG) e capturas de alta fidelidade do sistema real |
| [Avaliação heurística](heuristicas-nielsen.md) | As 10 heurísticas de Nielsen aplicadas ao sistema: evidências, 22 problemas e a severidade de cada um |
| [Acessibilidade](acessibilidade.md) | Auditoria pela WCAG 2.2 nível AA, com as razões de contraste calculadas e as correções para o CR-011 |

## Limites

- **Ainda não houve pesquisa com usuários.** As personas são proto-personas baseadas em
  dados secundários (IBGE) e nas hipóteses da equipe. A validação com usuários, com o
  questionário SUS, é a etapa seguinte (ver
  [analise-contextual.md](analise-contextual.md#validação-prevista)).
- A avaliação heurística foi feita por um avaliador; Nielsen recomenda de 3 a 5.
- A auditoria de acessibilidade foi feita pela leitura do código e pelas telas, sem
  leitor de tela e sem varredura automatizada.
- Os wireframes de baixa e média fidelidade foram documentados a partir do sistema já
  implementado. Os mockups originais do documento do PI serão anexados em
  [prototipos/originais/](prototipos/originais/).

## Como os achados viram mudança

Os problemas registrados aqui não foram corrigidos neste trabalho. As correções de
acessibilidade (A-01 a A-17) vão para o CR-011, e os problemas de usabilidade (H-01 a
H-22) pedem um CR próprio, pelo processo de gerência de configuração.
