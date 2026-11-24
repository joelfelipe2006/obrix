-- Valor que o cliente informa ter pago ao confirmar a conclusao.
-- Nulo enquanto o cliente nao confirma e quando a confirmacao e automatica.
ALTER TABLE solicitacao ADD COLUMN valor_confirmado_cliente DECIMAL(12,2);
