package com.example.ecommerce.repository;

import com.example.ecommerce.product.entity.ProductEntity;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.testdata.ProductTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("findByIdAndDeletedFalse should return only non-deleted product")
    void findByIdAndDeletedFalseShouldWork() {
        ProductEntity active = productRepository.save(ProductTestDataBuilder.aProduct().withDeleted(false).withName("A").build());
        ProductEntity deleted = productRepository.save(ProductTestDataBuilder.aProduct().withDeleted(true).withName("B").build());

        assertThat(productRepository.findByIdAndDeletedFalse(active.getId())).isPresent();
        assertThat(productRepository.findByIdAndDeletedFalse(deleted.getId())).isEmpty();
    }

    @Test
    @DisplayName("findAllByDeletedFalse should exclude soft deleted products")
    void findAllByDeletedFalseShouldExcludeDeleted() {
        productRepository.save(ProductTestDataBuilder.aProduct().withDeleted(false).withName("A1").build());
        productRepository.save(ProductTestDataBuilder.aProduct().withDeleted(true).withName("D1").build());

        List<ProductEntity> results = productRepository.findAllByDeletedFalse();
        assertThat(results).extracting(ProductEntity::isDeleted).containsOnly(false);
    }

    @Test
    @DisplayName("findByIdInAndDeletedFalse should filter by ids and deleted flag")
    void findByIdInAndDeletedFalseShouldWork() {
        ProductEntity p1 = productRepository.save(ProductTestDataBuilder.aProduct().withDeleted(false).withName("P1").build());
        ProductEntity p2 = productRepository.save(ProductTestDataBuilder.aProduct().withDeleted(true).withName("P2").build());
        ProductEntity p3 = productRepository.save(ProductTestDataBuilder.aProduct().withDeleted(false).withName("P3").build());

        List<ProductEntity> result = productRepository.findByIdInAndDeletedFalse(List.of(p1.getId(), p2.getId(), p3.getId()));

        assertThat(result).extracting(ProductEntity::getId).containsExactlyInAnyOrder(p1.getId(), p3.getId());
    }
}
