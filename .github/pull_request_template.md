## Change Request relacionado

Closes #<!-- número da issue do CR -->

CR: CR-XXX

## Tipo da mudança

- [ ] Corretiva
- [ ] Adaptativa
- [ ] Evolutiva
- [ ] Preventiva

## O que mudou

<!-- Resumo objetivo da mudança e do motivo. -->

## Itens de configuração alterados

<!-- Conforme docs/mcs/itens-de-configuracao.md. Ex.: IC-10 solicitacao, IC-14 front-end estático. -->

| IC | Caminho | Alteração |
|---|---|---|
|  |  |  |

## Testes de regressão executados

| Suíte | Comando | Resultado |
|---|---|---|
| Back-end | `mvn clean test` | <!-- ex.: 159/159 --> |
| Front-end | `node --test "src/test/js/*.test.js"` | <!-- ex.: 24/24 --> |
| Cobertura (JaCoCo) | `target/site/jacoco/index.html` | <!-- % de linhas --> |

## Checklist

- [ ] O CR foi aprovado pelo CCB antes do início da implementação.
- [ ] A branch tem nome descritivo e trata só deste CR.
- [ ] Os commits são pequenos e com mensagens em português.
- [ ] Testes novos cobrem a mudança e os existentes continuam passando.
- [ ] O CI está verde neste pull request.
- [ ] Mudança de banco entra por nova migration Flyway (nenhuma migration publicada foi editada).
- [ ] README, `docs/CONTEXTO.md` e `docs/mcs/` atualizados quando necessário.
- [ ] O registro do CR em `docs/mcs/cr/` foi atualizado com commits e datas.

## Aprovação

| Revisor | Papel | Decisão | Data |
|---|---|---|---|
|  |  |  |  |

O revisor que aprova precisa ser diferente do autor do pull request (ver `docs/mcs/ccb.md`).
