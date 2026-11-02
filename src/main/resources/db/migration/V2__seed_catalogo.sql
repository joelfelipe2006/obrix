-- Carga inicial do catalogo (RF07).
-- Nao existe perfil administrativo no escopo: as categorias e as regioes
-- entram por aqui, versionadas junto do codigo.

INSERT INTO categoria_servico (nome, descricao) VALUES
  ('Eletrica',    'Instalacoes e reparos eletricos residenciais'),
  ('Hidraulica',  'Encanamento, vazamentos e instalacoes hidraulicas'),
  ('Pintura',     'Pintura interna e externa'),
  ('Alvenaria',   'Pequenas obras, reparos e acabamento'),
  ('Limpeza',     'Diarista e limpeza pesada'),
  ('Jardinagem',  'Poda, corte de grama e manutencao de jardim'),
  ('Marcenaria',  'Montagem e reparo de moveis'),
  ('Refrigeracao','Instalacao e manutencao de ar-condicionado');

INSERT INTO regiao_atuacao (cidade, bairro) VALUES
  ('Goiania', 'Setor Sul'),
  ('Goiania', 'Setor Bueno'),
  ('Goiania', 'Setor Oeste'),
  ('Goiania', 'Setor Marista'),
  ('Goiania', 'Jardim America'),
  ('Goiania', 'Setor Campinas'),
  ('Goiania', 'Setor Pedro Ludovico'),
  ('Aparecida de Goiania', 'Centro'),
  ('Aparecida de Goiania', 'Garavelo'),
  ('Senador Canedo', 'Centro');
