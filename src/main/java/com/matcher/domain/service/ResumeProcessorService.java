

package com.matcher.domain.service;

import com.matcher.domain.model.ResumeChunk;
import com.matcher.domain.port.in.ProcessResumeUseCase;
import com.matcher.domain.port.out.VectorStorePort;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResumeProcessorService implements ProcessResumeUseCase {

    private final VectorStorePort vectorStorePort;

    public ResumeProcessorService(VectorStorePort vectorStorePort) {
        this.vectorStorePort = vectorStorePort;
    }

    @Override
    public void processAndStoreResume(UUID candidateId, InputStream pdfInputStream, String filename) {
        // ۱. منطق خواندن متن (توسط Adapter فراخوانی می‌شود)
        // ۲. ساخت متاداده‌های اختصاصی برای فیلتر RAG در آینده
        Map<String, Object> metadata = Map.of(
            "candidateId", candidateId.toString(),
            "filename", filename
        );

        // ادامه پیاده‌سازی ETL در Adapter انجام می‌گیرد
    }
}
