package com.iitsm.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AddNoteRequest(
        @NotBlank(message = "Note text cannot be blank")
        String noteText,

        boolean internalOnly) {}

