# 虾趣社区前端

基于 Vue 3、TypeScript、Vite、Pinia 和 Vue Router，由参考前端迁移而来；接口联调仍在进行。

本机已验证 Node.js 22.20.0；`test:unit` 使用 Node 的 TypeScript 类型剥离功能，其他 Node 版本的兼容性尚未验证。

## 本地运行

在 PowerShell 中进入本目录并执行：

```powershell
npm ci
npm run dev
```

也可将 `.env.example` 复制为 `.env.local`，按本机环境填写。配置中不要存放密钥。

开发服务器的 `/api` 请求默认转发至 `http://localhost:8080`。如网关运行在其他地址，在启动前设置环境变量，例如：

```powershell
$env:VITE_PROXY_TARGET = 'http://localhost:8080'
npm run dev
```

## 生产部署

默认构建使用同源 `/api` 作为请求前缀；部署服务需要将 `/api` 反向代理至网关并去掉 `/api` 前缀，同时将前端路由回退到 `index.html`。若浏览器需直接访问其他地址，在构建前设置 `VITE_API_BASE_URL` 为网关的完整地址或部署前缀，例如：

```powershell
$env:VITE_API_BASE_URL = 'https://example.com/gateway'
npm run build
```

`VITE_API_BASE_URL` 在构建时写入前端产物，变更后需重新构建；跨域时还需由实际网关配置允许对应前端来源。`VITE_PROXY_TARGET` 仅影响 Vite 开发代理，不影响生产构建。生产反向代理和跨域配置尚需在目标部署环境验证。

## 验证

```powershell
npm run type-check
npm run test:unit
npm run build
```

类型检查覆盖 `src` 下的 TypeScript 文件、全部 Vue 组件及 Vite 配置。类型检查通过不代表浏览器回归或后端联调已通过。业务接口需要启动对应的 Shrimp 网关、服务及其依赖；短信发送、上传和异步交互是否可用取决于实际环境。

当前认证服务代码仅生成并存储验证码，短信发送调用处于注释状态；因此点击获取验证码不代表手机会收到短信。用户已确认暂时保持现状。请勿在问题反馈或测试记录中发送验证码、令牌或认证日志。
