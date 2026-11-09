package com.matcher.infrastructure.adapter.parser;

import com.matcher.domain.model.ResumeChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PdfDocumentParser {

    public List<ResumeChunk> parseAndChunk(UUID candidateId, InputStream pdfInputStream, String filename) {
        
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageTopMargin(0)
                .build();

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                new InputStreamResource(pdfInputStream), 
                config
        );

        List<Document> rawDocuments = pdfReader.get();


        List<Document> safeDocuments = rawDocuments.stream()
                .map(doc -> {
                    Map<String, Object> cleanMetadata = new HashMap<>();
                    if (doc.getMetadata() != null) {
                        doc.getMetadata().forEach((key, value) -> {
                            if (key != null && value != null) {
                                cleanMetadata.put(key, value);
                            }
                        });
                    }
                    return new Document(
                            doc.getId() != null ? doc.getId() : UUID.randomUUID().toString(),
                            doc.getContent() != null ? doc.getContent() : "",
                            cleanMetadata
                    );
                })
                .toList();

      
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        List<Document> chunkedDocuments = textSplitter.apply(safeDocuments);

        
        return chunkedDocuments.stream()
                .map(doc -> {
                    Map<String, Object> metadata = new HashMap<>();
                    if (doc.getMetadata() != null) {
                        doc.getMetadata().forEach((k, v) -> {
                            if (k != null && v != null) {
                                metadata.put(k, v);
                            }
                        });
                    }
                    metadata.put("candidateId", candidateId.toString());
                    metadata.put("filename", filename != null ? filename : "unknown.pdf");

                    return new ResumeChunk(
                            UUID.randomUUID(),
                            candidateId,
                            doc.getContent(),
                            metadata
                    );
                })
                .toList();
    }
}