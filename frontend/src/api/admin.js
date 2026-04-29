import api from './axios';

export const getUsers = () => api.get('/api/users');
export const getUserStats = () => api.get('/api/users/stats');
export const getUser = (id) => api.get(`/api/users/${id}`);
export const updateUser = (id, data) => api.put(`/api/users/${id}`, data);
export const updateUserRole = (id, data) => api.patch(`/api/users/${id}/role`, data);
export const deleteUser = (id) => api.delete(`/api/users/${id}`);
