import { useState, useEffect } from 'react';
import { getUsers, getUserStats, updateUserRole, deleteUser } from '../api/admin';
import { useAuth } from '../context/AuthContext';

export default function AdminUsers() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchData = async () => {
    setLoading(true);
    setError('');
    try {
      const [usersRes, statsRes] = await Promise.all([getUsers(), getUserStats()]);
      setUsers(usersRes.data);
      setStats(statsRes.data);
    } catch {
      setError('Failed to load users. Make sure you have admin access.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleRoleChange = async (id, role) => {
    try {
      await updateUserRole(id, { role });
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update role');
    }
  };

  const handleDelete = async (id, username) => {
    if (!confirm(`Delete user "${username}"?\n\nThis will also delete all their todos and cannot be undone.`)) return;
    try {
      await deleteUser(id);
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete user');
    }
  };

  // Backend returns: { totalUsers, admins, regularUsers, totalTodos }
  return (
    <div className="page">
      <div className="page-header">
        <div>
          <div className="page-title">User Management</div>
          <div className="page-subtitle">Manage accounts and permissions</div>
        </div>
      </div>

      {/* Stats */}
      {stats && (
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-card-icon total">👥</div>
            <div>
              <div className="stat-card-value">{stats.totalUsers}</div>
              <div className="stat-card-label">Total users</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-card-icon done">🛡</div>
            <div>
              <div className="stat-card-value">{stats.admins}</div>
              <div className="stat-card-label">Admins</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-card-icon pending">👤</div>
            <div>
              <div className="stat-card-value">{stats.regularUsers}</div>
              <div className="stat-card-label">Regular users</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-card-icon high">📋</div>
            <div>
              <div className="stat-card-value">{stats.totalTodos}</div>
              <div className="stat-card-label">Total todos</div>
            </div>
          </div>
        </div>
      )}

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? (
        <div className="spinner-wrap"><div className="spinner" /></div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Email</th>
                <th>Role</th>
                <th style={{ width: 80, textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => {
                const isSelf = u.id === currentUser?.id;
                const initial = (u.username || u.email || '?')[0].toUpperCase();
                return (
                  <tr key={u.id}>
                    <td>
                      <div className="user-cell">
                        <div className="user-cell-avatar">{initial}</div>
                        <div>
                          <div style={{ fontWeight: 600, fontSize: 14 }}>
                            {u.username}
                            {isSelf && (
                              <span style={{ marginLeft: 6, fontSize: 11, color: 'var(--text-muted)', fontWeight: 400 }}>
                                (you)
                              </span>
                            )}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td style={{ color: 'var(--text-muted)' }}>{u.email}</td>
                    <td>
                      {isSelf ? (
                        <span className={`role-pill ${u.role === 'ADMIN' ? 'admin' : 'user'}`}>{u.role}</span>
                      ) : (
                        <select
                          className="role-select"
                          value={u.role}
                          onChange={(e) => handleRoleChange(u.id, e.target.value)}
                        >
                          <option value="USER">USER</option>
                          <option value="ADMIN">ADMIN</option>
                        </select>
                      )}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      {!isSelf && (
                        <button
                          className="icon-btn danger"
                          onClick={() => handleDelete(u.id, u.username)}
                          title="Delete user"
                        >
                          ✕
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
