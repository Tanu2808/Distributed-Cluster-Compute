/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "Fira Code", "monospace"],
      },
      colors: {
        // Dark slate backgrounds
        surface: {
          950: "#060b18",
          900: "#0c1526",
          800: "#111d33",
          700: "#182540",
          600: "#1f3054",
        },
        // Accent — cyan
        accent: {
          400: "#22d3ee",
          500: "#06b6d4",
          600: "#0891b2",
        },
        // Violet for cluster/logical highlights
        cluster: {
          400: "#a78bfa",
          500: "#8b5cf6",
          600: "#7c3aed",
        },
        // Status colors
        status: {
          online: "#4ade80",
          busy: "#fbbf24",
          offline: "#64748b",
          unhealthy: "#f87171",
          registering: "#60a5fa",
        },
      },
      backgroundImage: {
        "gradient-radial": "radial-gradient(var(--tw-gradient-stops))",
        "cluster-gradient": "linear-gradient(135deg, #06b6d4 0%, #8b5cf6 100%)",
        "hero-glow":
          "radial-gradient(ellipse 80% 50% at 50% -20%, rgba(6,182,212,0.15) 0%, transparent 60%)",
      },
      animation: {
        "pulse-slow": "pulse 3s cubic-bezier(0.4,0,0.6,1) infinite",
        glow: "glow 2s ease-in-out infinite alternate",
        "slide-in-right": "slideInRight 0.3s ease-out",
        "fade-in": "fadeIn 0.4s ease-out",
      },
      keyframes: {
        glow: {
          "0%": { boxShadow: "0 0 5px rgba(6,182,212,0.3)" },
          "100%": {
            boxShadow:
              "0 0 20px rgba(6,182,212,0.8), 0 0 40px rgba(6,182,212,0.3)",
          },
        },
        slideInRight: {
          "0%": { transform: "translateX(100%)", opacity: "0" },
          "100%": { transform: "translateX(0)", opacity: "1" },
        },
        fadeIn: {
          "0%": { opacity: "0", transform: "translateY(8px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
      },
      boxShadow: {
        card: "0 4px 24px rgba(0,0,0,0.4)",
        "card-hover": "0 8px 40px rgba(0,0,0,0.6)",
        "glow-cyan": "0 0 20px rgba(6,182,212,0.4)",
        "glow-violet": "0 0 20px rgba(139,92,246,0.4)",
      },
    },
  },
  plugins: [
    // @tailwindcss/forms removed to avoid breaking changes; using custom form styles
  ],
};
