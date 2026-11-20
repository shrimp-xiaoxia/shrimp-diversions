import defaultAvatar from '@/assets/avatar.png'

export { defaultAvatar }

export function useDefaultAvatar(event: Event) {
  const image = event.currentTarget
  if (image instanceof HTMLImageElement && image.src !== new URL(defaultAvatar, document.baseURI).href) {
    image.src = defaultAvatar
  }
}
