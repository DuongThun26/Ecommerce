package com.example.ecommerce.service;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.customer.entity.CustomerEntity;
import com.example.ecommerce.order.entity.OrderEntity;
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
import com.example.ecommerce.ticket.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private PurchaseTicketEntity sampleTicket;
    private OrderEntity sampleOrder;
    private CustomerEntity sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = CustomerEntity.builder().id(1L).name("Customer A").email("customer@example.com").build();
        sampleOrder = OrderEntity.builder().id(100L).customer(sampleCustomer).totalAmount(BigDecimal.valueOf(500000)).build();
        sampleTicket = PurchaseTicketEntity.builder()
                .id(10L)
                .ticketCode("TK-20260924-00100")
                .order(sampleOrder)
                .customer(sampleCustomer)
                .status(TicketStatus.PENDING_APPROVAL)
                .title("Phiếu mua hàng cho đơn hàng #100")
                .build();
    }

    @Test
    @DisplayName("getTicketById should return ticket detail when found")
    void getTicketByIdShouldReturnDetail() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
        TicketDetailResponse expectedResponse = TicketDetailResponse.builder().id(10L).ticketCode("TK-20260924-00100").build();
        when(ticketMapper.toDetailResponse(sampleTicket)).thenReturn(expectedResponse);

        TicketDetailResponse result = ticketService.getTicketById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getTicketById should throw exception when not found")
    void getTicketByIdShouldThrowWhenNotFound() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("approveTicket should delegate to orderService and return updated response")
    void approveTicketShouldDelegateToOrderService() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
        TicketApproveRequest request = new TicketApproveRequest("Approved for procurement");
        TicketResponse expectedResponse = TicketResponse.builder().id(10L).status(TicketStatus.APPROVED).build();
        when(ticketMapper.toResponse(sampleTicket)).thenReturn(expectedResponse);

        TicketResponse result = ticketService.approveTicket(10L, "po_manager", request);

        verify(orderService).approveOrder(100L, "po_manager", "Approved for procurement");
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TicketStatus.APPROVED);
    }

    @Test
    @DisplayName("rejectTicket should validate reason and delegate to orderService")
    void rejectTicketShouldDelegateToOrderService() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
        TicketRejectRequest request = new TicketRejectRequest("Out of budget");
        TicketResponse expectedResponse = TicketResponse.builder().id(10L).status(TicketStatus.REJECTED).build();
        when(ticketMapper.toResponse(sampleTicket)).thenReturn(expectedResponse);

        TicketResponse result = ticketService.rejectTicket(10L, "po_manager", request);

        verify(orderService).rejectOrder(100L, "po_manager", "Out of budget");
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TicketStatus.REJECTED);
    }

    @Test
    @DisplayName("rejectTicket should throw BadRequestException when reason is blank")
    void rejectTicketShouldThrowWhenReasonBlank() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
        TicketRejectRequest request = new TicketRejectRequest("   ");

        assertThatThrownBy(() -> ticketService.rejectTicket(10L, "po_manager", request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("returnTicket should validate reason and delegate to orderService")
    void returnTicketShouldDelegateToOrderService() {
        when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
        TicketReturnRequest request = new TicketReturnRequest("Please reduce quantity");
        TicketResponse expectedResponse = TicketResponse.builder().id(10L).status(TicketStatus.RETURNED).build();
        when(ticketMapper.toResponse(sampleTicket)).thenReturn(expectedResponse);

        TicketResponse result = ticketService.returnTicket(10L, "po_manager", request);

        verify(orderService).returnOrder(100L, "po_manager", "Please reduce quantity");
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TicketStatus.RETURNED);
    }

    @Test
    @DisplayName("getTickets should return paginated response")
    void getTicketsShouldReturnPaginatedList() {
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleTicket)));
        when(ticketMapper.toResponse(sampleTicket))
                .thenReturn(TicketResponse.builder().id(10L).ticketCode("TK-20260924-00100").build());

        PageResponse<TicketResponse> result = ticketService.getTickets(Map.of("page", "0", "size", "10"));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTicketCode()).isEqualTo("TK-20260924-00100");
    }
}
