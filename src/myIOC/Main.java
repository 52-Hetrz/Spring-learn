package myIOC;

/**
 * @ClassName Main
 * @Author Life
 * @Description
 * @Date 2024/3/15 10:19
 */
public class Main {


    public static void main(String[] args) {
        IOCContainer container = IOCContainer.getIocContainer();
        container.getBean(ServiceTwo.class).func1();

    }
}
