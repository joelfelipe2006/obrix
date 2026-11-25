# Registro de Change Requests

Registro geral das solicitações de mudança do Obrix. Cada CR tem um arquivo em
[cr/](cr/) com a descrição, a justificativa, os itens de configuração afetados, a análise
de impacto e, quando implementado, os arquivos alterados e os commits. O fluxo está em
[processo.md](processo.md), e a regra de decisão em [ccb.md](ccb.md).

## Classificação

| Tipo | Quando usar |
|---|---|
| Corretiva | Corrige um defeito ou uma regra que não cumpre o objetivo |
| Adaptativa | Adapta o sistema a um ambiente ou exigência externa (banco, hospedagem, professor) |
| Evolutiva | Acrescenta funcionalidade ou melhora uma existente |
| Preventiva | Reduz risco futuro sem mudar o comportamento: testes, qualidade, processo |

Prioridade: **alta** (bloqueia uma entrega ou afeta segurança ou cobrança), **média**
(necessária na entrega seguinte), **baixa** (pode esperar).

## Registro

| CR | Título | Tipo | Prioridade | Situação | Implementação | Branch | Baseline | Decisão do CCB |
|---|---|---|---|---|---|---|---|---|
| [CR-001](cr/CR-001.md) | Corrige o carregamento das solicitações no painel | Corretiva | Alta | Implementado | 03/11/2026 | `main` | `v0.1.0` | Pendente de ratificação |
| [CR-002](cr/CR-002.md) | Testes de integração da API e documento de contexto técnico | Preventiva | Alta | Implementado | 12/11/2026 | `correcoes` | `v0.2.0` | Pendente de ratificação |
| [CR-003](cr/CR-003.md) | Segurança e LGPD da conta do usuário | Corretiva | Alta | Implementado | 12/11/2026 | `correcoes` | `v0.2.0` | Pendente de ratificação |
| [CR-004](cr/CR-004.md) | Robustez da API e ajustes de regras de negócio | Corretiva | Média | Implementado | 12/11/2026 | `correcoes` | `v0.2.0` | Pendente de ratificação |
| [CR-005](cr/CR-005.md) | Paginação nas listas e tratamento de falta de conexão no front-end | Evolutiva | Média | Implementado | 12/11/2026 | `correcoes` | `v0.2.0` | Pendente de ratificação |
| [CR-006](cr/CR-006.md) | Migração do banco de MySQL para PostgreSQL | Adaptativa | Alta | Implementado | 13/11/2026 | `postgres` | `v0.2.0` | Pendente de ratificação |
| [CR-007](cr/CR-007.md) | Banco na nuvem com dados de demonstração no perfil demo | Adaptativa | Média | Implementado | 13/11/2026 | `banco-nuvem` | `v0.2.0` | Pendente de ratificação |
| [CR-008](cr/CR-008.md) | Medição de cobertura de testes com JaCoCo | Preventiva | Média | Implementado | 23/11/2026 | `cobertura` | `v0.3.0` | Pendente de ratificação |
| [CR-009](cr/CR-009.md) | Confirmação da conclusão com o valor pago pelo cliente | Evolutiva | Alta | Implementado | 23/11/2026 | `confirmacao-valor` | `v0.3.0` | Pendente de ratificação |
| [CR-010](cr/CR-010.md) | Confirmação às cegas | Corretiva | Alta | Implementado | 25/11/2026 | `confirmacao-cega` | `v0.3.0` | Pendente de ratificação |
| [CR-011](cr/CR-011.md) | Acessibilidade: auditoria WCAG e correções | Evolutiva | Média | Aberto | — | — | — | Pendente de ratificação |
| [CR-012](cr/CR-012.md) | Deploy da aplicação em nuvem | Adaptativa | Alta | Aberto | — | — | — | Pendente de ratificação |
| [CR-013](cr/CR-013.md) | Processo de gerência de configuração e CI de regressão | Preventiva | Alta | Em implementação | — | `mcs-processo` | — | Pendente de ratificação |


## Como os CRs CR-001 a CR-010 foram levantados

O processo formal foi instituído em 25/11/2026 (CR-013). As mudanças anteriores foram
levantadas do histórico do git (`git log` e `git diff --stat` de cada branch) e
registradas nessa data, com as datas reais de implementação, os hashes dos commits e os
arquivos de fato alterados.

- A branch `correcoes` reuniu 18 commits de temas diferentes. Eles foram separados em
  quatro CRs pelo tema de cada commit (CR-002 a CR-005).
- As branches `correcoes` e `postgres` foram integradas na `main` por avanço rápido
  (fast-forward), sem commit de merge nem pull request. O CR-001 foi um commit direto na
  `main`, antes de o projeto usar branches.
- Quando o histórico não registra quem pediu a mudança, o campo **Solicitante** diz
  isso, em vez de supor.
- Nenhum desses CRs passou por reunião registrada do CCB. A decisão consta como
  **pendente de ratificação pelo comitê**, e será preenchida quando o comitê se reunir
  (ver a tabela de reuniões em [ccb.md](ccb.md)).

## Próximo número livre

**CR-014**
