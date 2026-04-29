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

  const isActive = (path) => location.pathname === path ? 'nav-link active' : 'nav-link';

  if (!user) return null;

  return (
    <nav className="navbar">
      <div className="nav-brand">
        <Link to="/dashboard">TodoApp</Link>
      </div>
      <div className="nav-links">
        <Link to="/dashboard" className={isActive('/dashboard')}>Todos</Link>
        <Link to="/profile" className={isActive('/profile')}>Profile</Link>
        {isAdmin && <Link to="/admin/users" className={isActive('/admin/users')}>Admin</Link>}
      </div>
      <div className="nav-user">
        <span className="nav-username">{user.username}</span>
        {isAdmin && <span className="badge badge-admin">Admin</span>}
        <button className="btn btn-ghost btn-sm" onClick={handleLogout}>Logout</button>
      </div>
    </nav>
  );
}
