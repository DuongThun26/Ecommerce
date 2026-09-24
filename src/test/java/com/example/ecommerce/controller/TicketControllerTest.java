package com.example.ecommerce.controller;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.ticket.controller.TicketController;
import com.example.ecommerce.ticket.dto.request.TicketApproveRequest;
import com.example.ecommerce.ticket.dto.request.TicketRejectRequest;
import com.example.ecommerce.ticket.dto.request.TicketReturnRequest;
import com.example.ecommerce.ticket.dto.response.TicketDetailResponse;
import com.example.ecommerce.ticket.dto.response.TicketResponse;
import com.example.ecommerce.ticket.enums.TicketStatus;
import com.example.ecommerce.ticket.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @Test
    @DisplayName("GET /tickets should return paginated tickets")
    void getTicketsShouldReturnPage() throws Exception {
        PageResponse<TicketResponse> page = PageResponse.<TicketResponse>builder()
                .content(List.of(TicketResponse.builder().id(10L).ticketCode("TK-20260924-00001").status(TicketStatus.PENDING_APPROVAL).build()))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .isLast(true)
                .build();
        when(ticketService.getTickets(anyMap(), anyList())).thenReturn(page);

        mockMvc.perform(get("/tickets")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /tickets/{id} should return ticket detail")
    void getTicketByIdShouldReturnDetail() throws Exception {
        TicketDetailResponse response = TicketDetailResponse.builder()
                .id(10L)
                .ticketCode("TK-20260924-00001")
                .status(TicketStatus.PENDING_APPROVAL)
                .build();
        when(ticketService.getTicketById(10L)).thenReturn(response);

        mockMvc.perform(get("/tickets/10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /tickets/{id}/approve should approve ticket")
    void approveTicketShouldReturnOk() throws Exception {
        TicketApproveRequest request = new TicketApproveRequest("Approved");
        TicketResponse response = TicketResponse.builder().id(10L).status(TicketStatus.APPROVED).build();
        when(ticketService.approveTicket(eq(10L), any(), any(TicketApproveRequest.class))).thenReturn(response);

        mockMvc.perform(post("/tickets/10/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /tickets/{id}/reject should reject ticket")
    void rejectTicketShouldReturnOk() throws Exception {
        TicketRejectRequest request = new TicketRejectRequest("Out of budget");
        TicketResponse response = TicketResponse.builder().id(10L).status(TicketStatus.REJECTED).build();
        when(ticketService.rejectTicket(eq(10L), any(), any(TicketRejectRequest.class))).thenReturn(response);

        mockMvc.perform(post("/tickets/10/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /tickets/{id}/return should return ticket for revision")
    void returnTicketShouldReturnOk() throws Exception {
        TicketReturnRequest request = new TicketReturnRequest("Adjust volume");
        TicketResponse response = TicketResponse.builder().id(10L).status(TicketStatus.RETURNED).build();
        when(ticketService.returnTicket(eq(10L), any(), any(TicketReturnRequest.class))).thenReturn(response);

        mockMvc.perform(post("/tickets/10/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
