import { defineConfig } from "vite";
import plugin from "@vitejs/plugin-react";
export default defineConfig({
  plugins: [plugin()],
  preview: {
    port: 4173,
    strictPort: true,
    proxy: { "/api": "http://127.0.0.1:8000" },
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: { "/api": "http://127.0.0.1:8000" },
  },
});
