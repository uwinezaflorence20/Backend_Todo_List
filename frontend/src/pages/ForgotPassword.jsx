import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/auth';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await forgotPassword({ email });
      setSent(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Something went wrong. Please try again.');
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
          <h1>Reset your password</h1>
          <p>We&apos;ll send a secure link to your email so you can set a new password.</p>
          <div className="auth-features">
            <div className="auth-feature">
              <div className="auth-feature-icon">📧</div>
              <span>Check your email for the reset link</span>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">⏱</div>
              <span>Link expires in 30 minutes</span>
            </div>
            <div className="auth-feature">
              <div className="auth-feature-icon">🔒</div>
              <span>Secure one-time use link</span>
            </div>
          </div>
        </div>
      </div>

      <div className="auth-form-side">
        <div className="auth-form-box">
          {sent ? (
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📬</div>
              <h2>Check your inbox</h2>
              <p className="auth-form-subtitle">
                If <strong>{email}</strong> is registered, you&apos;ll receive a reset link shortly.
              </p>
              <Link to="/login" className="btn btn-primary" style={{ marginTop: '1.5rem', display: 'inline-flex' }}>
                Back to Sign in
              </Link>
            </div>
          ) : (
            <>
              <h2>Forgot password?</h2>
              <p className="auth-form-subtitle">Enter your email and we&apos;ll send you a reset link</p>
              {error && <div className="alert alert-error mb-3">{error}</div>}
              <form onSubmit={handleSubmit}>
                <div className="form-group mb-3">
                  <label className="form-label">Email address</label>
                  <input
                    className="form-input"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="you@example.com"
                    required
                    autoFocus
                  />
                </div>
                <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
                  {loading ? 'Sending…' : 'Send reset link'}
                </button>
              </form>
              <p style={{ textAlign: 'center', marginTop: '1.25rem', fontSize: 14, color: 'var(--text-muted)' }}>
                <Link to="/login" style={{ color: 'var(--primary)', textDecoration: 'none', fontWeight: 600 }}>
                  ← Back to Sign in
                </Link>
              </p>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
