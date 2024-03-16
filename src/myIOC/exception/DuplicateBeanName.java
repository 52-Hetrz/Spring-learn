package myIOC.exception;

/**
 * @ClassName DuplicateBeanName
 * @Author Life
 * @Description
 * @Date 2024/3/15 23:28
 */
public class DuplicateBeanName extends Exception{

    public DuplicateBeanName(String beanName, Class<?> clazz1, Class<?> clazz2){
        super("DuplicateBeanName：bean名称——"+beanName+"，重复类："+clazz1.getName()+"、"+clazz2.getName());
    }
}
