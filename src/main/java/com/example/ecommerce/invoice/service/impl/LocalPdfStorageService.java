package com.example.ecommerce.invoice.service.impl;

import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.invoice.service.PdfStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class LocalPdfStorageService implements PdfStorageService {

    private final Path rootLocation;

    public LocalPdfStorageService(@Value("${app.invoice.storage.local-path:./storage/invoices}") String storagePath) {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("Could not initialize invoice storage directory at {}", this.rootLocation, e);
            throw new UncheckedIOException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(String key, byte[] content) {
        try {
            Path targetFile = this.rootLocation.resolve(key).normalize();
            if (!targetFile.startsWith(this.rootLocation)) {
                throw new SecurityException("Cannot store file outside current storage directory.");
            }
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            Files.write(targetFile, content);
            log.info("Stored invoice PDF file at {}", targetFile);
            return key;
        } catch (IOException e) {
            log.error("Failed to store file with key {}", key, e);
            throw new UncheckedIOException("Failed to store PDF invoice", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            Path file = this.rootLocation.resolve(key).normalize();
            if (!file.startsWith(this.rootLocation) || !Files.exists(file)) {
                throw new ResourceNotFoundException("Invoice PDF file not found for key: " + key);
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            log.error("Failed to read file for key {}", key, e);
            throw new UncheckedIOException("Failed to read PDF file", e);
        }
    }

    @Override
    public Resource loadAsResource(String key) {
        Path file = this.rootLocation.resolve(key).normalize();
        if (!file.startsWith(this.rootLocation) || !Files.exists(file)) {
            throw new ResourceNotFoundException("Invoice PDF file not found for key: " + key);
        }
        return new FileSystemResource(file);
    }

    @Override
    public boolean exists(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        Path file = this.rootLocation.resolve(key).normalize();
        return file.startsWith(this.rootLocation) && Files.exists(file);
    }

    @Override
    public void delete(String key) {
        try {
            Path file = this.rootLocation.resolve(key).normalize();
            if (file.startsWith(this.rootLocation)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file for key: {}", key, e);
        }
    }
}
