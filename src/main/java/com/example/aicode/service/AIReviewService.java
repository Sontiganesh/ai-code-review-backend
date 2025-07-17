package com.example.aicode.service;

import jakarta.annotation.PostConstruct;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AIReviewService {

    private static final Logger logger = LoggerFactory.getLogger(AIReviewService.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${huggingface.api.key}")
    private String huggingFaceApiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String HUGGINGFACE_URL = "https://api-inference.huggingface.co/models/microsoft/phi-2";

    private final RestTemplate restTemplate;

    public AIReviewService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void logApiKeysOnStartup() {
        logger.info("DEBUG_CONFIG: Gemini API Key loaded: {}",
                geminiApiKey != null && geminiApiKey.length() > 5 ? "******" + geminiApiKey.substring(geminiApiKey.length() - 5) : "NOT SET or TOO SHORT");
        logger.info("DEBUG_CONFIG: Hugging Face API Key loaded: {}",
                huggingFaceApiKey != null && huggingFaceApiKey.length() > 5 ? "******" + huggingFaceApiKey.substring(huggingFaceApiKey.length() - 5) : "NOT SET or TOO SHORT");
    }

    private String detectLanguage(String code) {
        if (code.contains("def ") && code.contains(":") && code.contains("print(")) return "python";
        if (code.contains("function ") || code.contains("console.log(")) return "javascript";
        if (code.contains("public class") || code.contains("System.out.println")) return "java";
        return "unknown";
    }

    private String buildPrompt(String language, String code) {
        String exampleCode;
        String exampleReview;

        switch (language.toLowerCase()) {
            case "python" -> {
                exampleCode = """
                    def divide_numbers():
                        a = 10
                        b = 0
                        print("Result:", a / b)
                    divide_numbers()
                    """;
                exampleReview = """
                    [BUGS]
                    - **ZeroDivisionError**: Division by zero.

                    [COMMENTS]
                    def divide_numbers():  # Function to divide numbers
                        a = 10             # a is 10
                        b = 0              # b is 0
                        print("Result:", a / b)  # Division by zero

                    [EXPLANATION]
                    Code divides 10 by 0, causing runtime crash.

                    [IMPROVEMENTS]
                    - Check b != 0 before division.
                    - Use try-except block.
                    """;
            }
            case "javascript" -> {
                exampleCode = """
                    function calculateTotal() {
                        let price = 100;
                        console.log("Total:", price + tax);
                    }
                    calculateTotal();
                    """;
                exampleReview = """
                    [BUGS]
                    - **ReferenceError**: `tax` is not defined.

                    [COMMENTS]
                    function calculateTotal() {  // Function to calculate
                        let price = 100;
                        console.log("Total:", price + tax);  // tax is not defined
                    }

                    [EXPLANATION]
                    Adds price and tax but tax is missing.

                    [IMPROVEMENTS]
                    - Define `tax` before usage.
                    """;
            }
            default -> {
                exampleCode = """
                    public class BuggyExample {
                        public static void main(String[] args) {
                            int a = 5;
                            int b = 0;
                            System.out.println("Result: " + (a / b));
                        }
                    }
                    """;
                exampleReview = """
                    [BUGS]
                    - **ArithmeticException**: Division by zero.

                    [COMMENTS]
                    public class BuggyExample {  // Java class
                        public static void main(String[] args) {
                            int a = 5;
                            int b = 0;
                            System.out.println("Result: " + (a / b));  // Crashes here
                        }
                    }

                    [EXPLANATION]
                    Java app crashes due to division by 0.

                    [IMPROVEMENTS]
                    - Add validation for `b != 0`
                    - Add try-catch block
                    """;
            }
        }

        return """
            You are an expert code reviewer for %s language.
            Your task is to analyze code and perform the following:

            1. 🔍 Detect and describe any bugs.
            2. 💬 Add inline comments (brief and helpful).
            3. 📖 Provide a short explanation of what the code is doing.
            4. 🧠 Suggest improvements or optimizations.

            Use the format:
            [BUGS]
            [COMMENTS]
            [EXPLANATION]
            [IMPROVEMENTS]

            ---
            INPUT CODE EXAMPLE:
            %s

            OUTPUT REVIEW EXAMPLE:
            %s
            ---

            Now, review this code:
            Code:
            %s
            """.formatted(language, exampleCode, exampleReview, code);
    }

    public String getCodeReview(String dropdownLanguage, String code) {
        String detectedLanguage = detectLanguage(code);
        boolean mismatch = !dropdownLanguage.equalsIgnoreCase(detectedLanguage)
                && !detectedLanguage.equals("unknown");

        String prompt = buildPrompt(dropdownLanguage, code);
        logger.debug("Prompt for review:\n{}", prompt);

        StringBuilder finalReview = new StringBuilder();

        if (mismatch) {
            finalReview.append("⚠️ Warning: Language mismatch detected! You selected '")
                    .append(dropdownLanguage)
                    .append("' but the code looks like '")
                    .append(detectedLanguage)
                    .append("'.\n\n");
        }

        try {
            logger.info("Trying Hugging Face API...");
            String hfResponse = getReviewFromHuggingFace(prompt);
            finalReview.append("[HuggingFace]\n").append(hfResponse);
            return finalReview.toString();
        } catch (Exception e) {
            logger.warn("Hugging Face failed: {}", e.getMessage());
        }

        try {
            logger.info("Falling back to Gemini API...");
            String geminiResponse = getReviewFromGemini(prompt);
            finalReview.append("[Gemini]\n").append(geminiResponse);
            return finalReview.toString();
        } catch (Exception e) {
            logger.error("Gemini also failed: {}", e.getMessage());
            return finalReview.append("❌ All AI models failed. Please try again later.").toString();
        }
    }

    // ⬇️ Keep getReviewFromHuggingFace, getReviewFromGemini, cleanUpText unchanged

    private String getReviewFromHuggingFace(String prompt) {
        Map<String, String> requestBody = Map.of("inputs", prompt);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (huggingFaceApiKey == null || huggingFaceApiKey.isEmpty()) throw new IllegalArgumentException("Hugging Face API Key is not configured.");
        headers.setBearerAuth(huggingFaceApiKey);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(HUGGINGFACE_URL, HttpMethod.POST, request, String.class);
        if (response.getStatusCode() != HttpStatus.OK) throw new RuntimeException("Hugging Face API failed: " + response.getBody());
        JSONArray array = new JSONArray(response.getBody());
        if (array.length() > 0 && array.getJSONObject(0).has("generated_text")) {
            return cleanUpText(array.getJSONObject(0).getString("generated_text"));
        }
        throw new RuntimeException("Invalid Hugging Face response: " + response.getBody());
    }

    private String getReviewFromGemini(String prompt) {
        Map<String, Object> content = Map.of("role", "user", "parts", List.of(Map.of("text", prompt)));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = GEMINI_URL + "?key=" + geminiApiKey;
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) throw new IllegalArgumentException("Gemini API Key is not configured.");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, request, new ParameterizedTypeReference<>() {});
        var candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
        if (candidates != null && !candidates.isEmpty()) {
            var parts = (List<Map<String, Object>>) ((Map<String, Object>) candidates.get(0).get("content")).get("parts");
            if (parts != null && !parts.isEmpty()) {
                return cleanUpText((String) parts.get(0).get("text"));
            }
        }
        throw new RuntimeException("Invalid Gemini response: " + response.getBody());
    }

    private String cleanUpText(String rawText) {
        return rawText == null ? "" : rawText
                .replaceAll("```[a-zA-Z]*", "")
                .replaceAll("```", "")
                .replaceAll("\\\\n", "\n")
                .trim();
    }
}
