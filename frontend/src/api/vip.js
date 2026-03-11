import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('VIP API Error:', error);
    return Promise.reject(error);
  }
);

export const getVipPackages = () => {
  return apiClient
    .get('/vip/packages')
    .then((response) => response.data)
    .catch(() => []);
};
