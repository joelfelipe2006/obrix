'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { criarJanela, resposta } = require('./ambiente');

/** ObrixDemo que acusa qualquer uso: servida pelo servidor, a demonstracao nao pode ser tocada. */
function demoProibida() {
  const proibido = () => { throw new Error('a demonstracao nao deveria ser usada'); };
  return { sessao: proibido, minhasSolicitacoes: proibido, catalogo: proibido };
}

test('aberta do disco (file:): usa a demonstracao sem chamar a API', async () => {
  const janela = criarJanela({ protocolo: 'file:', demo: { sessao: () => null } });
  const inicio = await janela.Api.iniciar();

  assert.equal(inicio.modo, 'demonstracao');
  assert.equal(janela.chamadas.length, 0);
});

test('servida pelo servidor e API fora do ar: erro SEM_CONEXAO, sem demonstracao', async () => {
  const janela = criarJanela({
    demo: demoProibida(),
    fetch: () => Promise.reject(new TypeError('Failed to fetch')),
  });

  await assert.rejects(janela.Api.iniciar(), (e) => e.codigo === 'SEM_CONEXAO' && /conectar/.test(e.mensagem));
  assert.equal(janela.Api.modo, 'servidor');
  assert.equal(janela.Api.emDemonstracao(), false);
});

test('servida pelo servidor e API respondendo 500: tambem SEM_CONEXAO, sem demonstracao', async () => {
  const janela = criarJanela({
    demo: demoProibida(),
    fetch: () => resposta(500, { codigo: 'ERRO_INTERNO', mensagem: 'x', campos: [] }),
  });

  await assert.rejects(janela.Api.iniciar(), (e) => e.codigo === 'SEM_CONEXAO');
  assert.equal(janela.Api.emDemonstracao(), false);
});

test('rede cai depois de iniciar: a chamada falha com SEM_CONEXAO e nao vai para a demonstracao', async () => {
  let online = true;
  const janela = criarJanela({
    demo: demoProibida(),
    fetch: () => (online ? resposta(204) : Promise.reject(new TypeError('Failed to fetch'))),
  });
  await janela.Api.iniciar();

  online = false;
  await assert.rejects(janela.Api.minhasSolicitacoes(), (e) => e.codigo === 'SEM_CONEXAO');
});

test('erro de regra da API continua chegando com o codigo dela, nao como falta de conexao', async () => {
  const janela = criarJanela({
    fetch: (url) => (url.endsWith('/auth/sessao') ? resposta(204)
      : resposta(422, { codigo: 'TRANSICAO_INVALIDA', mensagem: 'Nao pode.', campos: [] })),
  });
  await janela.Api.iniciar();

  await assert.rejects(janela.Api.acao(1, 'confirmar'), (e) => e.codigo === 'TRANSICAO_INVALIDA');
});
