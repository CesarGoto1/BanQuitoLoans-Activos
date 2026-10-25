package com.grupob.bancobanquito.origination.internal.exceptions;
import com.grupob.bancobanquito.origination.internal.entities.*;
import com.grupob.bancobanquito.origination.internal.repositories.*;
import com.grupob.bancobanquito.origination.internal.services.*;
import com.grupob.bancobanquito.origination.internal.controllers.*;
import com.grupob.bancobanquito.origination.internal.exceptions.*;

public class InvalidLoanRequestStatusException extends RuntimeException {

    public InvalidLoanRequestStatusException(String mensaje) {
        super(mensaje);
    }
}

















