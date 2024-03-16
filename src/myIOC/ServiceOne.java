package myIOC;

import myIOC.annocation.MyAutowired;
import myIOC.annocation.MyComponent;

/**
 * @ClassName IOCOne
 * @Author Life
 * @Description
 * @Date 2024/3/15 10:14
 */
@MyComponent(name = "serviceOne")
public class ServiceOne {

    @MyAutowired
    ServiceTwo serviceTwo;

    public void function1(){
        System.out.println("---\tthis is serviceOne, function1\t---");
    }
}
