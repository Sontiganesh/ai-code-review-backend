package com.example.aicode.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_review_responses")
public class CodeReviewResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false) // in case feedback is long
    private String reviewFeedback;

    @Column(nullable = false)
    private LocalDateTime responseTime;

    @OneToOne
    @JoinColumn(name = "request_id", referencedColumnName = "id")
    private CodeReviewRequest request;

    public CodeReviewResponse() {
        // No-arg constructor for JPA
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReviewFeedback() {
        return reviewFeedback;
    }

    public void setReviewFeedback(String reviewFeedback) {
        this.reviewFeedback = reviewFeedback;
    }

    public LocalDateTime getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(LocalDateTime responseTime) {
        this.responseTime = responseTime;
    }

    public CodeReviewRequest getRequest() {
        return request;
    }

    public void setRequest(CodeReviewRequest request) {
        this.request = request;
    }
}
