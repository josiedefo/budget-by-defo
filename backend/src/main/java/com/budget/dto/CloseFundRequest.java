package com.budget.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CloseFundRequest {
    /** Effective closing date; defaults to today. Must not be in the future. */
    private LocalDate closedDate;
}
