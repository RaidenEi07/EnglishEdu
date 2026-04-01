import { resolve } from 'path';
import { defineConfig } from 'vite';

export default defineConfig({
  root: '.',
  publicDir: 'public',
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.test.ts', 'pages/**/*.test.ts'],
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: {
      input: {
        home: resolve(__dirname, 'index.html'),
        login: resolve(__dirname, 'pages/login/index.html'),
        forgotPassword: resolve(__dirname, 'pages/forgot-password/index.html'),
        profile: resolve(__dirname, 'pages/profile/index.html'),
        dashboard: resolve(__dirname, 'pages/my/dashboard/index.html'),
        myCourses: resolve(__dirname, 'pages/my/courses/index.html'),
        adminDashboard: resolve(__dirname, 'pages/admin/index.html'),
        adminUsers: resolve(__dirname, 'pages/admin/users/index.html'),
        adminCourses: resolve(__dirname, 'pages/admin/courses/index.html'),
        adminEnrollments: resolve(__dirname, 'pages/admin/enrollments/index.html'),
        teacherDashboard: resolve(__dirname, 'pages/teacher/dashboard/index.html'),
        teacherStudents:  resolve(__dirname, 'pages/teacher/students/index.html'),
        courseDetail:     resolve(__dirname, 'pages/course/index.html'),
        learn:            resolve(__dirname, 'pages/learn/index.html'),
        grades:           resolve(__dirname, 'pages/my/grades/index.html'),
        manage:           resolve(__dirname, 'pages/manage/index.html'),
      },
    },
  },
  server: {
    port: 3000,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:4000',
        changeOrigin: true,
      },
    },
  },
});
