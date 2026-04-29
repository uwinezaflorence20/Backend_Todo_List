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
    try {
      const [usersRes, statsRes] = await Promise.all([getUsers(), getUserStats()]);
      setUsers(usersRes.data);
      setStats(statsRes.data);
    } catch {
      setError('Failed to load users');
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
    if (!confirm(`Delete user "${username}"? This also deletes all their todos.`)) return;
    try {
      await deleteUser(id);
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete user');
    }
  };

  return (
    <div className="page">
      <h1>User Management</h1>

      {stats && (
        <div className="stats-row mb-4">
          <span className="stat">{stats.totalUsers} users</span>
          <span className="stat stat-success">{stats.adminCount} admins</span>
          <span className="stat">{stats.regularUserCount} regular</span>
        </div>
      )}

      {error && <div className="alert alert-error mb-4">{error}</div>}

      {loading ? (
        <div className="loading">Loading...</div>
      ) : (
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Email</th>
                <th>Role</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.username}{u.id === currentUser?.id ? ' (you)' : ''}</td>
                  <td>{u.email}</td>
                  <td>
                    <select
                      value={u.role}
                      onChange={(e) => handleRoleChange(u.id, e.target.value)}
                      disabled={u.id === currentUser?.id}
                      className="role-select"
                    >
                      <option value="USER">USER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </td>
                  <td>
                    <button
                      className="btn-icon btn-icon-danger"
                      onClick={() => handleDelete(u.id, u.username)}
                      title="Delete user"
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
