package com.example.aicode.controller;

import com.example.aicode.dto.CodeRequest;
import com.example.aicode.dto.CodeResponse;
import com.example.aicode.model.CodeReviewRequest;
import com.example.aicode.model.CodeReviewResponse;
import com.example.aicode.repository.CodeReviewRequestRepository;
import com.example.aicode.repository.CodeReviewResponseRepository;
import com.example.aicode.service.AIReviewService;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/review")
@CrossOrigin(origins = "*")
public class CodeReviewController {

    @Autowired
    private AIReviewService aiReviewService;

    @Autowired
    private CodeReviewRequestRepository requestRepository;

    @Autowired
    private CodeReviewResponseRepository responseRepository;


    @PostMapping
    @ResponseBody
    @Transactional
    public CodeResponse reviewCode(@RequestBody CodeRequest requestDto) {
        // Pass language and code to AI review service
        String feedback = aiReviewService.getCodeReview(requestDto.getLanguage(), requestDto.getCode());

        // Save CodeReviewRequest entity
        CodeReviewRequest request = new CodeReviewRequest();
        request.setCode(requestDto.getCode());
        request.setLanguage(requestDto.getLanguage());
        request.setRequestTime(LocalDateTime.now());
        requestRepository.save(request);

        // Save CodeReviewResponse entity
        CodeReviewResponse response = new CodeReviewResponse();
        response.setReviewFeedback(feedback);
        response.setResponseTime(LocalDateTime.now());
        response.setRequest(request);
        responseRepository.save(response);

        // Return response DTO
        return new CodeResponse(feedback);
    
    }
}
