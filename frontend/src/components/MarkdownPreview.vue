<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps<{
  content?: string | null
}>()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }

    return hljs.highlightAuto(code).value
  },
})

const html = computed(() => md.render(props.content || ''))
</script>

<template>
  <div v-if="content" class="markdown-preview" v-html="html" />
  <el-empty v-else description="暂无内容" />
</template>
