package com.example.aicode.repository;
import com.example.aicode.model.CodeReviewRequest;


import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeReviewRequestRepository extends JpaRepository<CodeReviewRequest, Long> {
}