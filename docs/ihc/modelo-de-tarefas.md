# Modelagem de tarefas (HTA)

Análise hierárquica de tarefas (*Hierarchical Task Analysis*) das tarefas principais do
Obrix, feita sobre o fluxo implementado. Cada tarefa tem o diagrama de decomposição e o
**plano**, que diz em que ordem e em que condições as subtarefas são executadas.

Convenções:

- a numeração segue a HTA clássica (0 → 1, 2… → 1.1, 1.2…);
- **Plano** usa "faça 1-2-3" para sequência, "se… então…" para condição e "até que"
  para repetição;
- as telas e arquivos citados são os do sistema real (`src/main/resources/static/`);
- os diagramas usam Mermaid, que o GitHub renderiza.

As tarefas são executadas pelas personas de [personas.md](personas.md).

---

## Cliente

### 0. Contratar um serviço residencial (visão geral)

```mermaid
flowchart TD
    T0["0. Contratar um serviço residencial"]
    T0 --> T1["1. Buscar profissional"]
    T0 --> T2["2. Solicitar serviço"]
    T0 --> T3["3. Acompanhar a solicitação"]
    T0 --> T4["4. Confirmar informando o valor pago<br/>ou contestar"]
    T0 --> T5["5. Avaliar o profissional"]
```

**Plano 0:** faça 1-2-3. Se o profissional recusar (status `RECUSADA`), volte a 1. Quando
o status chegar a `AGUARDANDO_CONFIRMACAO`, faça 4. Se 4 terminar em `CONCLUIDA`, faça 5
(opcional). Se 4 terminar em `CONTESTADA`, espere o profissional registrar a conclusão
de novo e repita 4.

### 1. Buscar profissional

```mermaid
flowchart TD
    T1["1. Buscar profissional"]
    T1 --> T11["1.1 Escolher a categoria"]
    T1 --> T12["1.2 Escolher cidade e bairro<br/>(opcional)"]
    T1 --> T13["1.3 Acionar 'Buscar profissionais'"]
    T1 --> T14["1.4 Comparar os resultados"]
    T1 --> T15["1.5 Abrir o perfil de um profissional"]
    T14 --> T141["1.4.1 Ler nota e quantidade<br/>de avaliações"]
    T14 --> T142["1.4.2 Ler descrição, categorias<br/>e regiões"]
    T14 --> T143["1.4.3 Considerar o bloco<br/>'ainda sem avaliação'"]
    T15 --> T151["1.5.1 Ler avaliações<br/>de serviços concluídos"]
```

**Plano 1:** faça 1.1. Faça 1.2 se quiser restringir a região. Faça 1.3. Se a categoria
não foi escolhida, o sistema avisa "Escolha uma categoria para buscar." e volta a 1.1.
Faça 1.4 (1.4.1 e 1.4.2 em qualquer ordem; 1.4.3 se houver o bloco separado). Faça 1.5
até encontrar um profissional adequado; se nenhum servir, refaça 1.2 com outros filtros
(tela de resultados, botão "Filtrar").

Telas: `index.html` → `resultados.html` → `perfil.html`. A busca não exige conta.

### 2. Solicitar serviço

```mermaid
flowchart TD
    T2["2. Solicitar serviço"]
    T2 --> T21["2.1 Acionar 'Solicitar serviço'<br/>no perfil"]
    T2 --> T22["2.2 Entrar na conta<br/>(se não estiver logado)"]
    T2 --> T23["2.3 Preencher o pedido"]
    T2 --> T24["2.4 Enviar a solicitação"]
    T23 --> T231["2.3.1 Escolher a categoria<br/>(só as que o profissional atende)"]
    T23 --> T232["2.3.2 Descrever o serviço"]
    T23 --> T233["2.3.3 Escolher a data desejada"]
    T23 --> T234["2.3.4 Escolher o bairro do serviço"]
    T23 --> T235["2.3.5 Conferir o endereço de execução"]
```

**Plano 2:** faça 2.1. Se não houver sessão, faça 2.2 (o sistema redireciona para
`entrar.html` e volta à solicitação depois do login). Faça 2.3: 2.3.1 a 2.3.4 em
qualquer ordem; 2.3.5 depois de 2.3.4, porque cidade e bairro vêm da região escolhida.
Faça 2.4. Se faltar descrição ou endereço, o sistema marca o campo e volta a 2.3.

Tela: `solicitar.html`, quarta e última etapa do fluxo de contratação.

### 3. Acompanhar a solicitação

```mermaid
flowchart TD
    T3["3. Acompanhar a solicitação"]
    T3 --> T31["3.1 Abrir 'Meu painel'"]
    T3 --> T32["3.2 Filtrar por aba<br/>(Todas, Em andamento, Aguardando você,<br/>Concluídas, Encerradas)"]
    T3 --> T33["3.3 Ler o status no selo do cartão"]
    T3 --> T34["3.4 Usar o contato liberado"]
    T3 --> T35["3.5 Cancelar a solicitação"]
```

**Plano 3:** faça 3.1-3.2-3.3 sempre que quiser saber o andamento. Se o status for
`ACEITA` ou posterior, 3.4 fica disponível (telefone e e-mail no cartão). Se o status for
`SOLICITADA` ou `ACEITA` e o cliente desistir, faça 3.5 (motivo obrigatório). Repita até
o status chegar a `AGUARDANDO_CONFIRMACAO` ou a um estado final.

Tela: `painel-cliente.html`.

### 4. Confirmar informando o valor pago, ou contestar

```mermaid
flowchart TD
    T4["4. Responder à conclusão registrada"]
    T4 --> T41["4.1 Localizar o cartão em destaque<br/>'Aguardando confirmação'"]
    T4 --> T42["4.2 Decidir: confirmar ou contestar"]
    T4 --> T43["4.3 Confirmar a conclusão"]
    T4 --> T44["4.4 Contestar a conclusão"]
    T43 --> T431["4.3.1 Abrir o modal de confirmação"]
    T43 --> T432["4.3.2 Informar quanto pagou<br/>(sem ver o valor do profissional)"]
    T43 --> T433["4.3.3 Ler o que acontece ao confirmar"]
    T43 --> T434["4.3.4 Acionar 'Confirmar conclusão'"]
    T44 --> T441["4.4.1 Abrir o modal de contestação"]
    T44 --> T442["4.4.2 Descrever o motivo"]
    T44 --> T443["4.4.3 Acionar 'Enviar contestação'"]
```

**Plano 4:** faça 4.1-4.2. Se o serviço foi feito como combinado, faça 4.3 (4.3.1-4.3.2-
4.3.3-4.3.4). Se não foi, faça 4.4 (4.4.1-4.4.2-4.4.3). Em 4.3.4:

- se o valor informado for **igual** ao registrado pelo profissional, o status vai para
  `CONCLUIDA` e a comissão é registrada;
- se for **diferente**, o status vai para `CONTESTADA`, sem comissão, e o serviço volta
  ao profissional.

Se o cliente não fizer nada em 7 dias, o sistema confirma sozinho (confirmação
automática), exceto se a solicitação já tiver sido contestada alguma vez.

Tela: `painel-cliente.html`, modais "Confirmar a conclusão do serviço" e "Contestar a
conclusão".

### 5. Avaliar o profissional

```mermaid
flowchart TD
    T5["5. Avaliar o profissional"]
    T5 --> T51["5.1 Acionar 'Avaliar profissional'<br/>no cartão concluído"]
    T5 --> T52["5.2 Escolher a nota (1 a 5)"]
    T5 --> T53["5.3 Escrever um comentário<br/>(opcional)"]
    T5 --> T54["5.4 Acionar 'Enviar avaliação'"]
```

**Plano 5:** só disponível em `CONCLUIDA` e uma vez por serviço. Faça 5.1-5.2, 5.3 se
quiser, e 5.4. O cliente pode escolher "Agora não" e voltar depois.

---

## Prestador

### 0. Atender uma solicitação (visão geral)

```mermaid
flowchart TD
    P0["0. Atender uma solicitação"]
    P0 --> P1["1. Aceitar ou recusar"]
    P0 --> P2["2. Iniciar a execução"]
    P0 --> P3["3. Registrar a conclusão com o valor"]
    P0 --> P4["4. Acompanhar o extrato"]
```

**Plano 0:** faça 1. Se recusou, a tarefa termina. Se aceitou, combine o serviço pelo
contato liberado e, no dia, faça 2. Ao terminar o serviço, faça 3. Se o cliente
contestar (status `CONTESTADA`), resolva a divergência com ele e faça 3 de novo. Faça 4
periodicamente.

### 1. Aceitar ou recusar

```mermaid
flowchart TD
    P1["1. Aceitar ou recusar"]
    P1 --> P11["1.1 Abrir o painel, aba 'Solicitações'"]
    P1 --> P12["1.2 Ler descrição, categoria,<br/>bairro e data desejada"]
    P1 --> P13["1.3 Aceitar"]
    P1 --> P14["1.4 Recusar"]
    P13 --> P131["1.3.1 Ler que o contato será liberado<br/>sem custo"]
    P13 --> P132["1.3.2 Acionar 'Aceitar' no modal"]
    P14 --> P141["1.4.1 Informar o motivo"]
    P14 --> P142["1.4.2 Acionar 'Recusar solicitacao'"]
```

**Plano 1:** faça 1.1-1.2. Se puder atender, faça 1.3 (1.3.1-1.3.2); senão, faça 1.4
(1.4.1-1.4.2). Antes do aceite, o prestador não cancela: o caminho é recusar.

### 2. Iniciar a execução

```mermaid
flowchart TD
    P2["2. Iniciar a execução"]
    P2 --> P21["2.1 Abrir a aba 'Em andamento'"]
    P2 --> P22["2.2 Acionar 'Iniciar execucao'<br/>no cartão aceito"]
    P2 --> P23["2.3 Cancelar, se o serviço não<br/>for acontecer (motivo obrigatório)"]
```

**Plano 2:** faça 2.1. No dia do serviço, faça 2.2. Se o serviço não for acontecer,
faça 2.3 em vez de 2.2 (só enquanto o status for `ACEITA`).

### 3. Registrar a conclusão com o valor

```mermaid
flowchart TD
    P3["3. Registrar a conclusão"]
    P3 --> P31["3.1 Acionar 'Registrar conclusao'<br/>no cartão em execução ou contestado"]
    P3 --> P32["3.2 Informar o valor final acordado"]
    P3 --> P33["3.3 Ler o que acontece agora<br/>(prazo do cliente, comissão)"]
    P3 --> P34["3.4 Confirmar no modal"]
```

**Plano 3:** faça 3.1-3.2-3.3-3.4. Se o valor estiver vazio ou for zero, o sistema
avisa "Informe um valor maior que zero." e volta a 3.2. O status vai para
`AGUARDANDO_CONFIRMACAO`, e a comissão ainda não existe.

### 4. Acompanhar o extrato

```mermaid
flowchart TD
    P4["4. Acompanhar o extrato"]
    P4 --> P41["4.1 Ler os indicadores do topo<br/>(nota média, concluídos, comissões)"]
    P4 --> P42["4.2 Abrir a aba 'Cobrancas'"]
    P4 --> P43["4.3 Ler cada linha: serviço, cliente,<br/>data, valor e comissão"]
    P4 --> P44["4.4 Acionar 'Carregar mais'<br/>(se houver)"]
```

**Plano 4:** faça 4.1 a qualquer momento. Para o detalhe, faça 4.2-4.3. Faça 4.4 até
não haver mais linhas.

---

## Observações da análise

- **Tarefa 4 do cliente é a mais crítica**: concentra a regra de negócio (comissão,
  prazo, divergência) e depende de o cliente lembrar o valor que pagou. É a primeira
  candidata ao teste com usuários.
- **O prestador não é avisado** de uma nova solicitação: precisa abrir o painel (plano
  P1). As notificações já são gravadas, mas não aparecem na interface.
- **Tarefas 1 e 2 do cliente** cabem em quatro telas, com o indicador de etapas em
  `solicitar.html`.
