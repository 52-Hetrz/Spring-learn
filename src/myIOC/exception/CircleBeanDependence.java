package myIOC.exception;

/**
 * @ClassName CircleBeanDependence
 * @Author Life
 * @Description
 * @Date 2024/3/16 11:21
 */
public class CircleBeanDependence extends Throwable{
    public CircleBeanDependence(Class<?> clazz1, Class<?> clazz2){
        super("CircleBeanDependence：依赖循环引用。相关类："+clazz1.getName()+"、"+clazz2.getName());
    }

    public CircleBeanDependence(Class<?> clazz){
        super("CircleBeanDependence：依赖循环引用。相关类："+clazz.getName());
    }
}
