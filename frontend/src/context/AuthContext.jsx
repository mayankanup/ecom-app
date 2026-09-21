import { createContext, useContext, useMemo, useState } from 'react';
import apiClient from '../api/client';

const AuthContext = createContext(null);

const STORAGE_KEY = 'auth';

function loadStoredAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(loadStoredAuth);

  const persist = (value) => {
    setAuth(value);
    if (value) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  };

  const login = async (email, password) => {
    const { data } = await apiClient.post('/api/auth/login', { email, password });
    persist(data);
    return data;
  };

  const register = async (name, email, password) => {
    await apiClient.post('/api/auth/register', { name, email, password });
    return login(email, password);
  };

  const logout = () => persist(null);

  const value = useMemo(
    () => ({
      user: auth ? { id: auth.id, name: auth.name, email: auth.email } : null,
      token: auth?.token ?? null,
      isAuthenticated: Boolean(auth?.token),
      login,
      register,
      logout,
    }),
    [auth],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
