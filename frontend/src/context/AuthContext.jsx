import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { logout as apiLogout } from '../api/auth';
import { getProfile } from '../api/profile';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('user')); } catch { return null; }
  });

  // On mount (or whenever user has no username), fetch full profile from /api/profile.
  // This runs after login (which only stores email) and on page refresh.
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token || !user) return;
    if (user.username && user.role) return; // already hydrated

    getProfile()
      .then(({ data }) => {
        const full = { id: data.id, username: data.username, email: data.email, role: data.role };
        localStorage.setItem('user', JSON.stringify(full));
        setUser(full);
      })
      .catch(() => {
        // Token is invalid/expired — clear session
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setUser(null);
      });
  }, [user?.email]); // re-run when user email changes (e.g. after login)

  const signIn = useCallback((userData, token) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
  }, []);

  const signOut = useCallback(async () => {
    try { await apiLogout(); } catch { /* ignore */ }
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  }, []);

  const updateUser = useCallback((updates) => {
    const current = JSON.parse(localStorage.getItem('user') || '{}');
    const updated = { ...current, ...updates };
    localStorage.setItem('user', JSON.stringify(updated));
    setUser(updated);
  }, []);

  return (
    <AuthContext.Provider value={{
      user,
      signIn,
      signOut,
      updateUser,
      isAdmin: user?.role === 'ADMIN',
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
