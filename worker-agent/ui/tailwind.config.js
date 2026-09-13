/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        worker: {
          bg: '#0f172a',
          card: '#1e293b',
          primary: '#10b981',
          primaryHover: '#059669',
          text: '#f8fafc',
          muted: '#94a3b8',
          border: '#334155'
        }
      }
    },
  },
  plugins: [],
}
