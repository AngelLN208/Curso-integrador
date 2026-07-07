/** @type {import('tailwindcss').Config} */
module.exports = {
    darkMode: ['selector', '[data-theme="dark"]'],
    content: [
        './frontend/views/**/*.html',
        './frontend/js/**/*.js',
        './portal/views/**/*.html',
        './portal/js/**/*.js',
    ],
    theme: {
        extend: {
            colors: {
                tinta: {
                    DEFAULT: '#14213D',
                    light: '#1E2E52',
                    dark: '#0D1526',
                },
                lienzo: {
                    DEFAULT: '#F7F8FA',
                    dark: '#111A2E',
                },
                guia: {
                    DEFAULT: '#FF7A45',
                    light: '#FF9B72',
                    dark: '#FF8A5C',
                },
                rumbo: {
                    DEFAULT: '#2F9E6E',
                    dark: '#4CC793',
                },
                alerta: {
                    DEFAULT: '#E5484D',
                },
                neblina: {
                    DEFAULT: '#8A94A6',
                    dark: '#6B7488',
                },
                superficie: {
                    DEFAULT: '#FFFFFF',
                    dark: '#182241',
                },
                borde: {
                    DEFAULT: '#E2E5EA',
                    dark: '#2A3350',
                },
            },
            fontFamily: {
                display: ['Sora', 'sans-serif'],
                sans: ['Inter', 'sans-serif'],
                mono: ['JetBrains Mono', 'monospace'],
            },
            borderRadius: {
                card: '10px',
            },
        },
    },
    plugins: [],
};