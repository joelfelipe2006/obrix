'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { criarJanela, resposta, plano } = require('./ambiente');

const PAGINA = { itens: [{ id: 1 }], pagina: 0, tamanho: 20, total: 1, temMais: false };

test('servidor: as listas chegam no formato de pagina da API', async () => {
  const janela = criarJanela({
    fetch: (url) => {
      if (url.endsWith('/auth/sessao')) return resposta(204);
      if (url.includes('/solicitacoes/extrato')) {
        return resposta(200, { pagina: PAGINA, totalComissao: 10, totalServicos: 100 });
      }
      return resposta(200, PAGINA);
    },
  });
  await janela.Api.iniciar();

  assert.deepEqual(plano(await janela.Api.minhasSolicitacoes()), PAGINA);
  assert.deepEqual(plano(await janela.Api.buscarPrestadores({ categoriaId: 1 })), PAGINA);
  assert.deepEqual(plano((await janela.Api.extrato()).pagina), PAGINA);
});

test('demonstracao: listas soltas viram uma pagina unica no mesmo formato', async () => {
  const janela = criarJanela({
    protocolo: 'file:',
    demo: {
      sessao: () => null,
      minhasSolicitacoes: () => [{ id: 1 }, { id: 2 }],
      buscarPrestadores: () => [{ id: 9 }],
      extrato: () => ({ itens: [{ solicitacaoId: 1 }], totalComissao: 28, totalServicos: 280 }),
    },
  });
  await janela.Api.iniciar();

  assert.deepEqual(plano(await janela.Api.minhasSolicitacoes()),
    { itens: [{ id: 1 }, { id: 2 }], pagina: 0, tamanho: 2, total: 2, temMais: false });
  assert.equal((await janela.Api.buscarPrestadores({ categoriaId: 1 })).itens[0].id, 9);

  const extrato = await janela.Api.extrato();
  assert.equal(extrato.pagina.itens.length, 1);
  assert.equal(extrato.pagina.temMais, false);
  assert.equal(extrato.totalComissao, 28);
});
