package com.telegram.expense_tracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Getter @Setter
@Table(name = "expense", schema = "public")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "currency")
    private String currency;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "remark")
    private String remark;

    @Column(name = "created_on")
    private Timestamp createdOn = new Timestamp(System.currentTimeMillis());

    public Expense() {}

    public Expense (Long userId, String currency, BigDecimal amount, String remark) {
        this.userId = userId;
        this.currency = currency;
        this.amount = amount;
        this.remark = remark;
    }

}
