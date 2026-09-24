package com.example.ecommerce.invoice.repository;

import com.example.ecommerce.invoice.entity.InvoiceItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItemEntity, Long> {
    List<InvoiceItemEntity> findAllByInvoiceId(Long invoiceId);
}
