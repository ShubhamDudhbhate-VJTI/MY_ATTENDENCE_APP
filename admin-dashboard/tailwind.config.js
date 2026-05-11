/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        facebook: '#1877F2',
        secondary: '#65676B',
        accent: '#00E5FF',
      }
    },
  },
  plugins: [],
}
