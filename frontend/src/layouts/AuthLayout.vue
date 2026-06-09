<template>
  <main class="auth-layout">
    <section class="auth-layout__brand">
      <div class="auth-layout__logo">
        <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="grad" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
              <stop stop-color="#7c3aed"/>
              <stop offset="1" stop-color="#5b21b6"/>
            </linearGradient>
          </defs>
          <rect width="48" height="48" rx="12" fill="url(#grad)"/>
          <path d="M14 12h10l3 2.5v19l-3-2.5H14z" fill="#fff" fill-opacity=".95"/>
          <path d="M24 12h10v19l-3-2.5" fill="#fff" fill-opacity=".55"/>
          <circle cx="36" cy="14" r="2.5" fill="#ddd6fe"/>
        </svg>
      </div>
      <h1>智能笔记学习平台</h1>
      <p>AI 驱动的学习系统 — 把笔记变成可练习、可复习的知识闭环。</p>
    </section>
    <RouterView />
  </main>
</template>

<style scoped>
.auth-layout__logo {
  margin-bottom: 24px;
}

.auth-layout__logo svg {
  width: 48px;
  height: 48px;
}

.auth-layout__brand h1 {
  margin: 0 0 12px;
  color: var(--gray-900);
  font-size: 36px;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.auth-layout__brand p {
  margin: 0;
  color: var(--gray-500);
  font-size: 16px;
  line-height: 1.7;
  max-width: 380px;
}
</style>
