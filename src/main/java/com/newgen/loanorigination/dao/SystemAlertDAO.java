package com.newgen.loanorigination.dao;

import com.newgen.loanorigination.model.SystemAlert;
import com.newgen.loanorigination.model.enums.AlertType;

import java.util.List;

public interface SystemAlertDAO {
    boolean saveIfAbsent(SystemAlert alert);
    boolean exists(Long applicationId, AlertType alertType);
    List<SystemAlert> findByApplicationId(Long applicationId);
    List<SystemAlert> findAll();
    long countTotal();
}
