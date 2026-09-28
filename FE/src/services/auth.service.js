import api from './api';

function decodeJwtPayload(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export const authService = {
  async login(username, password) {
    const data = await api.post('/auth/login', { username, password });
    if (data && data.accessToken) {
      localStorage.setItem('token', data.accessToken);
      const decoded = decodeJwtPayload(data.accessToken);
      const role = (decoded?.roles && decoded.roles[0]) ? decoded.roles[0].replace('ROLE_', '') : 'CUSTOMER';
      const userObj = {
        username: decoded?.sub || username,
        roles: decoded?.roles || [role],
        role: role,
        customerId: data.customerId || decoded?.customerId || 1,
      };
      localStorage.setItem('user', JSON.stringify(userObj));
    }
    return data;
  },

  async register(registerData) {
    return await api.post('/auth/register', registerData);
  },

  async logout() {
    try {
      await api.post('/auth/logout', {});
    } catch {
      // ignore
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  },

  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  },

  getToken() {
    return localStorage.getItem('token');
  },
};

export default authService;
