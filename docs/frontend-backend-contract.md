# 前后端接口契约清单

审计目标：保护既有 HMDP 前端静态页面和 Spring Boot 后端接口契约。后续工程化优化和 AI 改造必须遵守“新增接口，不破坏旧接口”的规则。

## 1. 项目接口形态

- 前端位置：`nginx-1.18.0/html/hmdp`
- 前端技术：静态 HTML + Vue + Axios + Element UI
- Axios 基础地址：`/api`
- 后端位置：`hm-dianping`
- 后端统一返回：`com.hmdp.dto.Result`

前端 `common.js` 响应拦截器会把 `response.data.success=false` 转为 Promise reject，把 `success=true` 的响应直接返回 `response.data`。因此前端页面普遍使用：

```js
axios.get(...).then(({ data }) => { ... })
```

也就是说，旧接口必须维持：

```json
{
  "success": true,
  "errorMsg": null,
  "data": "...",
  "total": null
}
```

## 2. 后端接口清单

| Controller | 方法 | HTTP | URL | 请求参数 | 请求体 | 返回类型 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `UserController` | `sendCode` | POST | `/user/code` | `phone` query | 无 | `Result` | 登录验证码 |
| `UserController` | `login` | POST | `/user/login` | 无 | `LoginFormDTO` | `Result` | 返回 token |
| `UserController` | `logout` | POST | `/user/logout` | 无 | 无 | `Result` | 当前返回功能未完成 |
| `UserController` | `me` | GET | `/user/me` | 无 | 无 | `Result` | 返回当前登录用户 |
| `UserController` | `sign` | POST | `/user/sign` | 无 | 无 | `Result` | 签到 |
| `UserController` | `signCount` | GET | `/user/sign/count` | 无 | 无 | `Result` | 连续签到天数 |
| `UserController` | `recordUv` | POST | `/user/uv` | 无 | 无 | `Result` | 记录 UV |
| `UserController` | `uvCount` | GET | `/user/uv/count` | 无 | 无 | `Result` | 查询 UV |
| `UserController` | `info` | GET | `/user/info/{id}` | `id` path | 无 | `Result` | 用户详情 |
| `UserController` | `queryUserById` | GET | `/user/{id}` | `id` path | 无 | `Result` | 用户主页基础信息 |
| `ShopTypeController` | `queryTypeList` | GET | `/shop-type/list` | 无 | 无 | `Result` | 店铺分类 |
| `ShopController` | `queryShopById` | GET | `/shop/{id}` | `id` path | 无 | `Result` | 店铺详情；当前存在 Result 嵌套风险 |
| `ShopController` | `saveShop` | POST | `/shop` | 无 | `Shop` | `Result` | 新增店铺 |
| `ShopController` | `updateShop` | PUT | `/shop` | 无 | `Shop` | `Result` | 更新店铺 |
| `ShopController` | `queryShopByType` | GET | `/shop/of/type` | `typeId,current,x,y` query | 无 | `Result` | 店铺列表，支持 GEO |
| `ShopController` | `loadShopDataToGeo` | POST | `/shop/geo/load` | 无 | 无 | `Result` | 导入 GEO 数据 |
| `ShopController` | `queryShopByName` | GET | `/shop/of/name` | `name,current` query | 无 | `Result` | 发布博客时搜索店铺 |
| `VoucherController` | `addVoucher` | POST | `/voucher` | 无 | `Voucher` | `Result` | 新增普通券 |
| `VoucherController` | `addSeckillVoucher` | POST | `/voucher/seckill` | 无 | `Voucher` | `Result` | 新增秒杀券 |
| `VoucherController` | `queryVoucherOfShop` | GET | `/voucher/list/{shopId}` | `shopId` path | 无 | `Result` | 店铺优惠券 |
| `VoucherOrderController` | `seckillVoucher` | POST | `/voucher-order/seckill/{id}` | `id` path | 无 | `Result` | 秒杀下单 |
| `BlogController` | `saveBlog` | POST | `/blog` | 无 | `Blog` | `Result` | 发布探店笔记 |
| `BlogController` | `likeBlog` | PUT | `/blog/like/{id}` | `id` path | 无 | `Result` | 点赞/取消点赞 |
| `BlogController` | `queryMyBlog` | GET | `/blog/of/me` | `current` query | 无 | `Result` | 我的博客 |
| `BlogController` | `queryHotBlog` | GET | `/blog/hot` | `current` query | 无 | `Result` | 热门博客；当前存在 Result 嵌套风险 |
| `BlogController` | `queryBlog` | GET | `/blog/{id}` | `id` path | 无 | `Result` | 博客详情；当前存在 Result 嵌套风险 |
| `BlogController` | `queryLikes` | GET | `/blog/likes/{id}` | `id` path | 无 | `Result` | 点赞用户；当前存在 Result 嵌套风险 |
| `BlogController` | `queryBlogOfFollow` | GET | `/blog/of/follow` | `lastId,offset` query | 无 | `Result` | Feed 滚动分页 |
| `BlogController` | `queryBlogByUserId` | GET | `/blog/of/user` | `id,current` query | 无 | `Result` | 指定用户博客 |
| `FollowController` | `follow` | PUT | `/follow/{id}/{isFollow}` | `id,isFollow` path | 无 | `Result` | 关注/取关 |
| `FollowController` | `isFollow` | GET | `/follow/or/not/{id}` | `id` path | 无 | `Result` | 是否关注 |
| `FollowController` | `followCommons` | GET | `/follow/common/{id}` | `id` path | 无 | `Result` | 共同关注 |
| `UploadController` | `uploadImage` | POST | `/upload/blog` | `file` multipart | 文件 | `Result` | 上传博客图片 |
| `UploadController` | `deleteBlogImg` | GET | `/upload/blog/delete` | `name` query | 无 | `Result` | 删除博客图片 |
| `BlogCommentsController` | 无公开方法 | - | `/blog-comments` | - | - | - | 当前未实现 |

## 3. 前端实际调用接口清单

| 页面 / 文件 | HTTP | URL | 参数 / 请求体 | 使用的返回字段 |
| --- | --- | --- | --- | --- |
| `login.html` | POST | `/user/login` | body: `phone,code,password` | `data` token |
| `login.html` | POST | `/user/code` | `phone` query | 仅判断成功 |
| `login2.html` | POST | `/user/login` | body: `phone,code,password` | `data` token |
| `index.html` | GET | `/shop-type/list` | 无 | `data[].id,name,icon` |
| `index.html` | GET | `/blog/hot` | `current` | `data[].id,name,icon,images,title,liked,isLike` |
| `index.html` | PUT | `/blog/like/{id}` | path id | 仅判断成功 |
| `index.html` | GET | `/blog/{id}` | path id | `data` blog |
| `shop-list.html` | GET | `/shop-type/list` | 无 | `data[].id,name` |
| `shop-list.html` | GET | `/shop/of/type` | `typeId,current,x,y` | `data[].id,name,images,area,avgPrice,distance` |
| `shop-detail.html` | GET | `/shop/{id}` | path id | `data.id,name,images,score,address,openHours,x,y` |
| `shop-detail.html` | GET | `/voucher/list/{shopId}` | path shopId | `data[].id,stock,beginTime,endTime,payValue,actualValue` |
| `shop-detail.html` | POST | `/voucher-order/seckill/{id}` | path voucher id | `data` orderId |
| `blog-detail.html` | GET | `/blog/{id}` | path id | `data.id,userId,shopId,name,icon,title,images,content,liked,isLike` |
| `blog-detail.html` | GET | `/shop/{shopId}` | path shopId | `data.name,images,address` |
| `blog-detail.html` | GET | `/blog/likes/{id}` | path blog id | `data[].id,icon` |
| `blog-detail.html` | PUT | `/blog/like/{id}` | path blog id | 仅判断成功 |
| `blog-detail.html` | GET | `/follow/or/not/{userId}` | path userId | `data` boolean |
| `blog-detail.html` | PUT | `/follow/{userId}/{isFollow}` | path userId/isFollow | 仅判断成功 |
| `blog-detail.html` | GET | `/user/me` | 无 | `data.id,nickName,icon` |
| `blog-edit.html` | GET | `/shop/of/name` | `name` | `data[].id,name,address` |
| `blog-edit.html` | POST | `/blog` | body: blog form | 仅判断成功 |
| `blog-edit.html` | POST | `/upload/blog` | multipart `file` | `data` image path |
| `blog-edit.html` | GET | `/upload/blog/delete` | `name` | 仅判断成功 |
| `blog-edit.html` | GET | `/user/me` | 无 | 登录状态 |
| `info.html` | GET | `/blog/of/me` | `current` 默认 | `data[]` blog |
| `info.html` | GET | `/blog/of/follow` | `lastId,offset` | `data.list,minTime,offset` |
| `info.html` | GET | `/user/me` | 无 | `data.id,nickName,icon` |
| `info.html` | GET | `/user/info/{id}` | path id | 用户详情字段 |
| `info.html` | POST | `/user/logout` | 无 | 仅判断成功；当前后端返回失败 |
| `info.html` | PUT | `/blog/like/{id}` | path blog id | 仅判断成功 |
| `info.html` | GET | `/blog/{id}` | path blog id | `data` blog |
| `other-info.html` | GET | `/blog/of/user` | `id,current` | `data[]` blog |
| `other-info.html` | GET | `/user/me` | 无 | 当前用户 |
| `other-info.html` | GET | `/user/{id}` | path id | `data.id,nickName,icon` |
| `other-info.html` | GET | `/user/info/{id}` | path id | 用户详情字段 |
| `other-info.html` | GET | `/follow/or/not/{id}` | path id | `data` boolean |
| `other-info.html` | GET | `/follow/common/{id}` | path id | `data[].id,nickName,icon` |
| `other-info.html` | PUT | `/follow/{id}/{isFollow}` | path id/isFollow | 仅判断成功 |
| `info-edit.html` | GET | `/user/me` | 无 | 当前用户 |

## 4. 前后端匹配情况

### 完全匹配

- `/user/login`
- `/user/code`
- `/user/me`
- `/user/info/{id}`
- `/user/{id}`
- `/shop-type/list`
- `/shop/of/type`
- `/shop/of/name`
- `/voucher/list/{shopId}`
- `/voucher-order/seckill/{id}`
- `/blog`
- `/blog/like/{id}`
- `/blog/of/me`
- `/blog/of/follow`
- `/blog/of/user`
- `/follow/or/not/{id}`
- `/follow/common/{id}`
- `/follow/{id}/{isFollow}`
- `/upload/blog`
- `/upload/blog/delete`

### 可能不匹配或高风险

- `/shop/{id}`：Controller 当前 `Result.ok(shopService.queryById(id))`，而 Service 已返回 `Result`，前端期望 `data` 是 Shop，实际可能变成嵌套 Result。
- `/blog/hot`：Controller 当前二次包装 Service `Result`，前端期望 `data` 是博客数组，实际可能嵌套。
- `/blog/{id}`：Controller 当前二次包装 Service `Result`，前端期望 `data` 是 Blog，实际可能嵌套。
- `/blog/likes/{id}`：Controller 当前二次包装 Service `Result`，前端期望 `data` 是 UserDTO 数组，实际可能嵌套。
- `/user/logout`：前端调用该接口后期望成功并清理 token；后端当前固定返回失败“功能未完成”。
- 匿名接口：`RefreshTokenIntercepter` 当前无 token 时直接 `return false`，可能导致 `/user/code`、`/user/login`、`/shop/**`、`/blog/hot` 等匿名接口被提前拦截。

### 前端调用但后端不存在

未发现明确的前端调用但后端完全不存在的接口。

### 后端存在但前端未使用

- `/user/sign`
- `/user/sign/count`
- `/user/uv`
- `/user/uv/count`
- `/shop`
- `/shop/geo/load`
- `/voucher`
- `/voucher/seckill`
- `/blog-comments` 当前无方法

## 5. 高风险核心接口

| 业务 | 接口 | 风险等级 | 保护要求 |
| --- | --- | --- | --- |
| 登录 | `POST /user/code`, `POST /user/login`, `GET /user/me` | P0 | URL、方法、token 返回结构不能变 |
| 店铺列表 | `GET /shop/of/type` | P0 | 查询参数和列表字段不能变 |
| 店铺详情 | `GET /shop/{id}` | P0 | 前端期望 `data` 为 Shop，禁止返回嵌套 Result |
| 优惠券展示 | `GET /voucher/list/{shopId}` | P0 | 前端秒杀按钮依赖券字段 |
| 秒杀下单 | `POST /voucher-order/seckill/{id}` | P0 | 成功时 `data` 为 orderId |
| 博客列表 | `GET /blog/hot`, `GET /blog/of/me`, `GET /blog/of/user` | P0 | 前端期望 `data` 为 blog 数组 |
| 博客详情 | `GET /blog/{id}` | P0 | 前端期望 `data` 为 Blog |
| 点赞 | `PUT /blog/like/{id}`, `GET /blog/likes/{id}` | P0 | 点赞后刷新详情和点赞列表 |
| 关注 | `GET /follow/or/not/{id}`, `PUT /follow/{id}/{isFollow}`, `GET /follow/common/{id}` | P0 | 关注状态和共同关注列表不能变 |
| Feed | `GET /blog/of/follow` | P0 | 前端依赖 `data.list/minTime/offset` |

## 6. 后续禁止破坏的接口列表

以下接口属于前端已使用契约，后续不得修改 URL、HTTP 方法、参数和基础返回结构：

```text
POST /user/code
POST /user/login
POST /user/logout
GET  /user/me
GET  /user/info/{id}
GET  /user/{id}
GET  /shop-type/list
GET  /shop/{id}
GET  /shop/of/type
GET  /shop/of/name
GET  /voucher/list/{shopId}
POST /voucher-order/seckill/{id}
POST /blog
PUT  /blog/like/{id}
GET  /blog/of/me
GET  /blog/hot
GET  /blog/{id}
GET  /blog/likes/{id}
GET  /blog/of/follow
GET  /blog/of/user
GET  /follow/or/not/{id}
GET  /follow/common/{id}
PUT  /follow/{id}/{isFollow}
POST /upload/blog
GET  /upload/blog/delete
```

AI 功能必须新增到 `/ai/**`，不能复用或改造上述旧接口。
