package com.grupob.bancobanquito.access;

public record Identity(String identificacion, UserRole rol) {

    
    public String maskedIdentification() {
        if (identificacion == null || identificacion.length() != 10) {
            return "**********";
        }
        return identificacion.substring(0, 2) + "******" + identificacion.substring(8);
    }
}















