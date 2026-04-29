import api from './axios';

export const getProfile = () => api.get('/api/profile');
export const updateProfile = (data) => api.put('/api/profile', data);
export const changePassword = (data) => api.put('/api/profile/change-password', data);
