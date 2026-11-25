# Processo de gerência de configuração

Este documento descreve como uma mudança entra no Obrix, do pedido até a baseline.
Os documentos relacionados ficam na mesma pasta:

- [ccb.md](ccb.md): o comitê de controle de mudanças e a regra de decisão;
- [itens-de-configuracao.md](itens-de-configuracao.md): o que está sob controle de configuração;
- [change-requests.md](change-requests.md): o registro geral dos CRs, com um arquivo por CR em [cr/](cr/);
- [baselines.md](baselines.md): a política de versões e as baselines publicadas.

## Fluxo de uma mudança

```
 1. CR registrado ──> 2. Classificação ──> 3. Análise de impacto ──> 4. Decisão do CCB
                                                                          │
              ┌──────────── rejeitada ou adiada: fim (fica no registro) ◄─┤
              │                                                           │ aprovada
              ▼                                                           ▼
 9. Baseline com tag ◄── 8. Merge na main ◄── 7. CI verde ◄── 6. Pull request ◄── 5. Branch
                                                              com revisão
```

| Etapa | O que acontece | Responsável | Registro |
|---|---|---|---|
| 1. CR registrado | Qualquer membro abre uma issue com o modelo **Change Request** e recebe o próximo número livre (`CR-XXX`). | Solicitante | Issue no GitHub e linha em [change-requests.md](change-requests.md) |
| 2. Classificação | Define o tipo (corretiva, adaptativa, evolutiva ou preventiva) e a prioridade (alta, média ou baixa). | Gerente do projeto | Campos da issue |
| 3. Análise de impacto | Lista os ICs afetados, os módulos, os riscos, o esforço e os testes necessários. | Responsável pelos ICs afetados | Campos da issue e `docs/mcs/cr/CR-XXX.md` |
| 4. Decisão do CCB | O comitê aprova, rejeita ou adia, pela regra de [ccb.md](ccb.md). | CCB | Tabela de reuniões em [ccb.md](ccb.md) e campo de decisão do CR |
| 5. Branch | Uma branch por CR, criada a partir da `main` atualizada. | Autor | Nome da branch no CR |
| 6. Pull request com revisão | O PR usa o modelo do repositório, cita o CR (`Closes #`) e lista os ICs alterados e os testes executados. Pelo menos um revisor diferente do autor aprova. | Autor e revisor | Pull request |
| 7. CI verde | O workflow **CI de regressão** roda `mvn -B clean test` e `node --test` e publica o relatório do JaCoCo. Sem CI verde, não há merge. | Automático | Aba Actions e artifact `relatorio-jacoco` |
| 8. Merge na main | Merge do PR aprovado e com CI verde. A branch pode ser apagada depois. | Autor | Commit de merge na `main` |
| 9. Baseline com tag | Quando um conjunto de CRs fecha uma entrega, a `main` recebe uma tag anotada `vX.Y.Z`. | Gerente do projeto | Tag e [baselines.md](baselines.md) |

Uma mudança só é considerada concluída depois da etapa 8, e o arquivo do CR é
atualizado com os commits, a branch, as datas e o resultado dos testes.

## Política de branches

- **`main` protegida.** Representa sempre a última versão integrada e testada. Ninguém
  faz commit direto nela: tudo entra por pull request, com revisão e CI verde. A proteção
  é configurada no GitHub em *Settings → Branches → Branch protection rules*, exigindo
  pull request, uma aprovação e o check **Testes de regressão**.
- **Uma branch por CR.** A branch trata só da mudança aprovada naquele CR. Se surgir
  outra necessidade no meio do trabalho, ela vira um novo CR.
- **Nomes descritivos**, em português, minúsculos e com hífen, dizendo o que muda:
  `confirmacao-cega`, `banco-nuvem`, `mcs-processo`. O número do CR vai no pull request
  e no arquivo do CR, que também registram o nome da branch.
- **Vida curta.** A branch nasce da `main` atualizada (`git checkout main && git pull`) e
  é integrada assim que o PR é aprovado, para evitar conflitos longos.

## Commits

- Pequenos, com uma intenção cada, e mensagem em português no imperativo de terceira
  pessoa (`Adiciona…`, `Corrige…`, `Documenta…`).
- Código, testes e documentação de uma mesma mudança podem ir em commits separados, na
  mesma branch.
- Mudança de estrutura do banco entra por uma nova migration Flyway. Uma migration
  publicada nunca é editada.

## Testes de regressão

Toda mudança roda as duas suítes completas, localmente e no CI:

```bash
mvn -B clean test                   # back-end: JUnit 5, integração com H2 e relatório JaCoCo
node --test src/test/js/*.test.js   # front-end: node:test, sem dependências
```

O relatório de cobertura fica em `target/site/jacoco/index.html` (localmente) e no
artifact `relatorio-jacoco` de cada execução do CI. O resultado dos testes entra no
pull request e no arquivo do CR.

## Auditoria de configuração

Antes de cada baseline, conferir:

1. **Auditoria funcional:** todos os CRs listados na baseline estão implementados e os
   testes de regressão passam na tag.
2. **Auditoria física:** os ICs listados em [itens-de-configuracao.md](itens-de-configuracao.md)
   existem nos caminhos indicados, e a versão do `pom.xml` corresponde à da tag.
3. **Rastreabilidade:** cada CR implementado aponta para a branch, os commits e o merge
   correspondentes, e cada merge na `main` corresponde a um CR.

## Histórico do processo

O processo formal foi instituído em 25/11/2026 (CR-013). As mudanças anteriores foram
levantadas a partir do histórico do git e registradas retroativamente, com as datas reais
de implementação. Até então, a decisão de cada mudança não passou por reunião registrada
do CCB, e as branches `correcoes` e `postgres` foram integradas na `main` por avanço
rápido (fast-forward), sem commit de merge nem pull request. Por isso a decisão do CCB
desses CRs consta como pendente de ratificação.
