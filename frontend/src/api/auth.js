import api from './axios';

export const login = (data) => api.post('/api/auth/signin', data);
export const signup = (data) => api.post('/api/auth/signup', data);
export const logout = () => api.post('/api/auth/logout');
export const forgotPassword = (data) => api.post('/api/auth/forgot-password', data);
export const resetPassword = (data) => api.post('/api/auth/reset-password', data);
