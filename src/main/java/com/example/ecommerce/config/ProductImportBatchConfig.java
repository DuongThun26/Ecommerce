package com.example.ecommerce.config;

import com.example.ecommerce.batch.ProductImportRow;
import com.example.ecommerce.entity.ProductEntity;
import com.example.ecommerce.enums.ProductStatus;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Locale;

@Configuration
public class ProductImportBatchConfig {

    // Job tổng cho import CSV product, mỗi lần chạy sẽ có run id mới.
    @Bean
    public Job productImportJob(JobRepository jobRepository, Step productImportStep) {
        return new JobBuilder("productImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(productImportStep)
                .build();
    }

    // Step xử lý theo chunk để tối ưu memory khi import file lớn.
    @Bean
    public Step productImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<ProductImportRow> productImportReader,
            ItemProcessor<ProductImportRow, ProductEntity> productImportProcessor,
            ItemWriter<ProductEntity> productImportWriter
    ) {
        return new StepBuilder("productImportStep", jobRepository)
                .<ProductImportRow, ProductEntity>chunk(100, transactionManager)
                .reader(productImportReader)
                .processor(productImportProcessor)
                .writer(productImportWriter)
                .build();
    }

    // Reader đọc từng dòng CSV từ filePath truyền vào qua JobParameters.
    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public FlatFileItemReader<ProductImportRow> productImportReader(
            @Value("#{jobParameters['filePath']}") String filePath
    ) {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setNames("name", "price", "stock", "status");
        tokenizer.setStrict(false);

        DefaultLineMapper<ProductImportRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> ProductImportRow.builder()
                .name(fieldSet.readString("name"))
                .price(fieldSet.readString("price"))
                .stock(fieldSet.readString("stock"))
                .status(fieldSet.readString("status"))
                .build());

        return new FlatFileItemReaderBuilder<ProductImportRow>()
                .name("productImportReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .lineMapper(lineMapper)
                .build();
    }

    // Processor validate từng record; record không hợp lệ trả về null để bị filter.
    @Bean
    public ItemProcessor<ProductImportRow, ProductEntity> productImportProcessor() {
        return row -> {
            String name = trimToNull(row.getName());
            if (name == null) {
                return null;
            }

            BigDecimal price = parsePrice(row.getPrice());
            Integer stock = parseStock(row.getStock());
            if (price == null || stock == null) {
                return null;
            }

            ProductStatus status = parseStatus(row.getStatus(), stock);
            if (status == null) {
                return null;
            }

            return ProductEntity.builder()
                    .name(name)
                    .price(price)
                    .stock(stock)
                    .status(status)
                    .deleted(false)
                    .build();
        };
    }

    // Writer chỉ ghi các record đã qua validate thành công.
    @Bean
    public ItemWriter<ProductEntity> productImportWriter(ProductRepository productRepository) {
        return items -> {
            if (!items.isEmpty()) {
                productRepository.saveAll(items);
            }
        };
    }

    // Chuẩn hóa chuỗi: trim và đổi chuỗi rỗng thành null.
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Parse price, chỉ chấp nhận số >= 0.
    private BigDecimal parsePrice(String rawPrice) {
        String value = trimToNull(rawPrice);
        if (value == null) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value);
            return parsed.signum() < 0 ? null : parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // Parse stock, chỉ chấp nhận số nguyên >= 0.
    private Integer parseStock(String rawStock) {
        String value = trimToNull(rawStock);
        if (value == null) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // Parse status; nếu thiếu status thì tự suy ra theo stock.
    private ProductStatus parseStatus(String rawStatus, Integer stock) {
        String value = trimToNull(rawStatus);
        if (value == null) {
            return stock == 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.AVAILABLE;
        }
        try {
            return ProductStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
