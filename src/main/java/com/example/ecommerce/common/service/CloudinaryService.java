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
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_FILES_COUNT = 10; // Tối đa 10 ảnh / lần

    public String upload(MultipartFile file) {
        validateImageFile(file);
        try {
            Map<?, ?> options = ObjectUtils.asMap(
                    "folder", "ecommerce/products",
                    "resource_type", "image"
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

        // Lọc bỏ file null hoặc rỗng nếu có
        List<MultipartFile> validFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            return Collections.emptyList();
        }

        if (validFiles.size() > MAX_FILES_COUNT) {
            throw new BadRequestException("Chỉ được tải lên tối đa " + MAX_FILES_COUNT + " ảnh cùng lúc.");
        }

        // Validate toàn bộ danh sách trước khi upload để tránh upload dở dang lên Cloudinary
        for (MultipartFile file : validFiles) {
            validateImageFile(file);
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : validFiles) {
            urls.add(upload(file));
        }
        return urls;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File ảnh không được để trống.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    String.format("File '%s' vượt quá dung lượng tối đa cho phép (5MB).", file.getOriginalFilename())
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    String.format("File '%s' có định dạng không hợp lệ. Chỉ chấp nhận JPG, PNG, WEBP, GIF.", file.getOriginalFilename())
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new BadRequestException(
                        String.format("Đuôi mở rộng của file '%s' không được hỗ trợ.", originalFilename)
                );
            }
        }
    }

}
