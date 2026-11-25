# Baselines e política de versões

## Política de versões

O Obrix usa versionamento semântico (`MAJOR.MINOR.PATCH`):

| Parte | Quando sobe | Exemplo |
|---|---|---|
| `MAJOR` | Mudança incompatível na API (`/api/v1` vira `/api/v2`) ou no esquema que exija migração manual | `1.0.0` → `2.0.0` |
| `MINOR` | Conjunto de CRs evolutivos ou adaptativos que fecha uma entrega, sem quebrar a API | `0.2.0` → `0.3.0` |
| `PATCH` | Apenas CRs corretivos sobre a baseline anterior | `0.3.0` → `0.3.1` |

Enquanto o projeto estiver em `0.y.z`, a API ainda pode mudar entre versões `MINOR`; a
`1.0.0` marca a primeira entrega considerada estável.

## Como uma baseline é criada

1. Todos os CRs da entrega estão integrados na `main` por pull request, com CI verde.
2. A auditoria de configuração de [processo.md](processo.md) é feita.
3. A versão do `pom.xml` (`<version>`) é atualizada para a da baseline, por um CR.
4. O gerente do projeto cria uma tag anotada no commit de merge da `main`, com a lista
   do que entrou na mensagem, e envia a tag:

   ```bash
   git tag -a v0.4.0 <commit> -m "..."
   git push origin v0.4.0
   ```

5. A baseline é registrada na tabela abaixo.

Uma tag publicada não é movida nem apagada. Uma correção vira uma nova versão `PATCH`.

## Baselines publicadas

| Versão | Commit na `main` | Data do commit | CRs incluídos | Conteúdo |
|---|---|---|---|---|
| `v0.1.0` | `1399db3` | 03/11/2026 | CR-001 | MVP: back-end Spring Boot com a máquina de estados da solicitação e a comissão na confirmação, testes de unidade do ciclo e da comissão, front-end estático ligado à API, H2 em desenvolvimento e MySQL previsto para produção |
| `v0.2.0` | `0693f2f` (merge `banco-nuvem`) | 13/11/2026 | CR-002 a CR-007 | Testes de integração da API, correções de segurança e LGPD da conta, robustez da API, paginação, migração para PostgreSQL e banco na nuvem com o perfil `demo` |
| `v0.3.0` | `6a2ce04` (merge `confirmacao-cega`) | 25/11/2026 | CR-008 a CR-010 | Cobertura com JaCoCo, confirmação com o valor pago pelo cliente e confirmação às cegas |

As três tags foram criadas em 25/11/2026, junto com a instituição do processo (CR-013),
apontando para os commits em que cada entrega foi de fato integrada na `main`. As datas
da tabela são as dos commits.

### Pendências conhecidas

- O `pom.xml` ainda declara `<version>0.1.0</version>` nas baselines `v0.2.0` e `v0.3.0`,
  porque a versão não era atualizada antes do processo. A partir da próxima baseline, o
  passo 3 acima corrige isso.
- `v0.1.0` aponta para um commit sem merge: até ali o desenvolvimento foi feito direto na
  `main`.
