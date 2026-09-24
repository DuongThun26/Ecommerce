package com.example.ecommerce.invoice.repository;

import com.example.ecommerce.invoice.entity.InvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long>, JpaSpecificationExecutor<InvoiceEntity> {

    Optional<InvoiceEntity> findByInvoiceNumber(String invoiceNumber);

    Optional<InvoiceEntity> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    Page<InvoiceEntity> findAllByCustomerId(Long customerId, Pageable pageable);

    Optional<InvoiceEntity> findByIdAndCustomerId(Long id, Long customerId);

    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    Long getNextInvoiceSequenceValue();
}
