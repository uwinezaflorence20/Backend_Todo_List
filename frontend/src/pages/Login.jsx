import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { login } from '../api/auth';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const successMsg = location.state?.message;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await login(form);
      // JwtResponse returns { token, email } — store it and let AuthContext hydrate the rest
      signIn({ email: data.email }, data.token);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid email or password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-panel">
        <div className="auth-panel-inner">
          <div className="auth-panel-logo">
            <div className="auth-panel-icon">✓</div>
            <span className="auth-panel-name">TodoApp</span>
          </div>
          <h1>Stay on top of everything</h1>
          <p>Manage your tasks, track your progress, and never miss a deadline.</p>
          <div className="auth-features">
            <div className="auth-feature">
              <div className="auth-feature-icon">📋</div>
              <span>Create and organise todos with priorities</span>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">🔔</div>
              <span>Set due dates and get notified</span>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">📊</div>
              <span>Track your productivity with stats</span>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">🔒</div>
              <span>Secure and private by default</span>
            </div>
          </div>
        </div>
      </div>

      <div className="auth-form-side">
        <div className="auth-form-box">
          <h2>Welcome back</h2>
          <p className="auth-form-subtitle">Sign in to your account to continue</p>

          {successMsg && <div className="alert alert-success mb-3">{successMsg}</div>}
          {error && <div className="alert alert-error mb-3">{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Email address</label>
              <input
                className="form-input"
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="you@example.com"
                required
                autoFocus
              />
            </div>
            <div className="form-group mb-2">
              <label className="form-label">Password</label>
              <input
                className="form-input"
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="Your password"
                required
              />
            </div>
            <div style={{ textAlign: 'right', marginBottom: '1.25rem' }}>
              <Link to="/forgot-password" style={{ fontSize: 13, color: 'var(--primary)', textDecoration: 'none' }}>
                Forgot password?
              </Link>
            </div>
            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <p style={{ textAlign: 'center', marginTop: '1.25rem', fontSize: 14, color: 'var(--text-muted)' }}>
            Don&apos;t have an account?{' '}
            <Link to="/register" style={{ color: 'var(--primary)', textDecoration: 'none', fontWeight: 600 }}>
              Create one
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
