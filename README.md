# Spring学习造轮子过程
## 第一部分：IOC容器
文件路径：src/myIOC
IOCContainer是主要的容器类。采用单例模式，使用反射获取类的注解、字段的注解、字段的类型、给类的字段赋值。
初始化分为两步：
* 首先扫描指定的包，获取所有的需要管理的类信息。在扫描过程中如果可以创建该bean，则直接创建。
* 扫描完毕之后，再根据构建的依赖关系，创建整个Bean池。
