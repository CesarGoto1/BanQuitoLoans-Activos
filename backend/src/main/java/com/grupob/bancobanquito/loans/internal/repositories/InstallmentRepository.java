package com.grupob.bancobanquito.loans.internal.repositories;
import com.grupob.bancobanquito.loans.internal.entities.*;
import com.grupob.bancobanquito.loans.internal.repositories.*;
import com.grupob.bancobanquito.loans.internal.services.*;
import com.grupob.bancobanquito.loans.internal.controllers.*;
import com.grupob.bancobanquito.loans.internal.exceptions.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentRepository extends JpaRepository<Installment, Integer> {

    List<Installment> findByIdOperacionOrderByNumeroCuotaAsc(Integer idOperacion);
}
















