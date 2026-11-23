package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.model.UtilityRun;
import com.newgen.loanorigination.model.enums.UtilityResult;

import java.time.LocalDateTime;
import java.util.List;

public interface UtilityRunDAO {
    Long startRun(String utilityName, LocalDateTime startedAt);
    void completeRun(Long runId, UtilityResult result, int applicationsProcessed, int alertsCreated,
                     String errorMessage, LocalDateTime completedAt);
    List<UtilityRun> findAll();
}
