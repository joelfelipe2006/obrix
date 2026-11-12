'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { criarJanela, resposta } = require('./ambiente');

test('desativar e excluir chamam as rotas da conta com POST', async () => {
  const janela = criarJanela({ fetch: () => resposta(204) });
  await janela.Api.iniciar();

  await janela.Api.desativarConta();
  await janela.Api.excluirConta();

  const chamadas = janela.chamadas.filter((c) => !c.url.endsWith('/auth/sessao'));
  assert.deepEqual(chamadas.map((c) => `${c.metodo} ${c.url}`),
    ['POST /api/v1/conta/desativar', 'POST /api/v1/conta/excluir']);
});

test('modo demonstracao: desativar tambem encerra a sessao local', async () => {
  let saiu = false;
  const janela = criarJanela({ protocolo: 'file:', demo: { sessao: () => null, sair: () => { saiu = true; } } });
  await janela.Api.iniciar();

  await janela.Api.desativarConta();
  assert.equal(saiu, true);
});
