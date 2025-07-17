package com.example.aicode.dto;

import jakarta.validation.constraints.NotBlank;

public class CodeRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String language;

    public CodeRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "CodeRequest{" +
               "code='" + code + '\'' +
               ", language='" + language + '\'' +
               '}';
    }
}
