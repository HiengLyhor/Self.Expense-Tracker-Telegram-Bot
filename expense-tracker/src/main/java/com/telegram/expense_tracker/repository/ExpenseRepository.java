package com.telegram.expense_tracker.repository;

import com.telegram.expense_tracker.model.Expense;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserId(Long id);

    @Modifying
    @Transactional
    @Query(
            value = "DELETE FROM expense r " +
                    "WHERE r.user_id = :userId " +
                    "AND (EXTRACT(YEAR FROM r.created_on) <> :year " +
                    "     OR EXTRACT(MONTH FROM r.created_on) <> :month)",
            nativeQuery = true
    )
    int deleteAllExceptCurrentMonth(@Param("userId") Long userId,
                                    @Param("year") int year,
                                    @Param("month") int month);

}
