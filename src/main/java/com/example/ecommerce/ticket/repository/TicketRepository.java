package com.example.ecommerce.ticket.repository;

import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<PurchaseTicketEntity, Long>, JpaSpecificationExecutor<PurchaseTicketEntity> {

    @EntityGraph(attributePaths = {"order", "subOrder", "customer", "reviewer"})
    java.util.List<PurchaseTicketEntity> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "subOrder", "customer", "reviewer"})
    Optional<PurchaseTicketEntity> findFirstByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "subOrder", "customer", "reviewer"})
    Optional<PurchaseTicketEntity> findBySubOrderId(Long subOrderId);

    @EntityGraph(attributePaths = {"order", "subOrder", "customer", "reviewer"})
    Optional<PurchaseTicketEntity> findByTicketCode(String ticketCode);

    @Override
    @EntityGraph(attributePaths = {"order", "subOrder", "customer", "reviewer"})
    Optional<PurchaseTicketEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"order", "subOrder", "customer", "reviewer"})
    Page<PurchaseTicketEntity> findAll(Specification<PurchaseTicketEntity> spec, Pageable pageable);
}
