

package com.matcher.domain.model;

import java.util.Map;
import java.util.UUID;

public record ResumeChunk(
    UUID id,
    UUID candidateId,
    String content,
    Map<String, Object> metadata
) {}
