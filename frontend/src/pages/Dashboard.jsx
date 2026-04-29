import { useState, useEffect, useCallback } from 'react';
import { getTodos, getTodoStats, createTodo, updateTodo, toggleTodo, deleteTodo, deleteAllTodos } from '../api/todos';
import TodoItem from '../components/TodoItem';
import TodoForm from '../components/TodoForm';

export default function Dashboard() {
  const [todos, setTodos] = useState([]);
  const [stats, setStats] = useState(null);
  const [page, setPage] = useState({ number: 0, totalPages: 0, totalElements: 0 });
  const [filters, setFilters] = useState({ search: '', completed: '', priority: '', page: 0, size: 10, sort: 'createdAt,desc' });
  const [showForm, setShowForm] = useState(false);
  const [editingTodo, setEditingTodo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchTodos = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params = { ...filters };
      if (params.completed === '') delete params.completed;
      if (params.priority === '') delete params.priority;
      if (params.search === '') delete params.search;
      const { data } = await getTodos(params);
      setTodos(data.content);
      setPage({ number: data.number, totalPages: data.totalPages, totalElements: data.totalElements });
    } catch {
      setError('Failed to load todos');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  const fetchStats = useCallback(async () => {
    try {
      const { data } = await getTodoStats();
      setStats(data);
    } catch { /* silent */ }
  }, []);

  useEffect(() => { fetchTodos(); fetchStats(); }, [fetchTodos, fetchStats]);

  const handleSave = async (formData) => {
    try {
      if (editingTodo) {
        await updateTodo(editingTodo.id, formData);
      } else {
        await createTodo(formData);
      }
      setShowForm(false);
      setEditingTodo(null);
      fetchTodos();
      fetchStats();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save todo');
    }
  };

  const handleToggle = async (id) => {
    try {
      await toggleTodo(id);
      fetchTodos();
      fetchStats();
    } catch { setError('Failed to update todo'); }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this todo?')) return;
    try {
      await deleteTodo(id);
      fetchTodos();
      fetchStats();
    } catch { setError('Failed to delete todo'); }
  };

  const handleDeleteAll = async () => {
    if (!confirm('Delete ALL todos? This cannot be undone.')) return;
    try {
      await deleteAllTodos();
      fetchTodos();
      fetchStats();
    } catch { setError('Failed to delete todos'); }
  };

  const handleEdit = (todo) => {
    setEditingTodo(todo);
    setShowForm(true);
  };

  const handleFilterChange = (key, val) => {
    setFilters((f) => ({ ...f, [key]: val, page: 0 }));
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>My Todos</h1>
          {stats && (
            <div className="stats-row">
              <span className="stat">{stats.total ?? page.totalElements} total</span>
              <span className="stat stat-success">{stats.completed} done</span>
              <span className="stat stat-warn">{stats.pending} pending</span>
              {stats.highPriority > 0 && <span className="stat stat-danger">{stats.highPriority} high priority</span>}
            </div>
          )}
        </div>
        <div className="header-actions">
          <button className="btn btn-primary" onClick={() => { setEditingTodo(null); setShowForm(true); }}>
            + Add Todo
          </button>
          {todos.length > 0 && (
            <button className="btn btn-danger-outline" onClick={handleDeleteAll}>Delete All</button>
          )}
        </div>
      </div>

      {(showForm || editingTodo) && (
        <div className="card mb-4">
          <h3>{editingTodo ? 'Edit Todo' : 'New Todo'}</h3>
          <TodoForm
            initial={editingTodo}
            onSave={handleSave}
            onCancel={() => { setShowForm(false); setEditingTodo(null); }}
          />
        </div>
      )}

      <div className="filters card mb-4">
        <input
          type="text"
          placeholder="Search..."
          value={filters.search}
          onChange={(e) => handleFilterChange('search', e.target.value)}
          className="filter-input"
        />
        <select value={filters.completed} onChange={(e) => handleFilterChange('completed', e.target.value)}>
          <option value="">All status</option>
          <option value="false">Pending</option>
          <option value="true">Completed</option>
        </select>
        <select value={filters.priority} onChange={(e) => handleFilterChange('priority', e.target.value)}>
          <option value="">All priorities</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
        <select value={filters.sort} onChange={(e) => handleFilterChange('sort', e.target.value)}>
          <option value="createdAt,desc">Newest first</option>
          <option value="createdAt,asc">Oldest first</option>
          <option value="dueDate,asc">Due date</option>
          <option value="priority,desc">Priority</option>
        </select>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : todos.length === 0 ? (
        <div className="empty-state">
          <p>No todos found.</p>
          <button className="btn btn-primary" onClick={() => { setEditingTodo(null); setShowForm(true); }}>
            Add your first todo
          </button>
        </div>
      ) : (
        <div className="todo-list">
          {todos.map((todo) => (
            <TodoItem
              key={todo.id}
              todo={todo}
              onToggle={handleToggle}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}

      {page.totalPages > 1 && (
        <div className="pagination">
          <button
            className="btn btn-ghost"
            disabled={page.number === 0}
            onClick={() => handleFilterChange('page', page.number - 1)}
          >
            Previous
          </button>
          <span>Page {page.number + 1} of {page.totalPages}</span>
          <button
            className="btn btn-ghost"
            disabled={page.number + 1 >= page.totalPages}
            onClick={() => handleFilterChange('page', page.number + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
