

package com.matcher.domain.port.out;

import com.matcher.domain.model.EvaluationResult;
import com.matcher.domain.model.JobPosting;
import com.matcher.domain.model.ResumeChunk;

import java.util.List;

public interface LlmEvaluationPort {
    EvaluationResult evaluateMatch(JobPosting jobPosting, List<ResumeChunk> relevantChunks);
}
