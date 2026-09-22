package com.example.ecommerce.product.service;

import com.example.ecommerce.product.dto.response.ProductImportResponse;
import com.example.ecommerce.common.exception.ProductImportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportService {

    private final JobLauncher jobLauncher;
    @Qualifier("productImportJob")
    private final Job productImportJob;

    // Import CSV product bằng Spring Batch và trả về thống kê total/success/failed.
    public ProductImportResponse importProducts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProductImportException("File CSV không được để trống.");
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("products-import-", ".csv");
            file.transferTo(tempFile);

            // timestamp giúp mỗi lần import là một JobInstance riêng.
            JobParameters parameters = new JobParametersBuilder()
                    .addString("filePath", tempFile.toAbsolutePath().toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(productImportJob, parameters);
            StepExecution stepExecution = jobExecution.getStepExecutions().stream()
                    .findFirst()
                    .orElseThrow(() -> new ProductImportException("Không thể lấy kết quả import."));

            // total = số dòng đã đọc; success = số bản ghi đã ghi; failed = số dòng bị filter/skip.
            long total = stepExecution.getReadCount();
            long success = stepExecution.getWriteCount();
            long failed = stepExecution.getFilterCount()
                    + stepExecution.getReadSkipCount()
                    + stepExecution.getProcessSkipCount()
                    + stepExecution.getWriteSkipCount();

            return ProductImportResponse.builder()
                    .total(total)
                    .success(success)
                    .failed(failed)
                    .build();
        } catch (IOException ex) {
            throw new ProductImportException("Không thể đọc file CSV.", ex);
        } catch (JobExecutionException ex) {
            throw new ProductImportException("Import CSV thất bại.", ex);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ex) {
                    log.warn("Không thể xóa file tạm import: {}", tempFile, ex);
                }
            }
        }
    }
}
