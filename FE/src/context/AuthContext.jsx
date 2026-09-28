import React, { createContext, useState, useEffect, useContext } from 'react';
import authService from '../services/auth.service';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const savedToken = authService.getToken();
    const savedUser = authService.getCurrentUser();
    if (savedToken && savedUser) {
      setToken(savedToken);
      setUser(savedUser);
    }
    setLoading(false);
  }, []);

  const login = async (username, password) => {
    const data = await authService.login(username, password);
    const currentUser = authService.getCurrentUser();
    const currentToken = authService.getToken();
    setUser(currentUser);
    setToken(currentToken);
    return data;
  };

  const register = async (registerData) => {
    return await authService.register(registerData);
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
    setToken(null);
  };

  const hasRole = (role) => {
    if (!user) return false;
    const userRole = user.role || (user.roles && user.roles[0]);
    if (!userRole) return false;
    const normalizedRole = userRole.replace('ROLE_', '');
    return normalizedRole === role;
  };

  const isAdmin = () => hasRole('ADMIN');
  const isProductOwner = () => hasRole('PRODUCT_OWNER') || hasRole('ADMIN');
  const isCustomer = () => hasRole('CUSTOMER');

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        login,
        register,
        logout,
        hasRole,
        isAdmin,
        isProductOwner,
        isCustomer,
        isAuthenticated: !!token,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
