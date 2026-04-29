import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, signOut, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    await signOut();
    navigate('/login');
  };

  const active = (path) =>
    location.pathname === path ? 'nav-link active' : 'nav-link';

  if (!user) return null;

  const initial = (user.username || user.email || '?')[0].toUpperCase();

  return (
    <nav className="navbar">
      <Link to="/dashboard" className="nav-logo">
        <div className="nav-logo-icon">✓</div>
        <span className="nav-logo-text">TodoApp</span>
      </Link>

      <div className="nav-links">
        <Link to="/dashboard" className={active('/dashboard')}>My Todos</Link>
        <Link to="/profile" className={active('/profile')}>Profile</Link>
        {isAdmin && (
          <Link to="/admin/users" className={active('/admin/users')}>
            Admin
          </Link>
        )}
      </div>

      <div className="nav-right">
        <span className="nav-username">{user.username || user.email}</span>
        {isAdmin && <span className="badge badge-admin" style={{ fontSize: 11 }}>Admin</span>}
        <div className="nav-avatar">{initial}</div>
        <button className="btn btn-ghost btn-sm" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </nav>
  );
}
