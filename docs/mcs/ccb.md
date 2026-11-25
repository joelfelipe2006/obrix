# CCB: comitê de controle de mudanças

O CCB (Change Control Board) decide quais Change Requests entram no Obrix. O fluxo em
que ele atua está em [processo.md](processo.md).

## Membros

| Membro | Papel no projeto | Papel no CCB |
|---|---|---|
| Joel Felipe dos Santos | Gerente do projeto e back-end | Coordena o comitê, classifica os CRs, cria as baselines e avalia o impacto no back-end, no banco e na configuração |
| Gabriel Ribeiro | Front-end e UX | Avalia o impacto no front-end, na experiência de uso e na acessibilidade |
| Luiz Leite | Testes e documentação | Avalia os testes necessários e a documentação afetada, e confere a regressão antes das baselines |

## Papéis no processo

- **Solicitante:** registra o CR (qualquer membro, ou um pedido externo, como o professor
  ou a revisão do artigo, registrado por um membro).
- **Coordenador (gerente do projeto):** classifica o CR, convoca a reunião e registra a
  decisão.
- **Analista de impacto:** o responsável pelos ICs afetados, conforme
  [itens-de-configuracao.md](itens-de-configuracao.md).
- **Autor:** implementa o CR na branch e abre o pull request.
- **Revisor:** revisa e aprova o pull request. Precisa ser diferente do autor.

## Regra de decisão

1. Um CR é **aprovado por maioria** dos membros (2 de 3).
2. Cada CR aprovado precisa de **pelo menos um revisor diferente do autor** no pull
   request. O autor pode votar no CCB, mas não aprova o próprio pull request.
3. As decisões possíveis são **aprovada**, **rejeitada** ou **adiada**. Rejeitar ou adiar
   exige um motivo registrado.
4. CR corretivo de prioridade alta pode ser implementado antes da reunião, desde que a
   decisão seja registrada na reunião seguinte (ratificação).
5. Toda decisão é registrada na tabela abaixo e no campo **Decisão do CCB** do CR.

## Registro das reuniões

| Data | Membros presentes | CRs avaliados | Decisões | Observações |
|---|---|---|---|---|
|  |  |  |  |  |

Os CRs CR-001 a CR-013 estão pendentes de ratificação pelo comitê (ver
[change-requests.md](change-requests.md)).
