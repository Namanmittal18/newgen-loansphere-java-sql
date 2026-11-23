package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.model.ApplicationHistory;

import java.util.List;

public interface ApplicationHistoryDAO {
    void save(ApplicationHistory history);
    List<ApplicationHistory> findByApplicationId(Long applicationId);
    List<ApplicationHistory> findRecent(int limit);
}
