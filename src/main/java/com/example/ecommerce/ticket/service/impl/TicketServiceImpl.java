package com.example.ecommerce.ticket.service.impl;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.ticket.dto.request.TicketApproveRequest;
import com.example.ecommerce.ticket.dto.request.TicketRejectRequest;
import com.example.ecommerce.ticket.dto.request.TicketReturnRequest;
import com.example.ecommerce.ticket.dto.response.TicketDetailResponse;
import com.example.ecommerce.ticket.dto.response.TicketResponse;
import com.example.ecommerce.ticket.entity.PurchaseTicketEntity;
import com.example.ecommerce.ticket.enums.TicketStatus;
import com.example.ecommerce.ticket.mapper.TicketMapper;
import com.example.ecommerce.ticket.repository.TicketRepository;
import com.example.ecommerce.ticket.service.TicketService;
import com.example.ecommerce.ticket.specification.TicketSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private final TicketRepository ticketRepository;
    private final OrderService orderService;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketById(Long id) {
        PurchaseTicketEntity ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase ticket with id " + id + " not found."));
        return ticketMapper.toDetailResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketByOrderId(Long orderId) {
        PurchaseTicketEntity ticket = ticketRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase ticket for order id " + orderId + " not found."));
        return ticketMapper.toDetailResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketByTicketCode(String ticketCode) {
        PurchaseTicketEntity ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase ticket with code " + ticketCode + " not found."));
        return ticketMapper.toDetailResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> getTickets(Map<String, Object> params, List<String> sortByParams) {
        TicketFilterCriteria criteria = resolveFilterCriteria(params);
        Pageable pageable = createPageable(params, sortByParams);
        Specification<PurchaseTicketEntity> spec = TicketSpecification.withFilters(
                criteria.customerId(),
                criteria.status(),
                toStartOfDay(criteria.createdFrom()),
                toEndOfDay(criteria.createdTo()),
                criteria.keyword()
        );
        Page<PurchaseTicketEntity> page = ticketRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(ticketMapper::toResponse));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketResponse approveTicket(Long ticketId, String poUsername, TicketApproveRequest request) {
        PurchaseTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase ticket with id " + ticketId + " not found."));

        String notes = request == null ? null : request.getNotes();
        // Delegate order transition
        orderService.approveOrder(ticket.getOrder().getId(), poUsername, notes);

        PurchaseTicketEntity updatedTicket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase ticket with id " + ticketId + " not found."));
        return ticketMapper.toResponse(updatedTicket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketResponse rejectTicket(Long ticketId, String poUsername, TicketRejectRequest request) {
        PurchaseTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase ticket with id " + ticketId + " not found."));

        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("Lý do từ chối không được để trống.");
        }

        // Delegate order transition
        orderService.rejectOrder(ticket.getOrder().getId(), poUsername, request.getReason());

        PurchaseTicketEntity updatedTicket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase ticket with id " + ticketId + " not found."));
        return ticketMapper.toResponse(updatedTicket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketResponse returnTicket(Long ticketId, String poUsername, TicketReturnRequest request) {
        PurchaseTicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase ticket with id " + ticketId + " not found."));

        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("Lý do trả lại không được để trống.");
        }

        // Delegate order transition
        orderService.returnOrder(ticket.getOrder().getId(), poUsername, request.getReason());

        PurchaseTicketEntity updatedTicket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase ticket with id " + ticketId + " not found."));
        return ticketMapper.toResponse(updatedTicket);
    }

    private Pageable createPageable(Map<String, Object> params, List<String> sortByParams) {
        int page = DEFAULT_PAGE;
        int size = DEFAULT_SIZE;

        if (params != null) {
            if (params.containsKey("page") && params.get("page") != null) {
                try {
                    int parsedPage = Integer.parseInt(params.get("page").toString().trim());
                    if (parsedPage < 0) {
                        throw new BadRequestException("Số trang phải lớn hơn hoặc bằng 0.");
                    }
                    page = parsedPage;
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Tham số page phải là số nguyên hợp lệ.");
                }
            }
            if (params.containsKey("size") && params.get("size") != null) {
                try {
                    int parsedSize = Integer.parseInt(params.get("size").toString().trim());
                    if (parsedSize <= 0) {
                        throw new BadRequestException("Số lượng bản ghi phải lớn hơn 0.");
                    }
                    size = parsedSize;
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Tham số size phải là số nguyên hợp lệ.");
                }
            }
        }

        Sort.Direction defaultDirection = resolveDirection(params);
        List<Sort.Order> orders = buildSortOrders(params, sortByParams, defaultDirection);
        if (orders.isEmpty()) {
            orders.add(new Sort.Order(defaultDirection, "id"));
        }
        return PageRequest.of(page, size, Sort.by(orders));
    }

    private List<Sort.Order> buildSortOrders(Map<String, Object> params, List<String> sortByParams, Sort.Direction defaultDirection) {
        List<Sort.Order> orders = new ArrayList<>();
        List<String> rawTokens = new ArrayList<>();

        if (sortByParams != null && !sortByParams.isEmpty()) {
            for (String sortBy : sortByParams) {
                if (sortBy != null && !sortBy.isBlank()) {
                    rawTokens.add(sortBy.trim());
                }
            }
        }

        if (rawTokens.isEmpty() && params != null) {
            Object sortField = params.get("sortBy");
            if (sortField != null && !sortField.toString().isBlank()) {
                rawTokens.add(sortField.toString().trim());
            }
        }

        int i = 0;
        while (i < rawTokens.size()) {
            String token = rawTokens.get(i);
            if (token.isBlank()) {
                i++;
                continue;
            }

            if (token.contains(",") || token.contains(":")) {
                String delimiter = token.contains(":") ? ":" : ",";
                String[] parts = token.split(delimiter, 2);
                String field = parts[0].trim();
                if (!field.isBlank()) {
                    String mappedField = resolveSortBy(field);
                    Sort.Direction direction = defaultDirection;
                    if (parts.length == 2 && !parts[1].trim().isBlank()) {
                        direction = parseDirection(parts[1].trim(), "sortBy");
                    }
                    orders.add(new Sort.Order(direction, mappedField));
                }
                i++;
            } else {
                String mappedField = resolveSortBy(token);
                Sort.Direction direction = defaultDirection;
                if (i + 1 < rawTokens.size()) {
                    String nextToken = rawTokens.get(i + 1).trim();
                    if ("asc".equalsIgnoreCase(nextToken) || "desc".equalsIgnoreCase(nextToken)) {
                        direction = parseDirection(nextToken, "sortBy");
                        i++;
                    }
                }
                orders.add(new Sort.Order(direction, mappedField));
                i++;
            }
        }
        return orders;
    }

    private Sort.Direction resolveDirection(Map<String, Object> params) {
        Sort.Direction direction = Sort.Direction.DESC;
        if (params != null) {
            String dirStr = null;
            if (params.containsKey("direction") && params.get("direction") != null) {
                dirStr = params.get("direction").toString().trim();
            }
            if (dirStr != null && !dirStr.isBlank()) {
                direction = parseDirection(dirStr, "direction");
            }
        }
        return direction;
    }

    private Sort.Direction parseDirection(String direction, String fieldName) {
        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(direction)) {
            return Sort.Direction.DESC;
        }
        if ("sortBy".equals(fieldName)) {
            throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'status' hoặc 'ticketCode'.");
        }
        throw new BadRequestException("Direction phải là 'asc' hoặc 'desc'.");
    }

    private String resolveSortBy(String sortBy) {
        if (sortBy == null) {
            throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'status' hoặc 'ticketCode'.");
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "id" -> "id";
            case "createdat", "created_at" -> "createdAt";
            case "status" -> "status";
            case "ticketcode", "ticket_code" -> "ticketCode";
            default -> throw new BadRequestException("sortBy phải là 'id', 'createdAt', 'status' hoặc 'ticketCode'.");
        };
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59, 999_999_999);
    }

    private TicketFilterCriteria resolveFilterCriteria(Map<String, Object> params) {
        Long customerId = parseLong(params, "customerId");
        TicketStatus status = parseTicketStatus(params, "status");
        LocalDate createdFrom = parseLocalDate(params, "createdFrom");
        LocalDate createdTo = parseLocalDate(params, "createdTo");
        String keyword = trimToNull(readParam(params, "keyword"));
        validateFilterRange(createdFrom, createdTo);
        return new TicketFilterCriteria(customerId, status, createdFrom, createdTo, keyword);
    }

    private void validateFilterRange(LocalDate createdFrom, LocalDate createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new BadRequestException("createdFrom phải nhỏ hơn hoặc bằng createdTo.");
        }
    }

    private TicketStatus parseTicketStatus(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }
        try {
            return TicketStatus.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("status không hợp lệ.");
        }
    }

    private Long parseLong(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BadRequestException(key + " phải là số nguyên hợp lệ.");
        }
    }

    private LocalDate parseLocalDate(Map<String, Object> params, String key) {
        String value = trimToNull(readParam(params, key));
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new BadRequestException(key + " phải theo định dạng yyyy-MM-dd.");
        }
    }

    private String readParam(Map<String, Object> params, String key) {
        if (params == null) {
            return null;
        }
        Object value = params.get(key);
        return value == null ? null : value.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record TicketFilterCriteria(
            Long customerId,
            TicketStatus status,
            LocalDate createdFrom,
            LocalDate createdTo,
            String keyword
    ) {
    }
}
