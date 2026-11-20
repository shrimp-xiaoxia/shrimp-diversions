import { createVNode, render } from 'vue'
import Message from '@/components/common/Message.vue'

const messageInstance = createVNode(Message)
const container = document.createElement('div')
document.body.appendChild(container)
render(messageInstance, container)

export const message = {
  show(msg: string, duration?: number) {
    (messageInstance.component?.exposed as { show: (message: string, duration?: number) => void } | null)?.show(msg, duration)
  }
} 