import { API_BASE_URL } from '../config/api.config';

class ApiService {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
  }

  getHeaders(customHeaders = {}) {
    const headers = {
      'Content-Type': 'application/json',
      ...customHeaders,
    };

    const token = localStorage.getItem('token');
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    return headers;
  }

  async handleResponse(response) {
    const contentType = response.headers.get('content-type');
    
    // Handle binary / PDF stream downloads
    if (contentType && (contentType.includes('application/pdf') || contentType.includes('application/octet-stream'))) {
      if (!response.ok) {
        throw new Error(`Download failed with status ${response.status}`);
      }
      return await response.blob();
    }

    let json;
    try {
      json = await response.json();
    } catch {
      json = null;
    }

    if (!response.ok) {
      const errorMessage = json?.error?.message || json?.message || `Request failed with status ${response.status}`;
      const error = new Error(errorMessage);
      error.status = response.status;
      error.code = json?.error?.code || json?.code;
      error.data = json?.data;
      throw error;
    }

    // Unwrap BaseResponse or ApiResponse
    if (json && typeof json === 'object') {
      if ('data' in json && 'success' in json) {
        // Check if double wrapped
        if (json.data && typeof json.data === 'object' && 'data' in json.data && 'success' in json.data) {
          return json.data.data;
        }
        return json.data;
      }
    }

    return json;
  }

  async get(endpoint, params = {}) {
    const url = new URL(`${this.baseUrl}${endpoint}`, window.location.origin);
    Object.keys(params).forEach((key) => {
      if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
        url.searchParams.append(key, params[key]);
      }
    });

    const response = await fetch(url.toString(), {
      method: 'GET',
      headers: this.getHeaders(),
    });

    return this.handleResponse(response);
  }

  async post(endpoint, body = {}, customHeaders = {}) {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'POST',
      headers: this.getHeaders(customHeaders),
      body: JSON.stringify(body),
    });

    return this.handleResponse(response);
  }

  async put(endpoint, body = {}, customHeaders = {}) {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'PUT',
      headers: this.getHeaders(customHeaders),
      body: JSON.stringify(body),
    });

    return this.handleResponse(response);
  }

  async patch(endpoint, body = {}, customHeaders = {}) {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'PATCH',
      headers: this.getHeaders(customHeaders),
      body: JSON.stringify(body),
    });

    return this.handleResponse(response);
  }

  async delete(endpoint) {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'DELETE',
      headers: this.getHeaders(),
    });

    return this.handleResponse(response);
  }

  getFormDataHeaders(customHeaders = {}) {
    const headers = { ...customHeaders };
    const token = localStorage.getItem('token');
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  }

  async upload(endpoint, formData, method = 'POST') {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method,
      headers: this.getFormDataHeaders(),
      body: formData,
    });

    return this.handleResponse(response);
  }

  async download(endpoint, filename = 'download.pdf') {
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method: 'GET',
      headers: this.getHeaders({
        Accept: 'application/pdf',
      }),
    });

    const blob = await this.handleResponse(response);
    const downloadUrl = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(downloadUrl);
    document.body.removeChild(a);
  }
}

export const api = new ApiService(API_BASE_URL);
export default api;
