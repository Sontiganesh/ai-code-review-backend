package com.example.aicode.dto;

public class CodeResponse {
    private String feedback;

     
    public CodeResponse() {
    }

    public CodeResponse(String feedback) {
        this.feedback = feedback;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
