'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { criarJanela, resposta } = require('./ambiente');

/**
 * Referencia exata, em BigInt, da conta do back-end:
 * valor(escala 2) x percentual / 100 -> escala 2, HALF_UP (Cobranca.calcularComissao).
 */
function referencia(valorTexto, percentualTexto) {
  const centavos = BigInt(Math.round(Number(valorTexto) * 100));
  const centesimos = BigInt(Math.round(Number(percentualTexto) * 100));
  const produto = centavos * centesimos;          // comissao em centavos x 10000
  const comissao = (produto + 5000n) / 10000n;    // HALF_UP para positivos
  return Number(comissao) / 100;
}

const api = () => criarJanela({ scripts: ['api.js'] }).Api;

test('casos de referencia iguais aos do back-end', () => {
  const { calcularComissao } = api();
  assert.equal(calcularComissao(280, 10), 28);        // caso dos mockups
  assert.equal(calcularComissao('280.00', '10.00'), 28);
  assert.equal(calcularComissao(99.95, 10), 10);       // 9,995 -> 10,00 (HALF_UP)
  assert.equal(calcularComissao(0.05, 10), 0.01);      // 0,005 -> 0,01
  assert.equal(calcularComissao(0.04, 10), 0);         // 0,004 -> 0,00
  assert.equal(calcularComissao(150, 12.5), 18.75);
  assert.equal(calcularComissao(1234567.89, 10), 123456.79);
});

test('onde a conta antiga do painel (valor * 0.10 com toFixed) errava, a nova acerta', () => {
  const { calcularComissao } = api();
  // 1,45 x 10% = 0,145 -> HALF_UP 0,15. Em ponto flutuante, 1.45 * 0.10 vale
  // 0.14499999..., e o toFixed(2) do painel mostrava R$ 0,14.
  assert.equal(calcularComissao(1.45, 10), 0.15);
  assert.equal(Number((1.45 * 0.10).toFixed(2)), 0.14);
  assert.equal(calcularComissao(2.05, 10), 0.21);
  assert.equal(Number((2.05 * 0.10).toFixed(2)), 0.2);
});

test('20 mil valores aleatorios batem com a referencia exata', () => {
  const { calcularComissao } = api();
  const percentuais = ['10.00', '12.50', '7.75', '15.00', '0.01'];
  let semente = 42;
  const aleatorio = () => (semente = (semente * 1103515245 + 12345) % 2147483648) / 2147483648;
  for (let i = 0; i < 20000; i++) {
    const valor = (Math.floor(aleatorio() * 10_000_000) / 100).toFixed(2); // ate R$ 100 mil
    const pct = percentuais[i % percentuais.length];
    assert.equal(calcularComissao(valor, pct), referencia(valor, pct), `valor=${valor} pct=${pct}`);
  }
});

test('regras vem do /catalogo no modo servidor', async () => {
  const janela = criarJanela({
    fetch: (url) => (url.endsWith('/auth/sessao') ? resposta(204)
      : resposta(200, { categorias: [], regioes: [], percentualComissao: 12.5, prazoConfirmacaoDias: 5 })),
  });
  await janela.Api.iniciar();
  const regras = await janela.Api.regras();
  assert.equal(regras.percentualComissao, 12.5);
  assert.equal(regras.prazoConfirmacaoDias, 5);
});

test('modo demonstracao: os mesmos padroes do servidor (10% e 7 dias)', async () => {
  const janela = criarJanela({ protocolo: 'file:', scripts: ['demo.js', 'api.js'] });
  janela.localStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
  await janela.Api.iniciar().catch(() => {});
  const regras = await janela.Api.regras();
  assert.equal(regras.percentualComissao, 10);
  assert.equal(regras.prazoConfirmacaoDias, 7);
});

test('textos: percentual e prazo formatados para a tela', () => {
  const { UI } = criarJanela({ scripts: ['ui.js'] });
  assert.equal(UI.percentual(10), '10%');
  assert.equal(UI.percentual('10.00'), '10%');
  assert.equal(UI.percentual(12.5), '12,5%');
  assert.equal(UI.dias(7), '7 dias');
  assert.equal(UI.dias(1), '1 dia');
});
