package com.example.ecommerce.category.repository;

import com.example.ecommerce.category.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long>, JpaSpecificationExecutor<CategoryEntity> {

    Optional<CategoryEntity> findByIdAndDeletedFalse(Long id);

    Optional<CategoryEntity> findByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

    boolean existsByCodeIgnoreCaseAndIdNotAndDeletedFalse(String code, Long id);

    List<CategoryEntity> findByManagedByIdAndDeletedFalse(Long userId);

    List<CategoryEntity> findByManagedByUsernameAndDeletedFalse(String username);

    List<CategoryEntity> findAllByDeletedFalse();

    Page<CategoryEntity> findAll(Specification<CategoryEntity> spec, Pageable pageable);
}
