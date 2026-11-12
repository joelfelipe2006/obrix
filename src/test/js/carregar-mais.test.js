'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { criarJanela, resposta } = require('./ambiente');

const VAZIA = { itens: [], pagina: 0, tamanho: 20, total: 0, temMais: false };

test('api.js pede a pagina seguinte com ?pagina=N, e a primeira sem parametro', async () => {
  const janela = criarJanela({
    fetch: (url) => (url.endsWith('/auth/sessao') ? resposta(204)
      : url.includes('/extrato') ? resposta(200, { pagina: VAZIA, totalComissao: 0, totalServicos: 0 })
      : resposta(200, VAZIA)),
  });
  await janela.Api.iniciar();

  await janela.Api.minhasSolicitacoes(0);
  await janela.Api.minhasSolicitacoes(2);
  await janela.Api.extrato(0);
  await janela.Api.extrato(1);

  const urls = janela.chamadas.map((c) => c.url).filter((u) => !u.endsWith('/auth/sessao'));
  assert.deepEqual(urls, [
    '/api/v1/solicitacoes',
    '/api/v1/solicitacoes?pagina=2',
    '/api/v1/solicitacoes/extrato',
    '/api/v1/solicitacoes/extrato?pagina=1',
  ]);
});

test('botao "Carregar mais" so existe quando a API diz que ha mais itens', () => {
  const janela = criarJanela({ scripts: ['ui.js'] });

  assert.equal(janela.UI.botaoCarregarMais(false), '');
  assert.equal(janela.UI.botaoCarregarMais(undefined), '');

  const html = janela.UI.botaoCarregarMais(true);
  assert.match(html, /data-carregar-mais/);
  assert.match(html, /Carregar mais/);
});
