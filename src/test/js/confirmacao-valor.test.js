'use strict';

// O demo.js duplica a maquina de estados: estes testes garantem que a
// confirmacao com o valor pago segue a mesma regra do back-end.

const test = require('node:test');
const assert = require('node:assert/strict');
const { criarJanela } = require('./ambiente');

function demoComCliente() {
  const janela = criarJanela({ protocolo: 'file:', scripts: ['api.js', 'demo.js'] });
  const demo = janela.ObrixDemo;
  demo.reiniciar();
  demo.login('ana@exemplo.com', 'obrix123');
  const aguardando = demo.minhasSolicitacoes().find((s) => s.status === 'AGUARDANDO_CONFIRMACAO');
  return { janela, demo, id: aguardando.id, valorFinal: aguardando.valorFinal };
}

test('mesmoValor: 280 e "280.00" sao iguais; 280 e 280.01 nao', () => {
  const { janela } = demoComCliente();
  assert.equal(janela.Api.mesmoValor(280, '280.00'), true);
  assert.equal(janela.Api.mesmoValor(280, 280.01), false);
});

test('demonstracao: valor igual conclui, grava o valor do cliente e gera a cobranca', () => {
  const { demo, id, valorFinal } = demoComCliente();
  assert.equal(valorFinal, null);

  const r = demo.acao(id, 'confirmar', { valorPago: '280' });

  assert.equal(r.status, 'CONCLUIDA');
  assert.equal(r.valorConfirmadoCliente, 280);
  assert.equal(r.cobranca.valorComissao, 28);
});

test('demonstracao: valor diferente contesta sem cobranca, com o motivo do back-end', () => {
  const { demo, id } = demoComCliente();

  const r = demo.acao(id, 'confirmar', { valorPago: 1500.5 });

  assert.equal(r.status, 'CONTESTADA');
  assert.equal(r.cobranca, null);
  assert.equal(r.valorConfirmadoCliente, null);
  const ultimo = r.historico[r.historico.length - 1];
  assert.equal(ultimo.autor, 'CLIENTE');
  assert.equal(ultimo.motivo,
    'Divergencia de valor: o valor informado pelo cliente (R$ 1.500,50) ' +
    'nao confere com o registrado pelo profissional.');
});

test('demonstracao: sem valor pago, ou com valor <= 0, recusa e nada muda', () => {
  const { demo, id } = demoComCliente();
  for (const corpo of [{}, { valorPago: 0 }, { valorPago: -10 }]) {
    assert.throws(() => demo.acao(id, 'confirmar', corpo), (e) => e.codigo === 'DADOS_INVALIDOS');
  }
  assert.equal(demo.solicitacao(id).status, 'AGUARDANDO_CONFIRMACAO');
});

// Confirmacao as cegas: o demo.js segue a mesma regra de exibicao do SolicitacaoMapper.

function sessaoDoPrestadorDe(janela, id) {
  const demo = janela.ObrixDemo;
  demo.sair();
  demo.login('carlos@exemplo.com', 'obrix123');
  return demo.solicitacao(id);
}

test('confirmacao as cegas: o cliente nao ve o valor do profissional antes de concluir', () => {
  const { janela, demo, id } = demoComCliente();

  assert.equal(demo.solicitacao(id).valorFinal, null);
  const naLista = demo.minhasSolicitacoes().find((s) => s.id === id);
  assert.equal(naLista.valorFinal, null);

  // O prestador continua vendo o proprio valor.
  assert.equal(sessaoDoPrestadorDe(janela, id).valorFinal, 280);
});

test('confirmacao as cegas: depois de concluida, o cliente ve os dois valores', () => {
  const { demo, id } = demoComCliente();

  const r = demo.acao(id, 'confirmar', { valorPago: 280 });

  assert.equal(r.valorFinal, 280);
  assert.equal(r.valorConfirmadoCliente, 280);
});

test('confirmacao as cegas: o motivo da divergencia nao traz o valor do profissional', () => {
  const { demo, id } = demoComCliente();

  const r = demo.acao(id, 'confirmar', { valorPago: 300 });

  assert.equal(r.status, 'CONTESTADA');
  assert.equal(r.valorFinal, null);
  assert.ok(!r.motivo.includes('280'), r.motivo);
});
