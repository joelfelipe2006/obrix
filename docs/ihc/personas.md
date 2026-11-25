# Proto-personas

> **Atenção:** estas são **proto-personas**, construídas a partir de dados secundários
> (IBGE) e das hipóteses da equipe registradas em
> [analise-contextual.md](analise-contextual.md). Nenhuma delas resulta de entrevista com
> usuários reais. Os nomes são fictícios e coincidem com as contas de demonstração do
> sistema (`DadosDeDemonstracao`), para que os cenários possam ser reproduzidos na
> aplicação. A validação com usuários e o questionário SUS são a etapa seguinte.

---

## Ana Carolina Souza, cliente

| | |
|---|---|
| **Idade** | 38 anos |
| **Ocupação** | Analista administrativa, trabalho presencial em horário comercial |
| **Onde mora** | Apartamento no Setor Sul, Goiânia |
| **Conta de demonstração** | `ana@exemplo.com` (senha `obrix123`) |

**Objetivos**

- Resolver pequenos reparos da casa sem perder um dia de trabalho procurando alguém.
- Contratar alguém com boas referências, de preferência que já tenha atendido vizinhos.
- Saber de antemão se vai pagar algo à plataforma (não quer surpresas).

**Frustrações**

- Indicações de grupos de mensagem que não respondem ou somem depois do orçamento.
- Aplicativos que pedem cadastro completo antes de mostrar qualquer profissional.
- Não saber se o preço cobrado é justo; sentir que pode ser enganada.

**Uso de tecnologia**

- Usa o celular para quase tudo: banco, compras e mensagens. Prefere resolver no
  intervalo do almoço.
- Confortável com formulários curtos; abandona os longos.
- Lê avisos quando estão perto do botão que vai apertar, e ignora textos longos no topo
  da página.

**Cenário de uso**

> Numa segunda-feira à noite, o chuveiro elétrico do banheiro social queima. No dia
> seguinte, no almoço, Ana abre o Obrix no celular, escolhe **Elétrica** e o bairro
> **Setor Sul**, e compara os eletricistas pelas avaliações. Abre o perfil de Carlos
> Mendes (12 anos de experiência) e vê que o telefone só aparece depois do aceite.
> Entra na conta e solicita a "Instalação de chuveiro elétrico 220v no banheiro social"
> para dali a dois dias. Carlos aceita, e os contatos são liberados. Depois do serviço,
> Ana paga R$ 280,00 a Carlos diretamente. No painel, o cartão aparece em destaque como
> "Aguardando confirmação", com "Valor: você informa ao confirmar". Ela informa R$ 280,00,
> o serviço é concluído e ela avalia Carlos com 5 estrelas.

---

## Carlos Mendes, prestador

| | |
|---|---|
| **Idade** | 45 anos |
| **Ocupação** | Eletricista autônomo há 12 anos, sem CNPJ |
| **Onde atua** | Setor Sul e Setor Bueno, Goiânia |
| **Conta de demonstração** | `carlos@exemplo.com` (senha `obrix123`) |

**Objetivos**

- Ter uma agenda mais previsível, com clientes novos além das indicações.
- Não pagar por contato que não vira serviço.
- Acompanhar quanto deve de comissão sem precisar de planilha.

**Frustrações**

- Plataformas que cobram para ver o contato do cliente, mesmo quando o cliente desiste.
- Clientes que "somem" depois do serviço, sem retorno nem avaliação.
- Telas cheias de texto pequeno, difíceis de ler na rua e sob o sol.

**Uso de tecnologia**

- Celular Android intermediário, usado entre um serviço e outro; plano de dados limitado.
- Usa bem o WhatsApp e o aplicativo do banco; pouca paciência para cadastros longos.
- Prefere botões grandes e ações em um toque.

**Cenário de uso**

> No intervalo entre dois serviços, Carlos abre o painel do profissional e vê o contador
> na aba **Solicitações**: há um pedido novo de Ana, no Setor Sul. (O sistema grava
> notificações, mas ainda não as mostra na interface; por isso ele precisa abrir o
> painel para descobrir.) Lê a descrição e toca em **Aceitar**. O modal
> explica que telefone e e-mail serão liberados sem custo. No dia combinado, ao chegar,
> toca em **Iniciar execução**. Terminado o serviço, recebe R$ 280,00 e toca em
> **Registrar conclusão**, informando o valor final. O modal avisa que Ana terá 7 dias
> para confirmar e que a comissão só nasce na confirmação. Dias depois, na aba
> **Cobranças**, Carlos vê a comissão de R$ 28,00 (10%) registrada sobre o serviço.

---

## Como as personas orientam o projeto

| Necessidade da persona | Decisão de interface no Obrix | Onde |
|---|---|---|
| Ana não quer cadastro antes de ver profissionais | Busca e perfil públicos; a conta só é exigida ao solicitar | `pagina-inicio.js`, `pagina-perfil.js` |
| Ana desconfia de desconhecidos | Avaliações só de serviços concluídos; profissionais sem nota em bloco separado | `pagina-resultados.js` |
| Ana quer saber se vai pagar algo | "Custo para você: zero" no formulário de solicitação | `solicitar.html` |
| Carlos não quer pagar por contato | "Receber solicitações não tem custo" no painel; contato liberado sem custo no aceite | `painel-prestador.html`, modal de aceite |
| Carlos usa o celular na rua | Botões com no mínimo 44 px de altura; ações direto no cartão | `obrix.css`, `pagina-painel-prestador.js` |
| Carlos quer acompanhar a comissão | Indicadores no topo e aba Cobranças com totais | `pagina-painel-prestador.js` |
