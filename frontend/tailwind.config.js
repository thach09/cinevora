/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#080a12',
        panel: '#111522',
        line: '#22283a',
        accent: '#ef5da8',
        cyan: '#65d8ff',
      },
      boxShadow: {
        glow: '0 16px 60px rgba(239, 93, 168, 0.18)',
      },
    },
  },
  plugins: [],
}
