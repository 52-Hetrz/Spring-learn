package myIOC;

/**
 * @ClassName Main
 * @Author Life
 * @Description
 * @Date 2024/3/15 10:19
 */
public class Main {


    public static void main(String[] args) {
        IocContainer container = IocContainer.getIocContainer();
        container.getBean(ServiceTwo.class).func1();
    }
}
