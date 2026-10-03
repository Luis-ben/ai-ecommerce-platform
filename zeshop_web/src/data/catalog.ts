export type CatalogProduct = {
  id: string
  title: string
  description: string
  category: string
  brand: string
  price: number
  originPrice?: number
  stock: number
  image: string
  tag?: string
  sales: number
  brandTier?: 'CORE' | 'LUXURY'
}

export const demoProducts: CatalogProduct[] = [
  { id: '1', title: '欧洲站夏季新款时尚休闲短裤热裤女裤运动家具纯棉韩版宽松百搭裤', description: '轻盈透气的夏季休闲短裤，宽松剪裁适合日常出行。', category: '下装', brand: 'Burberry', price: 88.3, originPrice: 2312, stock: 2222, image: '/image/catalog/demo/product/1.webp', tag: 'Clearance', sales: 982 },
  { id: '2', title: '中长款牛仔半身裙女春夏季2021新款薄款高腰开叉包臀长裙A字裙子', description: '高腰开叉牛仔半身裙，通勤与休闲场景都适合。', category: '下装', brand: 'Versace', price: 324, originPrice: 699, stock: 546, image: '/image/catalog/demo/product/2.webp', tag: 'Hot', sales: 816 },
  { id: '3', title: '双肩包书包男女笔记本电脑包时尚潮流旅行背包', description: '轻便大容量旅行背包，适配笔记本电脑与日常通勤。', category: '配饰', brand: 'Saint Laurent', price: 299, originPrice: 599, stock: 444, image: '/image/catalog/demo/product/3.webp', sales: 721 },
  { id: '4', title: '男子 休闲鞋 TANJUN 天君 休闲鞋 运动鞋 812654', description: '简洁百搭的轻量休闲运动鞋，柔软鞋底提升日常舒适度。', category: '鞋履', brand: 'Chanel', price: 99, originPrice: 299, stock: 333, image: '/image/catalog/demo/product/4.webp', tag: 'Hot', sales: 654 },
  { id: '5', title: '高级感男装夏季潮牌美式复古短袖t恤男士重磅纯棉宽松半袖男体恤', description: '重磅纯棉面料，美式复古廓形，适合日常搭配。', category: '上装', brand: 'Balenciaga', price: 999, originPrice: 1299, stock: 421, image: '/image/catalog/demo/product/5.webp', sales: 498 },
  { id: '6', title: '冰丝褶皱垂感圆领长袖T恤男宽松薄款高级感v领男装上衣', description: '冰丝垂感面料，轻薄透气，打造简约高级的日常造型。', category: '上装', brand: 'Valentino', price: 423, originPrice: 699, stock: 123, image: '/image/catalog/demo/product/6.webp', sales: 462 },
  { id: '7', title: '轻便跑鞋女夏新款跑步运动鞋减震软底网面透气休闲运动鞋女鞋', description: '网面透气跑鞋，减震软底适合跑步和轻运动。', category: '运动户外', brand: 'Dior', price: 99, originPrice: 299, stock: 3333, image: '/image/catalog/demo/product/7.webp', tag: 'New', sales: 932 },
  { id: '8', title: '夏季新款模糊数码直喷短袖上衣男装情侣纯棉t恤ins潮', description: '纯棉短袖上衣，数码直喷图案，适合情侣搭配。', category: '上装', brand: 'Louis Vuitton', price: 99, originPrice: 333, stock: 2222, image: '/image/catalog/demo/product/8.webp', sales: 380 },
  { id: '9', title: '春夏新品暗黑系甜辣风吊带裙欧根纱蓬蓬裙冷淡法式连衣裙女', description: '法式欧根纱吊带连衣裙，轻盈裙摆展现优雅气质。', category: '女装', brand: 'Valentino', price: 299, originPrice: 999, stock: 99, image: '/image/catalog/demo/product/9.webp', tag: 'New', sales: 388 },
  { id: '10', title: '法式小众设计高级感连衣裙2022年新款收腰显瘦中长款裙子女夏', description: '收腰显瘦中长款连衣裙，小众设计适合夏日出行。', category: '女装', brand: 'Gucci', price: 169, originPrice: 334, stock: 33, image: '/image/catalog/demo/product/10.webp', sales: 319 },
  { id: '11', title: '夏季新款polo领连衣裙女学院风减龄不规则下摆长裙子', description: '学院风 Polo 领连衣裙，不规则裙摆俏皮又利落。', category: '女装', brand: 'Valentino', price: 98, originPrice: 234, stock: 2223, image: '/image/catalog/demo/product/11.webp', tag: 'Clearance', sales: 287 },
  { id: '12', title: '凉鞋女款夏季2022年新款坡跟女鞋夏款松糕鞋高跟鞋子爆款女士', description: '夏季坡跟凉鞋，舒适增高设计，适合度假和日常搭配。', category: '鞋履', brand: 'Valentino', price: 79, originPrice: 222, stock: 2222, image: '/image/catalog/demo/product/12.webp', sales: 251 },
  { id: '13', title: '休闲polo衬衫连衣裙大码女装高级感夏2022新款小个子御姐', description: '休闲 Polo 衬衫连衣裙，兼顾舒适度和高级感。', category: '女装', brand: 'Dior', price: 199, originPrice: 333, stock: 3445, image: '/image/catalog/demo/product/13.webp', sales: 243 },
  { id: '14', title: '夏季套装短袖T恤男装一套搭配帅气潮情侣男生半袖上衣服', description: '短袖 T 恤套装，轻松完成夏季情侣搭配。', category: '男装', brand: 'Armani', price: 299, originPrice: 433, stock: 423, image: '/image/catalog/demo/product/14.webp', sales: 211 },
  { id: '15', title: '男鞋2022夏季透气冲孔时尚休闲板鞋压花耐磨小白鞋男', description: '冲孔透气小白鞋，耐磨鞋底适合城市日常穿着。', category: '鞋履', brand: 'Prada', price: 324, originPrice: 599, stock: 546, image: '/image/catalog/demo/product/15.webp', tag: 'Hot', sales: 205 },
  { id: '35', title: '气质通勤高街蛋青色挺括烟管裤9分裤套装下装22秋女', description: '挺括烟管裤，利落剪裁适合通勤和正式场合。', category: '下装', brand: 'Gucci', price: 100.99, originPrice: 399, stock: 999, image: '/image/catalog/demo/product/16.webp', sales: 184 },
  { id: '39', title: '夏季新款女装法式气质洋气高级感温柔风吊带仙女连衣裙', description: '温柔风吊带仙女裙，轻盈面料呈现法式夏日气质。', category: '女装', brand: 'Saint Laurent', price: 100, originPrice: 3534, stock: 999, image: '/image/catalog/demo/product/18.webp', tag: 'New', sales: 172 },
  { id: '101', title: 'GG 马衔扣皮革乐福鞋', description: '奢华品牌演示目录商品，经典马衔扣元素与柔软皮革鞋面。', category: '奢华鞋履', brand: 'Gucci', price: 6890, originPrice: 7900, stock: 6, image: '/image/catalog/demo/product/4.webp', tag: 'Luxury', sales: 68, brandTier: 'LUXURY' },
  { id: '102', title: 'Re-Nylon 轻旅双肩包', description: '奢华品牌演示目录商品，轻量尼龙材质与城市通勤结构。', category: '奢华配饰', brand: 'Prada', price: 12600, originPrice: 13800, stock: 4, image: '/image/catalog/demo/product/3.webp', tag: 'Luxury', sales: 54, brandTier: 'LUXURY' },
  { id: '103', title: 'Oblique 印花旅行手袋', description: '奢华品牌演示目录商品，旅行容量与经典印花风格。', category: '奢华配饰', brand: 'Dior', price: 15800, originPrice: 17600, stock: 3, image: '/image/catalog/demo/product/5.webp', tag: 'Luxury', sales: 47, brandTier: 'LUXURY' },
  { id: '104', title: 'Monogram 经典旅行袋', description: '奢华品牌演示目录商品，适合短途出行的轻便旅行包。', category: '奢华配饰', brand: 'Louis Vuitton', price: 18500, originPrice: 19900, stock: 2, image: '/image/catalog/demo/product/6.webp', tag: 'Luxury', sales: 42, brandTier: 'LUXURY' },
  { id: '105', title: '羊皮链条肩背包', description: '奢华品牌演示目录商品，柔软菱格纹皮革与链条肩带。', category: '奢华配饰', brand: 'Chanel', price: 29800, originPrice: 32800, stock: 2, image: '/image/catalog/demo/product/9.webp', tag: 'Luxury', sales: 38, brandTier: 'LUXURY' },
  { id: '106', title: '真丝方巾限定配色', description: '奢华品牌演示目录商品，真丝面料与艺术图案。', category: '奢华配饰', brand: 'Hermès', price: 4200, originPrice: 4800, stock: 8, image: '/image/catalog/demo/product/10.webp', tag: 'Luxury', sales: 76, brandTier: 'LUXURY' },
  { id: '107', title: 'Kate 细链条肩背包', description: '奢华品牌演示目录商品，简洁晚宴造型与金属链条。', category: '奢华配饰', brand: 'Saint Laurent', price: 13200, originPrice: 14900, stock: 5, image: '/image/catalog/demo/product/11.webp', tag: 'Luxury', sales: 61, brandTier: 'LUXURY' },
  { id: '108', title: '编织皮革斜挎包', description: '奢华品牌演示目录商品，手工感编织皮革与日常容量。', category: '奢华配饰', brand: 'Bottega Veneta', price: 23600, originPrice: 25200, stock: 2, image: '/image/catalog/demo/product/12.webp', tag: 'Luxury', sales: 33, brandTier: 'LUXURY' },
  { id: '109', title: 'Triomphe 经典太阳镜', description: '奢华品牌演示目录商品，复古镜框与轻量镜片。', category: '奢华配饰', brand: 'Celine', price: 3600, originPrice: 4200, stock: 12, image: '/image/catalog/demo/product/13.webp', tag: 'Luxury', sales: 88, brandTier: 'LUXURY' },
  { id: '110', title: 'FF 标识羊毛围巾', description: '奢华品牌演示目录商品，羊毛混纺面料与经典标识。', category: '奢华配饰', brand: 'Fendi', price: 5800, originPrice: 6600, stock: 7, image: '/image/catalog/demo/product/14.webp', tag: 'Luxury', sales: 41, brandTier: 'LUXURY' },
  { id: '111', title: '复古方形腕表演示款', description: '奢华品牌演示目录商品，复古方形表盘与简洁皮表带。', category: '奢华腕表', brand: 'Cartier', price: 42600, originPrice: 46800, stock: 1, image: '/image/catalog/demo/product/15.webp', tag: 'Luxury', sales: 19, brandTier: 'LUXURY' },
  { id: '112', title: '经典潜水腕表演示款', description: '奢华品牌演示目录商品，运动风格与耐用表壳设计。', category: '奢华腕表', brand: 'Rolex', price: 76800, originPrice: 82000, stock: 1, image: '/image/catalog/demo/product/16.webp', tag: 'Luxury', sales: 12, brandTier: 'LUXURY' },
  { id: '113', title: 'Rockstud 铆钉高跟鞋', description: '奢华品牌演示目录商品，尖头鞋型与标志性铆钉装饰。', category: '奢华鞋履', brand: 'Valentino', price: 8900, originPrice: 9900, stock: 5, image: '/image/catalog/demo/product/7.webp', tag: 'Luxury', sales: 51, brandTier: 'LUXURY' },
  { id: '114', title: 'Hourglass 轮廓手袋', description: '奢华品牌演示目录商品，结构化轮廓与通勤容量。', category: '奢华配饰', brand: 'Balenciaga', price: 17400, originPrice: 18800, stock: 3, image: '/image/catalog/demo/product/17.webp', tag: 'Luxury', sales: 29, brandTier: 'LUXURY' },
  { id: '115', title: 'Peekaboo 手提包演示款', description: '奢华品牌演示目录商品，经典手提结构与可拆卸肩带。', category: '奢华配饰', brand: 'Fendi', price: 22900, originPrice: 24600, stock: 2, image: '/image/catalog/demo/product/18.webp', tag: 'Luxury', sales: 24, brandTier: 'LUXURY' },
  { id: '116', title: 'Puzzle 几何手袋演示款', description: '奢华品牌演示目录商品，几何拼接皮革与多种背法。', category: '奢华配饰', brand: 'Loewe', price: 19800, originPrice: 21600, stock: 3, image: '/image/catalog/demo/product/1.webp', tag: 'Luxury', sales: 31, brandTier: 'LUXURY' },
  { id: '117', title: '羽绒夹克限定演示款', description: '奢华品牌演示目录商品，轻量保暖填充与城市户外风格。', category: '奢华服饰', brand: 'Moncler', price: 15900, originPrice: 17600, stock: 4, image: '/image/catalog/demo/product/2.webp', tag: 'Luxury', sales: 36, brandTier: 'LUXURY' },
  { id: '118', title: 'Alexander McQueen 运动鞋', description: '奢华品牌演示目录商品，厚底鞋型与简洁城市风格。', category: '奢华鞋履', brand: 'Alexander McQueen', price: 7600, originPrice: 8500, stock: 6, image: '/image/catalog/demo/product/7.webp', tag: 'Luxury', sales: 44, brandTier: 'LUXURY' },
]

// match 命中商品数据里真实存在的 category，保证点击任一磁贴都能筛出商品。
export const homeCategories = [
  { name: '运动户外', match: '运动户外', sub: '运动户外系列', image: '/image/catalog/demo/multiple-img/1.webp' },
  { name: '时尚女装', match: '女装', sub: '当季女装', image: '/image/catalog/demo/multiple-img/2.webp' },
  { name: '男士精选', match: '男装', sub: '男装精选', image: '/image/catalog/demo/multiple-img/4.webp' },
  { name: '鞋履专区', match: '鞋履', sub: '鞋履专区', image: '/image/catalog/demo/multiple-img/5.webp' },
  { name: '配饰单品', match: '配饰', sub: '精致配饰', image: '/image/catalog/demo/multiple-img/6.webp' },
  { name: '下装精选', match: '下装', sub: '下装精选', image: '/image/catalog/demo/multiple-img/7.webp' },
]

const brandNames = ['Gucci', 'Valentino', 'Balenciaga', 'Saint Laurent', 'Louis Vuitton', 'Prada', 'Chanel', 'Dior', 'Armani', 'Burberry', 'Versace', 'Hermès', 'Bottega Veneta', 'Celine', 'Fendi', 'Cartier', 'Rolex', 'Loewe', 'Moncler', 'Alexander McQueen']
const brandAssets = [1, 10, 11, 12, 2, 3, 4, 5, 7, 9, 8, 6, 1, 2, 3, 4, 5, 6, 7, 8]

export const homeBrands = brandNames.map((name, index) => ({
  id: index + 1,
  name,
  image: `/image/catalog/demo/brands/${brandAssets[index]}.webp`,
  tier: index < 12 ? 'LUXURY' : 'PREMIUM',
}))
