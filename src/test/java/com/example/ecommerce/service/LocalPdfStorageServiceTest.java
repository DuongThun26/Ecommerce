package com.example.ecommerce.service;

import com.example.ecommerce.invoice.service.impl.LocalPdfStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPdfStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalPdfStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalPdfStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("LocalPdfStorageService should store, load, check existence and delete files")
    void testStoreAndLoadLifecycle() {
        String key = "2026/09/INV-20260924-000001.pdf";
        byte[] content = "TEST_PDF_BYTES".getBytes();

        String storedKey = storageService.store(key, content);
        assertThat(storedKey).isEqualTo(key);
        assertThat(storageService.exists(key)).isTrue();

        byte[] loaded = storageService.load(key);
        assertThat(loaded).isEqualTo(content);

        Resource resource = storageService.loadAsResource(key);
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();

        storageService.delete(key);
        assertThat(storageService.exists(key)).isFalse();
    }
}
