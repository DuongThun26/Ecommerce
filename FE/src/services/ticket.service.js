import api from './api';

export const ticketService = {
  async getTickets(params = {}) {
    return await api.get('/tickets', params);
  },

  async getTicketById(id) {
    return await api.get(`/tickets/${id}`);
  },

  async approveTicket(ticketId, notes = '') {
    return await api.post(`/tickets/${ticketId}/approve`, { notes });
  },

  async rejectTicket(ticketId, reason) {
    return await api.post(`/tickets/${ticketId}/reject`, { reason });
  },

  async returnTicket(ticketId, reason) {
    return await api.post(`/tickets/${ticketId}/return`, { reason });
  },
};

export default ticketService;
