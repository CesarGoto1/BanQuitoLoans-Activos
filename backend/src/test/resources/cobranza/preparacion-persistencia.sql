DO $$
BEGIN
    IF current_database() <> 'banquito_recaudaciones_test' THEN
        RAISE EXCEPTION 'Este archivo solo puede ejecutarse en la base aislada de pruebas';
    END IF;
END $$;

INSERT INTO "cierresDia" ("fechaNegocio", "estadoCierre", "usuarioCreacion")
VALUES ('2026-10-31', 'PENDIENTE', 'test');

INSERT INTO "codigosValidacion" ("codigoValidacion", "nivelValidacion", "descripcionValidacion", "usuarioCreacion")
SELECT 'ARC-00' || numero, 'ARCHIVO', 'Validacion de archivo de prueba', 'test'
FROM generate_series(1, 7) numero;

INSERT INTO "codigosValidacion" ("codigoValidacion", "nivelValidacion", "descripcionValidacion", "usuarioCreacion")
SELECT 'REG-00' || numero, 'REGISTRO', 'Validacion de registro de prueba', 'test'
FROM generate_series(1, 7) numero;

CREATE TABLE test_payment_effect (
    id_registro integer NOT NULL UNIQUE REFERENCES "registrosRecaudacion" ("idRegistro")
);
