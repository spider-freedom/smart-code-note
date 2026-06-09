import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const currentTab = ref(0)

  function setTab(index) {
    currentTab.value = index
  }

  return { currentTab, setTab }
})
