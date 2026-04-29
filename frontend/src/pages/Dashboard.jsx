import { useState, useEffect, useCallback } from 'react';
import { getTodos, getTodoStats, createTodo, updateTodo, toggleTodo, deleteTodo, deleteAllTodos } from '../api/todos';
import TodoItem from '../components/TodoItem';
import TodoForm from '../components/TodoForm';
import { useAuth } from '../context/AuthContext';

const DEFAULT_FILTERS = { search: '', completed: '', priority: '', page: 0, size: 10, sort: 'createdAt,desc' };

export default function Dashboard() {
  const { user } = useAuth();
  const [todos, setTodos] = useState([]);
  const [stats, setStats] = useState(null);
  const [pagination, setPagination] = useState({ number: 0, totalPages: 0, totalElements: 0 });
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [showForm, setShowForm] = useState(false);
  const [editingTodo, setEditingTodo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saveError, setSaveError] = useState('');

  const fetchTodos = useCallback(async () => {
    setLoading(true);
    try {
      const params = { ...filters };
      if (!params.completed) delete params.completed;
      if (!params.priority) delete params.priority;
      if (!params.search) delete params.search;
      const { data } = await getTodos(params);
      setTodos(data.content);
      setPagination({ number: data.number, totalPages: data.totalPages, totalElements: data.totalElements });
    } catch { /* error handled by interceptor */ }
    finally { setLoading(false); }
  }, [filters]);

  const fetchStats = useCallback(async () => {
    try {
      const { data } = await getTodoStats();
      setStats(data);
    } catch { /* silent */ }
  }, []);

  useEffect(() => { fetchTodos(); }, [fetchTodos]);
  useEffect(() => { fetchStats(); }, [fetchStats]);

  const refresh = () => { fetchTodos(); fetchStats(); };

  const handleSave = async (formData) => {
    setSaveError('');
    try {
      if (editingTodo) {
        await updateTodo(editingTodo.id, formData);
      } else {
        await createTodo(formData);
      }
      setShowForm(false);
      setEditingTodo(null);
      refresh();
    } catch (err) {
      setSaveError(err.response?.data?.message || 'Failed to save todo');
    }
  };

  const handleToggle = async (id) => {
    try { await toggleTodo(id); refresh(); } catch { /* ignore */ }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this todo?')) return;
    try { await deleteTodo(id); refresh(); } catch { /* ignore */ }
  };

  const handleDeleteAll = async () => {
    if (!confirm('Delete ALL your todos? This cannot be undone.')) return;
    try { await deleteAllTodos(); refresh(); } catch { /* ignore */ }
  };

  const handleEdit = (todo) => {
    setEditingTodo(todo);
    setShowForm(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const setFilter = (key, val) =>
    setFilters((f) => ({ ...f, [key]: val, page: 0 }));

  const openNewForm = () => { setEditingTodo(null); setShowForm(true); };
  const closeForm = () => { setShowForm(false); setEditingTodo(null); setSaveError(''); };

  const total = stats?.total ?? pagination.totalElements;

  return (
    <div className="page">
      {/* Header */}
      <div className="page-header">
        <div>
          <div className="page-title">My Todos</div>
          <div className="page-subtitle">Welcome back, {user?.username || user?.email}</div>
        </div>
        <div className="header-actions">
          <button className="btn btn-primary" onClick={openNewForm}>+ New todo</button>
          {todos.length > 0 && (
            <button className="btn btn-danger" onClick={handleDeleteAll}>Delete all</button>
          )}
        </div>
      </div>

      {/* Stats */}
      {stats !== null && (
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-card-icon total">📋</div>
            <div>
              <div className="stat-card-value">{total}</div>
              <div className="stat-card-label">Total</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-card-icon done">✅</div>
            <div>
              <div className="stat-card-value">{stats.completed}</div>
              <div className="stat-card-label">Completed</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-card-icon pending">⏳</div>
            <div>
              <div className="stat-card-value">{stats.pending}</div>
              <div className="stat-card-label">Pending</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-card-icon high">🔴</div>
            <div>
              <div className="stat-card-value">{stats.highPriority}</div>
              <div className="stat-card-label">High priority</div>
            </div>
          </div>
        </div>
      )}

      {/* Inline form */}
      {(showForm || editingTodo) && (
        <>
          {saveError && <div className="alert alert-error">{saveError}</div>}
          <TodoForm initial={editingTodo} onSave={handleSave} onCancel={closeForm} />
        </>
      )}

      {/* Filters */}
      <div className="filters-bar">
        <input
          className="filter-search"
          type="text"
          placeholder="🔍  Search todos…"
          value={filters.search}
          onChange={(e) => setFilter('search', e.target.value)}
        />
        <select className="filter-select" value={filters.completed} onChange={(e) => setFilter('completed', e.target.value)}>
          <option value="">All status</option>
          <option value="false">Pending</option>
          <option value="true">Completed</option>
        </select>
        <select className="filter-select" value={filters.priority} onChange={(e) => setFilter('priority', e.target.value)}>
          <option value="">All priorities</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
        <select className="filter-select" value={filters.sort} onChange={(e) => setFilter('sort', e.target.value)}>
          <option value="createdAt,desc">Newest first</option>
          <option value="createdAt,asc">Oldest first</option>
          <option value="dueDate,asc">Due date</option>
          <option value="priority,desc">Priority</option>
        </select>
      </div>

      {/* List */}
      {loading ? (
        <div className="spinner-wrap"><div className="spinner" /></div>
      ) : todos.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">📝</div>
          <h3>{filters.search || filters.completed || filters.priority ? 'No matching todos' : 'No todos yet'}</h3>
          <p>
            {filters.search || filters.completed || filters.priority
              ? 'Try adjusting your filters.'
              : 'Create your first todo to get started.'}
          </p>
          {!filters.search && !filters.completed && !filters.priority && (
            <button className="btn btn-primary" onClick={openNewForm}>+ New todo</button>
          )}
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

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="pagination">
          <button
            className="btn btn-ghost btn-sm"
            disabled={pagination.number === 0}
            onClick={() => setFilter('page', pagination.number - 1)}
          >
            ← Previous
          </button>
          <span className="pagination-info">
            Page {pagination.number + 1} of {pagination.totalPages}
          </span>
          <button
            className="btn btn-ghost btn-sm"
            disabled={pagination.number + 1 >= pagination.totalPages}
            onClick={() => setFilter('page', pagination.number + 1)}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
