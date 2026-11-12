/* Acesso (W13/M13).
   A mensagem de erro e sempre a mesma, exista ou nao o e-mail: resposta
   diferente permitiria descobrir quem tem conta na plataforma. */
(function () {
  'use strict';

  var form;

  UI.iniciarPagina().then(function (inicio) {
    if (inicio.sessao) { irParaDestino(inicio.sessao); return; }

    form = document.getElementById('form-entrar');
    form.addEventListener('submit', entrar);

    if (Api.emDemonstracao()) {
      var caixa = document.getElementById('contas-demo');
      caixa.hidden = false;
      caixa.querySelectorAll('[data-entrar]').forEach(function (botao) {
        botao.addEventListener('click', function () {
          document.getElementById('email').value = botao.dataset.entrar;
          document.getElementById('senha').value = 'obrix123';
          form.requestSubmit();
        });
      });
    }
  }).catch(UI.erro);

  function entrar(evento) {
    evento.preventDefault();
    UI.limparErros(form);

    var dados = UI.lerFormulario(form);
    var botao = form.querySelector('button[type="submit"]');

    if (!dados.email || !dados.senha) {
      UI.toast('Informe e-mail e senha.', 'erro');
      return;
    }

    UI.ocupado(botao, true, 'Entrando...');
    Api.login(dados.email, dados.senha)
      .then(irParaDestino)
      .catch(function (e) { UI.ocupado(botao, false); UI.erro(e); });
  }

  function irParaDestino(sessao) {
    var destino = destinoSeguro(UI.parametro('destino'));
    if (destino) { window.location.href = destino; return; }
    window.location.href = sessao.perfil === 'CLIENTE'
      ? 'painel-cliente.html' : 'painel-prestador.html';
  }

  /**
   * O parametro "destino" vem da URL, entao qualquer um pode monta-lo. So vale
   * um caminho relativo do proprio site: sem esquema (javascript:, http:...),
   * sem "//" (outro dominio), sem barra inicial e sem barra invertida, que
   * alguns navegadores tratam como "/". Qualquer outro valor e descartado.
   */
  function destinoSeguro(destino) {
    if (!destino) return null;
    var valor = String(destino).trim();
    if (!valor) return null;
    if (/^[a-z][a-z0-9+.\-]*:/i.test(valor)) return null;
    if (valor.indexOf('//') !== -1) return null;
    if (valor.indexOf('\\') !== -1) return null;
    if (valor.charAt(0) === '/') return null;
    if (/[\u0000-\u001f]/.test(valor)) return null;
    return valor;
  }
})();
