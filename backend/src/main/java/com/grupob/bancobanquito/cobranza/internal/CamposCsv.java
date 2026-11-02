package com.grupob.bancobanquito.cobranza.internal;

import java.util.ArrayList;
import java.util.List;

final class CamposCsv {

    private CamposCsv() {
    }

    static List<String> leer(String linea) {
        return leerPrimeros(linea, Integer.MAX_VALUE);
    }

    static List<String> leerPrimeros(String linea, int limite) {
        List<String> campos = new ArrayList<>();
        int posicion = 0;
        while (true) {
            StringBuilder campo = new StringBuilder();
            if (posicion < linea.length() && linea.charAt(posicion) == '"') {
                posicion++;
                boolean cerrado = false;
                while (posicion < linea.length()) {
                    char caracter = linea.charAt(posicion++);
                    if (caracter != '"') {
                        campo.append(caracter);
                    } else if (posicion < linea.length() && linea.charAt(posicion) == '"') {
                        campo.append('"');
                        posicion++;
                    } else {
                        cerrado = true;
                        break;
                    }
                }
                if (!cerrado || posicion < linea.length() && linea.charAt(posicion) != ',') {
                    throw new IllegalArgumentException("Campo CSV entre comillas inválido.");
                }
            } else {
                while (posicion < linea.length() && linea.charAt(posicion) != ',') {
                    char caracter = linea.charAt(posicion++);
                    if (caracter == '"') {
                        throw new IllegalArgumentException("Comillas fuera de un campo delimitado.");
                    }
                    campo.append(caracter);
                }
            }
            campos.add(campo.toString());
            if (posicion == linea.length() || campos.size() == limite) {
                return List.copyOf(campos);
            }
            posicion++;
        }
    }
}
