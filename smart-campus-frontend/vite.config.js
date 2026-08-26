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
  css: {
    preprocessorOptions: {
      scss: {
        // 全局注入响应式 mixin（断点唯一事实来源），组件内可直接 @include respond-to(md)
        additionalData: "@use '@/assets/styles/mixins.scss' as *;\n"
      }
    }
  },
  server: {
  host: true,                              // 监听 0.0.0.0，手机可访问
  proxy: {
    '/uploads': 'http://localhost:8080'    // 图片走电脑本机转发
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
