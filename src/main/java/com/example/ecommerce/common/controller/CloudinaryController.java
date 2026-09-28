package com.example.ecommerce.common.controller;

import com.example.ecommerce.common.response.ApiResponse;
import com.example.ecommerce.common.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cloudinary")
@RequiredArgsConstructor
@Tag(
        name = "Cloudinary",
        description = "APIs cho việc tải ảnh lên Cloudinary."
)
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    @Operation(
            summary = "Tải 1 ảnh lên Cloudinary",
            description = "API nhận vào 1 file ảnh và trả về đường dẫn URL trên Cloudinary."
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(@RequestParam("file") MultipartFile file) {
        String url = cloudinaryService.upload(file);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Tải ảnh lên Cloudinary thành công", Map.of("url", url)));
    }

    @Operation(
            summary = "Tải nhiều ảnh lên Cloudinary",
            description = "API nhận vào danh sách file ảnh và trả về danh sách URL trên Cloudinary."
    )
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMultiple(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = cloudinaryService.uploadMultiple(files);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Tải danh sách ảnh lên Cloudinary thành công", Map.of(
                        "urls", urls,
                        "total", urls.size()
                )));
    }
}