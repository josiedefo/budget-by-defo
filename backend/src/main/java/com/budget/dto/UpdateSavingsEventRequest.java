package com.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateSavingsEventRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate eventDate;

    private String note;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
