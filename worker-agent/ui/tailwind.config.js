/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Preserved for backwards compatibility with un-redesigned pages
        worker: {
          bg: '#f7f8fa',
          card: '#ffffff',
          primary: '#146eb4',
          primaryHover: '#0f5b94',
          text: '#16191f',
          muted: '#5f6b7a',
          border: '#d5dbdb',
        },
        // Enterprise Console Tokens (AWS Console Light Infrastructure)
        console: {
          bg: '#f7f8fa',
          topbar: '#ffffff',
          sidebar: '#ffffff',
          surface: '#ffffff',
          subtle: '#f3f4f6',
          card: '#ffffff',
          hover: '#f3f4f6',
          border: '#d5dbdb',
          borderSubtle: '#e9ecef',
          text: '#16191f',
          textMuted: '#5f6b7a',
          textDim: '#687078',
          accent: '#146eb4',
          accentHover: '#0f5b94',
        },
      },
      fontFamily: {
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
      }
    },
  },
  plugins: [],
}

