/**
 * 后台编译样式的引用计数管理。
 *
 * 每个后台页面都会渲染自己的 AdminShell，页面间跳转时外壳先卸载再挂载；
 * 如果卸载就立刻移除 <link>，浏览器会在新外壳挂载前重排一次，视觉上就是「一点击就闪」。
 * 所以采用引用计数 + 延迟回收：跳转过程中计数回到 1 就不移除，真正离开后台才回收。
 */
const ADMIN_STYLES = ['/beike/admin/css/bootstrap.css', '/beike/admin/css/app.css']

let refCount = 0
let links: HTMLLinkElement[] = []
let releaseTimer = 0

export function acquireAdminStyles(): void {
  refCount += 1
  window.clearTimeout(releaseTimer)
  if (links.length) return
  links = ADMIN_STYLES.map((href) => {
    const link = document.createElement('link')
    link.rel = 'stylesheet'
    link.href = href
    link.dataset.adminStyle = href
    document.head.appendChild(link)
    return link
  })
}

export function releaseAdminStyles(): void {
  refCount = Math.max(0, refCount - 1)
  window.clearTimeout(releaseTimer)
  releaseTimer = window.setTimeout(() => {
    if (refCount > 0) return
    links.splice(0).forEach((link) => link.remove())
  }, 150)
}
