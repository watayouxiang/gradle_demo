# gradle_demo

gradle_router 页面路由框架：

- Annotation + APT
  - Anotation：标记路由页面
  - APT：生成 RouterMapping_xxx.class、mapping_xxx.json
- Plugin + Transform + AMS
  - Plugin：根据 mapping_xxx.json，生成 RouterMapping.md
  - Transform：收集所有 RouterMapping_xxx.class
  - AMS：生成 RouterMapping.class

