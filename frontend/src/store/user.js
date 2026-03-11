import { ref } from 'vue';

const isLoggedIn = ref(false);

export const useUserStore = () => {
  return {
    isLoggedIn,
  };
};
