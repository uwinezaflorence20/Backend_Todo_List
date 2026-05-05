import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../api/auth';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.newPassword !== form.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setLoading(true);
    try {
      await resetPassword({ token, newPassword: form.newPassword });
      navigate('/login', { state: { message: 'Password reset! You can now sign in with your new password.' } });
    } catch (err) {
      setError(err.response?.data?.message || 'Reset failed. The link may have expired. try again');
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-panel">
          <div className="auth-panel-inner">
            <div className="auth-panel-logo">
              <div className="auth-panel-icon">✓</div>
              <span className="auth-panel-name">TodoApp</span>
            </div>
          </div>
        </div>
        <div className="auth-form-side">
          <div className="auth-form-box" style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>⚠️</div>
            <h2>Invalid link</h2>
            <p className="auth-form-subtitle">This reset link is invalid or has already been used.</p>
            <Link to="/forgot-password" className="btn btn-primary" style={{ marginTop: '1.5rem', display: 'inline-flex' }}>
              Request a new link
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-panel">
        <div className="auth-panel-inner">
          <div className="auth-panel-logo">
            <div className="auth-panel-icon">✓</div>
            <span className="auth-panel-name">TodoApp</span>
          </div>
          <h1>Create a new password</h1>
          <p>Choose a strong password to keep your account secure.</p>
          <div className="auth-features">
            <div className="auth-feature">
              <div className="auth-feature-icon">🔑</div>
              <span>Minimum 8 characters</span>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">🛡</div>
              <span>One-time secure reset link</span>
            </div>
          </div>
        </div>
      </div>
      <div className="auth-form-side">
        <div className="auth-form-box">
          <h2>Set new password</h2>
          <p className="auth-form-subtitle">Enter and confirm your new password below</p>
          {error && <div className="alert alert-error mb-3">{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">New password</label>
              <input
                className="form-input"
                type="password"
                value={form.newPassword}
                onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
                placeholder="8–40 characters"
                required
                autoFocus
              />
            </div>
            <div className="form-group mb-3">
              <label className="form-label">Confirm new password</label>
              <input
                className="form-input"
                type="password"
                value={form.confirmPassword}
                onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                placeholder="Repeat new password"
                required
              />
            </div>
            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? 'Resetting…' : 'Reset password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
