/*
 * Carrega os scripts reais do front-end (src/main/resources/static/js) num
 * contexto isolado do Node, com window, location e fetch simulados.
 * Sem dependencias: roda com `node --test src/test/js`.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const PASTA_JS = path.join(__dirname, '..', '..', 'main', 'resources', 'static', 'js');

/** Resposta minima no formato do fetch. */
function resposta(status, corpo) {
  const texto = corpo === undefined ? '' : JSON.stringify(corpo);
  return Promise.resolve({
    status,
    ok: status >= 200 && status < 300,
    text: () => Promise.resolve(texto),
  });
}

/**
 * Cria uma "janela" com os scripts pedidos ja executados.
 * @param {object} opcoes
 *   protocolo  - 'http:' (servido pelo Spring) ou 'file:' (aberto do disco)
 *   fetch      - funcao (url, config) => Promise de resposta
 *   demo       - objeto que faz o papel de window.ObrixDemo
 *   scripts    - nomes dos arquivos a carregar, em ordem
 */
function criarJanela(opcoes) {
  const chamadas = [];
  const janela = {
    location: { protocol: opcoes.protocolo || 'http:', href: '' },
    ObrixDemo: opcoes.demo || {},
    fetch: (url, config) => {
      chamadas.push({ url, metodo: (config && config.method) || 'GET', corpo: config && config.body });
      return opcoes.fetch ? opcoes.fetch(url, config) : resposta(204);
    },
    console,
  };
  janela.window = janela;
  vm.createContext(janela);
  for (const nome of opcoes.scripts || ['api.js']) {
    const codigo = fs.readFileSync(path.join(PASTA_JS, nome), 'utf8');
    vm.runInContext(codigo, janela, { filename: nome });
  }
  janela.chamadas = chamadas;
  return janela;
}

/** Objetos criados dentro do vm tem outro prototype: compara-se so o conteudo. */
function plano(valor) {
  return JSON.parse(JSON.stringify(valor));
}

module.exports = { criarJanela, resposta, plano };
