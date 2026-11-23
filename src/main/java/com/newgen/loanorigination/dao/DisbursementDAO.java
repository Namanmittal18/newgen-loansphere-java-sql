package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.model.Disbursement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DisbursementDAO {
    Disbursement save(Disbursement disbursement);
    Optional<Disbursement> findByApplicationId(Long applicationId);
    Optional<Disbursement> findByReference(String reference);
    boolean existsByApplicationId(Long applicationId);
    boolean existsByReference(String reference);
    BigDecimal sumTotalDisbursedAmount();
    long countTotal();
    List<Disbursement> findAll();
}
