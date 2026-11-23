package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.model.CoreBankingRecord;

import java.util.List;
import java.util.Optional;

public interface CoreBankingDAO {
    CoreBankingRecord save(CoreBankingRecord record);
    Optional<CoreBankingRecord> findByApplicationId(Long applicationId);
    boolean existsByApplicationId(Long applicationId);
    List<CoreBankingRecord> findAll();
    long countTotal();
}
