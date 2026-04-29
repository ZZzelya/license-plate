import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';
const TOKEN_STORAGE_KEY = 'lp_token';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error),
);

const resolveData = (response) => response.data;

export const extractErrorMessage = (error, fallback = 'Что-то пошло не так') => {
  const responseData = error?.response?.data;

  if (typeof responseData === 'string') {
    return responseData;
  }

  return (
    responseData?.message ||
    responseData?.error ||
    responseData?.details ||
    error?.message ||
    fallback
  );
};

export const authApi = {
  login: (payload) => api.post('/auth/login', payload).then(resolveData),
  register: (payload) => api.post('/auth/register', payload).then(resolveData),
  getMe: () => api.get('/auth/me').then(resolveData),
  updateMe: (payload) => api.put('/auth/me', payload).then(resolveData),
};

export const applicantsApi = {
  getAll: () => api.get('/applicants').then(resolveData),
  getByPassport: (passportNumber) =>
    api.get('/applicants/by-passport', { params: { passportNumber } }).then(resolveData),
  create: (payload) => api.post('/applicants', payload).then(resolveData),
  update: (id, payload) => api.put(`/applicants/${id}`, payload).then(resolveData),
  updateRole: (id, role) => api.patch(`/applicants/${id}/role`, null, { params: { role } }).then(resolveData),
  changePassport: (id, newPassportNumber) =>
    api.patch(`/applicants/${id}/passport`, null, { params: { newPassportNumber } }).then(resolveData),
  remove: (id) => api.delete(`/applicants/${id}`).then(resolveData),
};

export const applicationsApi = {
  getAll: () => api.get('/applications').then(resolveData),
  getWithDetails: (id) => api.get(`/applications/${id}/with-details`).then(resolveData),
  getByPassport: (passportNumber) =>
    api.get('/applications/by-passport', { params: { passportNumber } }).then(resolveData),
  create: (payload) => api.post('/applications', payload).then(resolveData),
  confirm: (id, payload = {}) => api.patch(`/applications/${id}/confirm`, payload).then(resolveData),
  complete: (id, payload = {}) => api.patch(`/applications/${id}/complete`, payload).then(resolveData),
  cancel: (id, payload = {}) => api.patch(`/applications/${id}/cancel`, payload).then(resolveData),
  remove: (id) => api.delete(`/applications/${id}`).then(resolveData),
};

export const departmentsApi = {
  getAll: () => api.get('/departments').then(resolveData),
  getByRegion: (region) =>
    api.get('/departments/by-region', { params: { region } }).then(resolveData),
  create: (payload) => api.post('/departments', payload).then(resolveData),
  update: (id, payload) => api.put(`/departments/${id}`, payload).then(resolveData),
  remove: (id) => api.delete(`/departments/${id}`).then(resolveData),
};

export const licensePlatesApi = {
  getAll: () => api.get('/license-plates').then(resolveData),
  getByNumber: (plateNumber) =>
    api.get('/license-plates/by-number', { params: { plateNumber } }).then(resolveData),
  getAvailableByDepartment: (departmentId) =>
    api.get('/license-plates/available/by-department', { params: { departmentId } }).then(resolveData),
  create: (payload) => api.post('/license-plates', payload).then(resolveData),
  update: (id, payload) => api.put(`/license-plates/${id}`, payload).then(resolveData),
  remove: (id) => api.delete(`/license-plates/${id}`).then(resolveData),
};

export const servicesApi = {
  getAll: () => api.get('/services').then(resolveData),
  getAvailable: () => api.get('/services/available').then(resolveData),
  create: (payload) => api.post('/services', payload).then(resolveData),
  update: (id, payload) => api.put(`/services/${id}`, payload).then(resolveData),
  remove: (id) => api.delete(`/services/${id}`).then(resolveData),
};

export const passportChangeRequestsApi = {
  create: (payload) => api.post('/passport-change-requests', payload).then(resolveData),
  getMine: () => api.get('/passport-change-requests/my').then(resolveData),
  getAll: (status = '') =>
    api.get('/passport-change-requests', { params: status ? { status } : {} }).then(resolveData),
  approve: (id, payload = {}) => api.patch(`/passport-change-requests/${id}/approve`, payload).then(resolveData),
  reject: (id, payload = {}) => api.patch(`/passport-change-requests/${id}/reject`, payload).then(resolveData),
};

export default api;
