import { useState, useEffect } from 'react';
import { getProfile, updateProfile, changePassword } from '../api/profile';
import { useAuth } from '../context/AuthContext';

export default function Profile() {
  const { user, updateUser } = useAuth();
  const [profile, setProfile] = useState({ username: '', email: '' });
  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [profileMsg, setProfileMsg] = useState(null);
  const [pwMsg, setPwMsg] = useState(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [pwLoading, setPwLoading] = useState(false);

  useEffect(() => {
    getProfile()
      .then(({ data }) => setProfile({ username: data.username, email: data.email }))
      .catch(() => {});
  }, []);

  const handleProfileSave = async (e) => {
    e.preventDefault();
    setProfileMsg(null);
    setProfileLoading(true);
    try {
      const { data } = await updateProfile(profile);
      updateUser({ username: data.username, email: data.email });
      setProfileMsg({ type: 'success', text: 'Profile updated successfully.' });
    } catch (err) {
      setProfileMsg({ type: 'error', text: err.response?.data?.message || 'Update failed' });
    } finally {
      setProfileLoading(false);
    }
  };

  const handlePasswordSave = async (e) => {
    e.preventDefault();
    setPwMsg(null);
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setPwMsg({ type: 'error', text: 'New passwords do not match.' });
      return;
    }
    setPwLoading(true);
    try {
      await changePassword({ currentPassword: pwForm.currentPassword, newPassword: pwForm.newPassword });
      setPwMsg({ type: 'success', text: 'Password changed successfully.' });
      setPwForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setPwMsg({ type: 'error', text: err.response?.data?.message || 'Password change failed' });
    } finally {
      setPwLoading(false);
    }
  };

  const initial = (user?.username || user?.email || '?')[0].toUpperCase();

  return (
    <div className="page">
      {/* Hero header */}
      <div className="profile-header">
        <div className="profile-avatar">{initial}</div>
        <div className="profile-info">
          <h2>{user?.username || 'User'}</h2>
          <span>{user?.email}</span>
          {user?.role && (
            <span
              style={{
                marginLeft: 8,
                background: 'rgba(255,255,255,0.2)',
                padding: '1px 8px',
                borderRadius: 99,
                fontSize: 11,
                fontWeight: 700,
                textTransform: 'uppercase',
                letterSpacing: '.4px',
              }}
            >
              {user.role}
            </span>
          )}
        </div>
      </div>

      <div className="profile-grid">
        {/* Account details */}
        <div className="card">
          <div className="card-title">Account details</div>
          {profileMsg && (
            <div className={`alert alert-${profileMsg.type} mb-3`}>{profileMsg.text}</div>
          )}
          <form onSubmit={handleProfileSave}>
            <div className="form-group">
              <label className="form-label">Username</label>
              <input
                className="form-input"
                type="text"
                value={profile.username}
                onChange={(e) => setProfile({ ...profile, username: e.target.value })}
                required
              />
            </div>
            <div className="form-group mb-3">
              <label className="form-label">Email address</label>
              <input
                className="form-input"
                type="email"
                value={profile.email}
                onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary" disabled={profileLoading}>
              {profileLoading ? 'Saving…' : 'Save changes'}
            </button>
          </form>
        </div>

        {/* Change password */}
        <div className="card">
          <div className="card-title">Change password</div>
          {pwMsg && (
            <div className={`alert alert-${pwMsg.type} mb-3`}>{pwMsg.text}</div>
          )}
          <form onSubmit={handlePasswordSave}>
            <div className="form-group">
              <label className="form-label">Current password</label>
              <input
                className="form-input"
                type="password"
                value={pwForm.currentPassword}
                onChange={(e) => setPwForm({ ...pwForm, currentPassword: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">New password</label>
              <input
                className="form-input"
                type="password"
                value={pwForm.newPassword}
                onChange={(e) => setPwForm({ ...pwForm, newPassword: e.target.value })}
                placeholder="8–40 characters"
                required
              />
            </div>
            <div className="form-group mb-3">
              <label className="form-label">Confirm new password</label>
              <input
                className="form-input"
                type="password"
                value={pwForm.confirmPassword}
                onChange={(e) => setPwForm({ ...pwForm, confirmPassword: e.target.value })}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary" disabled={pwLoading}>
              {pwLoading ? 'Updating…' : 'Change password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
