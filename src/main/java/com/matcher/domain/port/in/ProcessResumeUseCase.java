

package com.matcher.domain.port.in;

import java.io.InputStream;
import java.util.UUID;

public interface ProcessResumeUseCase {
    void processAndStoreResume(UUID candidateId, InputStream pdfInputStream, String filename);
}
