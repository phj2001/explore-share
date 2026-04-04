import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

const getElementPlusChunkName = (id) => {
  if (id.includes('@element-plus/icons-vue')) {
    return 'vendor-ep-icons'
  }

  const componentMatch = id.match(/element-plus\/es\/components\/([^/]+)\//)
    || id.match(/element-plus\/lib\/components\/([^/]+)\//)

  if (componentMatch?.[1]) {
    return `vendor-ep-${componentMatch[1]}`
  }

  if (id.includes('element-plus/es') || id.includes('element-plus/lib')) {
    return 'vendor-element-base'
  }

  return 'vendor-misc'
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }

          if (id.includes('element-plus') || id.includes('@element-plus')) {
            return getElementPlusChunkName(id)
          }

          if (id.includes('vue-router')) {
            return 'vendor-router'
          }

          if (id.includes('pinia')) {
            return 'vendor-store'
          }

          if (id.includes('/node_modules/vue/') || id.includes('/node_modules/@vue/')) {
            return 'vendor-vue'
          }

          return 'vendor-misc'
        }
      }
    }
  }
})
