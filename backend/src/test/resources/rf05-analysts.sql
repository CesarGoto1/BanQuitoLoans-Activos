INSERT INTO "analistas" ("cedulaAnalista", "nombreAnalista", "usuarioCreacion")
VALUES ('1799999901', 'Analista de prueba RF05 A', 'rf05-test'),
       ('1799999902', 'Analista de prueba RF05 B', 'rf05-test')
ON CONFLICT ("cedulaAnalista") DO NOTHING;
