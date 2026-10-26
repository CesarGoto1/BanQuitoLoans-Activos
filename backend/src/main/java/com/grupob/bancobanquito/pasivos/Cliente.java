package com.grupob.bancobanquito.pasivos;

public record Cliente(
        Integer idCliente,
        String cedula,
        String nombres,
        String apellidos) {
}
