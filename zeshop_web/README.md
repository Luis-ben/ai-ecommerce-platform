# Zeshop Vue storefront

Vue 3 + TypeScript client for the Zeshop rewrite. The storefront and `/admin/` management shell follow the BeikeShop default theme structure. The local demo catalog uses the original repository's licensed `catalog/demo` assets and seeded product/category/brand data; `BEIKESHOP-LICENSE.txt` is retained in this module.

Configure `VITE_API_BASE_URL` to exactly one backend: Python `http://127.0.0.1:8000` or Java `http://127.0.0.1:8080`.

```powershell
$env:npm_config_cache='D:\work\npm-cache'
npm install
npm run dev
```

The Vite preview uses port 5173 by default; if another local app owns it, use `npm run dev -- --port 5174` or another free port.
