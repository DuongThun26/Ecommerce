package com.example.ecommerce.invoice.service;

import org.springframework.core.io.Resource;

public interface PdfStorageService {

    String store(String key, byte[] content);

    byte[] load(String key);

    Resource loadAsResource(String key);

    boolean exists(String key);

    void delete(String key);
}
