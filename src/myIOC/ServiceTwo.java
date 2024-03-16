package myIOC;

import myIOC.annocation.MyAutowired;
import myIOC.annocation.MyComponent;

/**
 * @ClassName IOCTwo
 * @Author Life
 * @Description
 * @Date 2024/3/15 10:14
 */
@MyComponent
public class ServiceTwo {
    @MyAutowired
    ServiceOne serviceOne;

    public void func1(){
        System.out.println("---\tthis is serviceTwo, func1\t---");
        serviceOne.function1();

    }

}
