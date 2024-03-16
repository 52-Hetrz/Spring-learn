package myIOC;

import myIOC.annocation.MyAutowired;
import myIOC.annocation.MyComponent;
import myIOC.exception.CircleBeanDependence;
import myIOC.exception.DuplicateBeanName;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @ClassName IOCMain
 * @Author Life
 * @Description
 * @Date 2024/3/15 09:23
 */
public class IocContainer {

    // IOC容器单例
    private static final IocContainer IOC_CONTAINER;

    static {
        try {
            IOC_CONTAINER = new IocContainer();
        } catch (DuplicateBeanName | CircleBeanDependence e) {
            throw new RuntimeException(e);
        }
    }

    public static IocContainer getIocContainer() {
        return IOC_CONTAINER;
    }
    // 类和对应的Bean
    private final Map<Class<?>, Object> CLAZZ_BEAN_MAP = new HashMap<>();
    // 名称和对应的Bean
    private final Map<String, Object> NAME_BEAN_MAP = new HashMap<>();
    // 每一个类对应的依赖信息。<Bean类,<类字段，字段类对象>>
    private final Map<Class<?>, Map<Field, Class<?>>> BEAN_DEPENDENT_INFO = new HashMap<>();

    /**
     * IocContainer的构造函数。<br>
     * 将当前目录下的bean初始化到容器当中
     */
    private IocContainer() throws DuplicateBeanName, CircleBeanDependence {
        // 构建类依赖信息
        this.buildDependenceInfo();
        // 注入bean
        try {
            initBeansWithDependence();
        }catch (InstantiationException | IllegalAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * 构建每一个类的依赖关系，如果遇到可以直接注入的类则直接创建bean。
     */
    private void buildDependenceInfo() throws DuplicateBeanName{
        // 获取当前JVM正在运行的classpath
        URL classPath = ClassLoader.getSystemClassLoader().getResource("");
        try(URLClassLoader loader = new URLClassLoader(new URL[]{classPath})) {
            Package pack = IocContainer.class.getPackage();
            List<String> classNameList = getClassNames(pack.getName());
            for (String className:classNameList){
                Class<?> clazz = loader.loadClass(className);
                MyComponent annotation = getAnnotation(clazz, MyComponent.class);
                if (annotation != null){
                    // 提取当前类的依赖列表
                    Map<Field, Class<?>> dependenceInfo = getClassDependenceInfo(clazz);
                    BEAN_DEPENDENT_INFO.put(clazz, dependenceInfo);
                    // 检查当前bean是否可以直接创建
                    if (checkDependence(new HashSet<>(dependenceInfo.values()))){
                        String name = annotation.name();
                        createBean(clazz, name, dependenceInfo);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e){
            e.printStackTrace();
        }
    }

    /**
     * 根据构建好的依赖关系，初始化IOC容器当中的所有bean
     */
    private void initBeansWithDependence() throws DuplicateBeanName, CircleBeanDependence, InstantiationException, IllegalAccessException {
        Map<Class<?>, Byte> classStatusMap = new HashMap<>();
        for (Class<?> clazz:BEAN_DEPENDENT_INFO.keySet()){
            initBeansWhitDependenceRecursion(classStatusMap, clazz);
        }
    }

    /**
     * 递归的构建bean信息
     * @param classStatusMap 每一个类当前的状态。
     *                       优先根据CLAZZ_BEAN_MAP有没有来判断是否已经加载。
     *                       如果classStatusMap中为0表示正在处理当前类的依赖关系；
     *                       如果classStatusMap中为1表示已经初始化成功
     * @param curClazz 当前要处理的类
     */
    private void initBeansWhitDependenceRecursion(Map<Class<?>, Byte> classStatusMap, Class<?> curClazz) throws DuplicateBeanName, InstantiationException, IllegalAccessException, CircleBeanDependence {
        if (CLAZZ_BEAN_MAP.containsKey(curClazz)){
            // 当前类已经加载
            return;
        }
        if (!classStatusMap.containsKey(curClazz)){
            // 当前类还未处理，设置为正在处理
            classStatusMap.put(curClazz, (byte)0);
        }else if (classStatusMap.get(curClazz) == (byte)0){
            // 当前正在处理该节点，存在循环引用
            throw new CircleBeanDependence(curClazz);
        }
        Map<Field , Class<?>> dependenceInfo = BEAN_DEPENDENT_INFO.get(curClazz);
        for (Class<?> clazz:dependenceInfo.values()){
            if (!classStatusMap.containsKey(clazz)){
                initBeansWhitDependenceRecursion(classStatusMap, clazz);
            }else if (classStatusMap.get(clazz) == (byte)0){
                // 存在循环引用
                throw new CircleBeanDependence(curClazz, clazz);
            }
            // 已经加载完毕
        }
        MyComponent myComponent = getAnnotation(curClazz, MyComponent.class);
        String name =  myComponent == null?"":myComponent.name();
        createBean(curClazz, name , dependenceInfo);
        classStatusMap.put(curClazz, (byte)1);
    }

    /**
     * 向IOC容器中创建一个bean
     * @param clazz bean的类型
     * @param name bean的名称
     * @param dependenceInfo 当前bean的依赖信息
     */
    private void createBean(Class<?> clazz, String name, Map<Field, Class<?>> dependenceInfo) throws InstantiationException, IllegalAccessException, DuplicateBeanName {
        if (checkDependence(new HashSet<>(dependenceInfo.values()))){
            Object bean = clazz.newInstance();
            for (Map.Entry<Field, Class<?>> entry:dependenceInfo.entrySet()){
                Field field = entry.getKey();
                Class<?> deClass = entry.getValue();
                field.setAccessible(true);
                field.set(bean, getBean(deClass));
            }
            addBean(clazz, bean);
            if (!name.isEmpty()){
                // bean名称重复，抛出异常
                if (NAME_BEAN_MAP.containsKey(name)){
                    throw new DuplicateBeanName(name, clazz, NAME_BEAN_MAP.get(name).getClass());
                }
                addBean(name, bean);
            }
        }
    }

    /**
     * 检查IOC容器中是否有这些类型的bean
     * @param classSet 类的集合
     * @return 如果都包含，则返回true；否则返回false
     */
    private boolean checkDependence(Set<Class<?>> classSet){
        if(classSet.isEmpty()){
            return true;
        }
        for (Class<?> clazz:classSet){
            if (!CLAZZ_BEAN_MAP.containsKey(clazz)){
                return false;
            }
        }
        return true;
    }

    /**
     * 获取当前类的bean依赖关系
     * @param clazz 需要获取的类
     * @return Map。key为当前类的字段，value为当前类的类型
     */
    private Map<Field, Class<?>> getClassDependenceInfo(Class<?> clazz){
        if(clazz == null){
            return new HashMap<>();
        }
        Field[] fields = clazz.getDeclaredFields();
        Map<Field, Class<?>> res = new HashMap<>();
        for (Field field:fields){
            // 当前字段的类型
            Class<?> fieldClazz = field.getType();
            // 当前字段所带有的注解
            MyAutowired annotation = getAnnotation(field, MyAutowired.class);
            if (annotation != null){
                // 当前类是需要注入的
                //todo: 注入的时候指定名称
                res.put(field, fieldClazz);
            }
        }
        return res;
    }



    /**
     * 获取某个包下的所有的class的名称
     * @param packageName 包名称
     * @return class名称的列表
     */
    private List<String> getClassNames(String packageName) {
        List<String> classNames = new ArrayList<>();
        try {
            String path = packageName.replace('.', '/');
            URL resource = ClassLoader.getSystemClassLoader().getResource(path);
            if (resource != null) {
                File directory = new File(resource.getFile());
                File[] files = directory.listFiles();
                if (files != null){
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                            classNames.add(className);
                        }
                    }
                }
            } else {
                throw new RuntimeException("No such package: " + packageName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classNames;
    }

    /**
     * 获取当前类的注解信息
     * @param clazz 类
     * @param annotationType 注解类型
     * @return 第一个匹配的注解
     */
    private <A extends Annotation> A getAnnotation(Class<?> clazz, Class<A> annotationType){
        A[] annotations = clazz.getAnnotationsByType(annotationType);
        if (annotations.length > 0){
            return annotations[0];
        }
        return null;
    }

    private <A extends Annotation> A[] getAnnotations(Class<?> clazz, Class<A> annotationType){
        return clazz.getAnnotationsByType(annotationType);
    }

    /**
     * 获取当前字段的注解信息
     * @param field 字段
     * @param annotationType 注解类型
     * @return 第一个匹配的注解
     */
    private <A extends Annotation> A getAnnotation(Field field, Class<A> annotationType){
        A[] annotations = field.getAnnotationsByType(annotationType);
        if (annotations.length > 0){
            return annotations[0];
        }
        return null;
    }

    private <A extends Annotation> A[] getAnnotations(Field field, Class<A> annotationType){
        return field.getAnnotationsByType(annotationType);
    }



    public <S> S getBean(Class<S> clazz){
        return (S) CLAZZ_BEAN_MAP.get(clazz);
    }

    private boolean addBean(Class<?> clazz, Object bean){
        if (CLAZZ_BEAN_MAP.containsKey(clazz)){
            CLAZZ_BEAN_MAP.put(clazz, bean);
            return false;
        }
        CLAZZ_BEAN_MAP.put(clazz, bean);
        return true;
    }

    public Object getBean(String name){
        return NAME_BEAN_MAP.get(name);
    }
    private boolean addBean(String name, Object bean){
        if (NAME_BEAN_MAP.containsKey(name)){
            NAME_BEAN_MAP.put(name, bean);
            return false;
        }
        NAME_BEAN_MAP.put(name, bean);
        return true;
    }

}
