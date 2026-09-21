import axios from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

apiClient.interceptors.request.use((config) => {
  try {
    const auth = JSON.parse(localStorage.getItem('auth'));
    if (auth?.token) {
      config.headers.set('Authorization', `Bearer ${auth.token}`);
    }
  } catch {
    // no stored auth, proceed unauthenticated
  }
  return config;
});

export default apiClient;
