import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { authApi, extractErrorMessage } from '../common/api';

const TOKEN_STORAGE_KEY = 'lp_token';
const USER_STORAGE_KEY = 'lp_user';
const THEME_STORAGE_KEY = 'lp_theme';

const AuthContext = createContext(null);

const safeJsonParse = (value, fallback = null) => {
  try {
    return value ? JSON.parse(value) : fallback;
  } catch (error) {
    return fallback;
  }
};

export const normalizeRole = (source) => {
  if (!source) {
    return 'USER';
  }

  const raw = Array.isArray(source) ? source[0] : source;

  if (typeof raw === 'string') {
    return raw.replace('ROLE_', '').toUpperCase();
  }

  return 'USER';
};

const normalizeUser = (rawUser = {}) => ({
  id: rawUser.id || null,
  username: rawUser.username || '',
  fullName: rawUser.fullName || 'Пользователь',
  email: rawUser.email || '',
  passportNumber: rawUser.passportNumber || '',
  phoneNumber: rawUser.phoneNumber || '',
  address: rawUser.address || '',
  applicantId: rawUser.applicantId || null,
  role: normalizeRole(rawUser.role),
  avatar: rawUser.avatar || '',
});

const buildSession = (responseData) => {
  const payload = responseData?.data || responseData;
  const token = payload?.token || payload?.accessToken || '';
  const user = normalizeUser(payload?.user || payload);

  return { token, user, role: user.role };
};

const getStoredUser = () => safeJsonParse(localStorage.getItem(USER_STORAGE_KEY), null);

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_STORAGE_KEY) || '');
  const [user, setUser] = useState(() => getStoredUser());
  const [theme, setTheme] = useState(() => localStorage.getItem(THEME_STORAGE_KEY) || 'dark');
  const [authLoading, setAuthLoading] = useState(Boolean(localStorage.getItem(TOKEN_STORAGE_KEY)));

  const role = useMemo(() => normalizeRole(user?.role), [user?.role]);
  const isAuthenticated = Boolean(token && user);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  const persistSession = useCallback((session) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, session.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(session.user));
    setToken(session.token);
    setUser(session.user);
  }, []);

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setToken('');
    setUser(null);
  }, []);

  const getDefaultRoute = useCallback(
    (candidateRole = role) => (normalizeRole(candidateRole) === 'ADMIN' ? '/admin' : '/dashboard'),
    [role],
  );

  const refreshProfile = useCallback(async () => {
    if (!localStorage.getItem(TOKEN_STORAGE_KEY)) {
      setAuthLoading(false);
      return null;
    }

    try {
      const currentUser = await authApi.getMe();
      const normalizedUser = normalizeUser(currentUser);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(normalizedUser));
      setUser(normalizedUser);
      return normalizedUser;
    } catch (error) {
      clearSession();
      return null;
    } finally {
      setAuthLoading(false);
    }
  }, [clearSession]);

  useEffect(() => {
    refreshProfile();
  }, [refreshProfile]);

  const login = useCallback(
    async ({ identifier, password }) => {
      const response = await authApi.login({
        identifier: identifier.trim(),
        password,
      });

      const session = buildSession(response);

      if (!session.token) {
        throw new Error('Бэкенд не вернул JWT токен после логина');
      }

      persistSession(session);
      const from = location.state?.from?.pathname;
      navigate(from || getDefaultRoute(session.role), { replace: true });

      return session;
    },
    [getDefaultRoute, location.state, navigate, persistSession],
  );

  const register = useCallback(
    async (formData) => {
      const response = await authApi.register({
        fullName: formData.fullName,
        passportNumber: formData.passportNumber,
        phoneNumber: formData.phoneNumber,
        email: formData.email,
        address: formData.address,
        password: formData.password,
      });

      const session = buildSession(response);

      if (!session.token) {
        throw new Error('Бэкенд не вернул JWT токен после регистрации');
      }

      persistSession(session);
      navigate(getDefaultRoute(session.role), { replace: true });
      return session;
    },
    [getDefaultRoute, navigate, persistSession],
  );

  const updateProfile = useCallback(
    async (payload) => {
      const updatedUser = normalizeUser(await authApi.updateMe(payload));
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(updatedUser));
      setUser(updatedUser);
      return updatedUser;
    },
    [],
  );

  const logout = useCallback(() => {
    clearSession();
    navigate('/login', { replace: true });
  }, [clearSession, navigate]);

  const toggleTheme = useCallback(() => {
    setTheme((current) => (current === 'dark' ? 'light' : 'dark'));
  }, []);

  const value = useMemo(
    () => ({
      user,
      token,
      role,
      theme,
      isAuthenticated,
      authLoading,
      login,
      register,
      logout,
      toggleTheme,
      getDefaultRoute,
      updateProfile,
      refreshProfile,
      extractErrorMessage,
    }),
    [
      authLoading,
      getDefaultRoute,
      isAuthenticated,
      login,
      logout,
      refreshProfile,
      register,
      role,
      theme,
      token,
      toggleTheme,
      updateProfile,
      user,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth должен использоваться внутри AuthProvider');
  }

  return context;
};
