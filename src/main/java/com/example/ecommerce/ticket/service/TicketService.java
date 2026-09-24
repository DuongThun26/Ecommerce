package com.example.ecommerce.ticket.service;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.ticket.dto.request.TicketApproveRequest;
import com.example.ecommerce.ticket.dto.request.TicketRejectRequest;
import com.example.ecommerce.ticket.dto.request.TicketReturnRequest;
import com.example.ecommerce.ticket.dto.response.TicketDetailResponse;
import com.example.ecommerce.ticket.dto.response.TicketResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface TicketService {

    TicketDetailResponse getTicketById(Long id);

    TicketDetailResponse getTicketByOrderId(Long orderId);

    TicketDetailResponse getTicketByTicketCode(String ticketCode);

    PageResponse<TicketResponse> getTickets(Map<String, Object> params, List<String> sortByParams);

    default PageResponse<TicketResponse> getTickets(Map<String, Object> params) {
        return getTickets(params, null);
    }

    TicketResponse approveTicket(Long ticketId, String poUsername, TicketApproveRequest request);

    TicketResponse rejectTicket(Long ticketId, String poUsername, TicketRejectRequest request);

    TicketResponse returnTicket(Long ticketId, String poUsername, TicketReturnRequest request);
}
