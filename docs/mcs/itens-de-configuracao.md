# Itens de configuração

Itens de configuração (ICs) são as partes do Obrix cujas versões são controladas. Todos
estão no repositório git; nenhum artefato gerado (`target/`, relatórios, `.jar`) é IC.
A identificação de uma versão de qualquer IC é o hash do commit ou a tag da baseline.

## Lista

| ID | Nome | Caminho no repositório | Tipo | Responsável | Como é versionado |
|---|---|---|---|---|---|
| IC-01 | Build e dependências | `pom.xml` | Configuração de build | Joel Felipe dos Santos | Commits na branch do CR; o campo `<version>` acompanha a tag da baseline |
| IC-02 | Configuração da aplicação | `src/main/resources/application.yml`, `application-dev.yml`, `application-prod.yml` | Configuração | Joel Felipe dos Santos | Commits; um arquivo por perfil; credenciais só por variável de ambiente, nunca no arquivo |
| IC-03 | Migrations do banco | `src/main/resources/db/migration/V{n}__*.sql` | Esquema de banco (Flyway) | Joel Felipe dos Santos | Cada mudança é uma nova migration numerada; uma migration publicada nunca é editada |
| IC-04 | Banco local de produção | `docker-compose.yml` | Infraestrutura | Joel Felipe dos Santos | Commits; imagem do PostgreSQL com versão fixa |
| IC-05 | Inicialização e configuração | `src/main/java/br/com/obrix/ObrixApplication.java`, `.../config/` | Código-fonte (pacote) | Joel Felipe dos Santos | Commits por pacote, na branch do CR |
| IC-06 | Transversal | `src/main/java/br/com/obrix/shared/` | Código-fonte (pacote) | Joel Felipe dos Santos | Commits por pacote |
| IC-07 | Segurança e autenticação | `src/main/java/br/com/obrix/seguranca/` | Código-fonte (pacote) | Joel Felipe dos Santos | Commits por pacote |
| IC-08 | Usuários e conta | `src/main/java/br/com/obrix/usuario/` | Código-fonte (pacote) | Joel Felipe dos Santos | Commits por pacote |
| IC-09 | Catálogo | `src/main/java/br/com/obrix/catalogo/` | Código-fonte (pacote) | Joel Felipe dos Santos | Commits por pacote |
| IC-10 | Solicitação (máquina de estados) | `src/main/java/br/com/obrix/solicitacao/` | Código-fonte (pacote) | Joel Felipe dos Santos | Commits por pacote |
| IC-11 | Cobrança | `src/main/java/br/com/obrix/cobranca/` | Código-fonte (pacote) | Joel Felipe dos Santos | Commits por pacote |
| IC-12 | Avaliação | `src/main/java/br/com/obrix/avaliacao/` | Código-fonte (pacote) | Joel Felipe dos Santos | Commits por pacote |
| IC-13 | Notificação | `src/main/java/br/com/obrix/notificacao/` | Código-fonte (pacote) | Joel Felipe dos Santos | Commits por pacote |
| IC-14 | Front-end estático | `src/main/resources/static/` (HTML, `css/obrix.css`, `js/`) | Código-fonte (front-end) | Gabriel Ribeiro | Commits; servido pela aplicação, entra no mesmo `.jar` |
| IC-15 | Testes do back-end | `src/test/java/`, `src/test/resources/application-test.yml` | Testes (JUnit 5) | Luiz Leite | Commits junto com a mudança testada; rodam no CI |
| IC-16 | Testes do front-end | `src/test/js/` | Testes (`node:test`) | Luiz Leite | Commits junto com a mudança testada; rodam no CI |
| IC-17 | README | `README.md` | Documentação | Luiz Leite | Commits; atualizado no mesmo PR da mudança |
| IC-18 | Contexto técnico | `docs/CONTEXTO.md` | Documentação | Luiz Leite | Commits; atualizado no mesmo PR da mudança |
| IC-19 | Documentação de gerência de configuração | `docs/mcs/` | Documentação de processo | Joel Felipe dos Santos | Commits; um arquivo por CR em `docs/mcs/cr/` |
| IC-20 | Modelos e CI do GitHub | `.github/` (modelos de issue e PR, `workflows/ci.yml`) | Configuração de processo | Luiz Leite | Commits; o workflow roda em todo pull request e push na `main` |
| IC-21 | Arquivos ignorados | `.gitignore` | Configuração do repositório | Joel Felipe dos Santos | Commits |

## Regras

- Um CR lista, na análise de impacto, os ICs que vai alterar. O pull request lista os
  ICs que de fato alterou.
- O responsável por um IC participa da análise de impacto dos CRs que o afetam.
- Um IC novo (pacote, arquivo de configuração, pasta de documentação) entra nesta lista
  no mesmo pull request em que é criado.
