package com.example.ecommerce.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.ecommerce.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File ảnh tải lên không được để trống.");
        }
        try {
            Map<?, ?> options = ObjectUtils.asMap(
                    "folder", "ecommerce/products",
                    "resource_type", "auto"
            );
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null) {
                throw new RuntimeException("Cloudinary upload did not return secure_url");
            }
            log.info("Uploaded file {} to Cloudinary: {}", file.getOriginalFilename(), secureUrl);
            return secureUrl.toString();
        } catch (IOException ex) {
            log.error("Upload file to Cloudinary failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Tải ảnh lên Cloudinary thất bại: " + ex.getMessage(), ex);
        }
    }

    public List<String> uploadMultiple(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                urls.add(upload(file));
            }
        }
        return urls;
    }
}
