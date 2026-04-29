import api from './axios';

export const getTodos = (params) => api.get('/api/todos', { params });
export const getTodoStats = () => api.get('/api/todos/stats');
export const getTodo = (id) => api.get(`/api/todos/${id}`);
export const createTodo = (data) => api.post('/api/todos', data);
export const updateTodo = (id, data) => api.put(`/api/todos/${id}`, data);
export const toggleTodo = (id) => api.patch(`/api/todos/${id}/toggle`);
export const deleteTodo = (id) => api.delete(`/api/todos/${id}`);
export const deleteAllTodos = () => api.delete('/api/todos');
