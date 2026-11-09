

package com.matcher.infrastructure.adapter.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matcher.domain.model.EvaluationResult;
import com.matcher.domain.model.JobPosting;
import com.matcher.domain.model.ResumeChunk;
import com.matcher.domain.port.out.LlmEvaluationPort;
import com.matcher.infrastructure.adapter.ai.dto.LlmEvaluationResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SpringAiLlmAdapter implements LlmEvaluationPort {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public SpringAiLlmAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        
        // ثبت ماژول‌های جاوا برای پشتیبانی کامل از Java Records و تنظیمات انعطاف‌پذیری Jackson
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public EvaluationResult evaluateMatch(JobPosting jobPosting, List<ResumeChunk> relevantChunks) {
        String context = relevantChunks.stream()
                .map(ResumeChunk::content)
                .collect(Collectors.joining("\n---\n"));

        UUID candidateId = relevantChunks.stream()
                .map(ResumeChunk::candidateId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(UUID.randomUUID());

        BeanOutputConverter<LlmEvaluationResponse> outputConverter = 
                new BeanOutputConverter<>(LlmEvaluationResponse.class, this.objectMapper);

        String userPrompt = """
            You are a Senior Technical Recruiter and AI Evaluator.
            Evaluate the candidate's resume context against the given job description.
            
            Job Title: {jobTitle}
            Required Skills: {requiredSkills}
            Min Experience: {minExperience} years
            Job Description: {jobDescription}
            
            Candidate Resume Context:
            {resumeContext}
            
            CONSTRAINTS:
            - overallMatchScore must be a number between 0.0 and 100.0 (e.g. 85.0, NOT 0.85).
            - Keep summary concise (max 2-3 sentences).
            - Ensure matchingSkills, missingSkills, and recommendations are valid JSON arrays of strings.
            
            {format}
            """;

        PromptTemplate promptTemplate = new PromptTemplate(userPrompt);
        Map<String, Object> params = Map.of(
                "jobTitle", jobPosting.title() != null ? jobPosting.title() : "",
                "requiredSkills", jobPosting.requiredSkills() != null ? String.join(", ", jobPosting.requiredSkills()) : "",
                "minExperience", jobPosting.minYearsExperience() != null ? jobPosting.minYearsExperience() : 0,
                "jobDescription", jobPosting.description() != null ? jobPosting.description() : "",
                "resumeContext", context,
                "format", outputConverter.getFormat()
        );

        Prompt prompt = promptTemplate.create(params);

        String rawResponse = chatClient.prompt(prompt)
                .call()
                .content();

        String cleanJson = sanitizeAndRepairJson(rawResponse);

        LlmEvaluationResponse llmResponse;
        try {
            // تبدیل مستقیم JSON با ObjectMapper اصلاح‌شده
            llmResponse = objectMapper.readValue(cleanJson, LlmEvaluationResponse.class);
        } catch (Exception e) {
            System.err.println("Failed to parse JSON response: " + e.getMessage() + " | Raw JSON: " + cleanJson);
            llmResponse = new LlmEvaluationResponse(
                    50.0,
                    "Evaluation completed with partial LLM response.",
                    List.of(),
                    List.of(),
                    List.of("Review complete candidate profile manually.")
            );
        }

        // نرمال‌سازی نمره‌های اعشاری (مثلاً 0.8 به 80.0)
        double score = llmResponse.overallMatchScore();
        if (score > 0.0 && score <= 1.0) {
            score = score * 100.0;
        }

        return new EvaluationResult(
                candidateId,
                jobPosting.id(),
                score,
                llmResponse.summary(),
                llmResponse.matchingSkills() != null ? llmResponse.matchingSkills() : List.of(),
                llmResponse.missingSkills() != null ? llmResponse.missingSkills() : List.of(),
                llmResponse.recommendations() != null ? llmResponse.recommendations() : List.of()
        );
    }

    private String sanitizeAndRepairJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        String trimmed = raw.trim();
        
        // پاک‌سازی تگ‌های مارک‌داون
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        // استخراج محدوده دقیق JSON از اول { تا آخر }
        int firstBrace = trimmed.indexOf("{");
        int lastBrace = trimmed.lastIndexOf("}");

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        if (firstBrace != -1 && (lastBrace == -1 || lastBrace <= firstBrace)) {
            return trimmed.substring(firstBrace) + "}";
        }

        return trimmed;
    }
}