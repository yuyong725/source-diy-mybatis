package cn.javadog.sd.mybatis.support.io;

import java.io.InputStream;
import java.net.URL;

/**
 * @author 余勇
 * @date 2019-12-06 19:53
 * ClassLoader 包装器。将多个类加载器包装成一个类加载器使用
 */
public class ClassLoaderWrapper {

  /**
   * 默认 ClassLoader 对象
   * 目前不存在初始化该属性的构造方法。
   * 修改入口在 {@link Resources#setDefaultClassLoader(ClassLoader)}
   */
  ClassLoader defaultClassLoader;

  /**
   * 系统 ClassLoader 对象
   * 在构造方法中，已经初始化。
   */
  ClassLoader systemClassLoader;

  /**
   * 构造函数
   */
  ClassLoaderWrapper() {
    try {
      systemClassLoader = ClassLoader.getSystemClassLoader();
    } catch (SecurityException ignored) {
      // Google App Engine 会抛错AccessControlException，为什么呢，注释写的
    }
  }
  
  /**
   * 获得指定资源的 URL
   */
  public URL getResourceAsURL(String resource) {
    return getResourceAsURL(resource, getClassLoaders(null));
  }

  /**
   * 使用指定的类加载器，从classpath下拿到资源的URL
   */
  public URL getResourceAsURL(String resource, ClassLoader classLoader) {
    return getResourceAsURL(resource, getClassLoaders(classLoader));
  }

  /**
   * 获得指定资源的 InputStream
   */
  public InputStream getResourceAsStream(String resource) {
    return getResourceAsStream(resource, getClassLoaders(null));
  }

  /**
   * 使用指定的类加载器，从classpath下拿到资源的InputStream
   */
  public InputStream getResourceAsStream(String resource, ClassLoader classLoader) {
    return getResourceAsStream(resource, getClassLoaders(classLoader));
  }

  /**
   * 从classpath获取指定的类(没找到就抛出呵呵)
   */
  public Class<?> classForName(String name) throws ClassNotFoundException {
    return classForName(name, getClassLoaders(null));
  }

  /**
   * 使用指定的类加载器，从classpath获取指定的类
   */
  public Class<?> classForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
    return classForName(name, getClassLoaders(classLoader));
  }

  /**
   * 使用指定的类加载器'们', 获得指定资源的 InputStream
   */
  InputStream getResourceAsStream(String resource, ClassLoader[] classLoader) {
    // 遍历 ClassLoader 数组，直到某个类加载器找到资源
    for (ClassLoader cl : classLoader) {
      if (null != cl) {
        // 获得 InputStream ，不带 /
        InputStream returnValue = cl.getResourceAsStream(resource);
        // 获得 InputStream ，带 /
        if (null == returnValue) {
          returnValue = cl.getResourceAsStream("/" + resource);
        }

        // 成功获得到，返回
        if (null != returnValue) {
          return returnValue;
        }
      }
    }
    return null;
  }

  /**
   * 使用指定的类加载器'们', 获得指定资源的 URL
   */
  URL getResourceAsURL(String resource, ClassLoader[] classLoader) {

    URL url;
    // 遍历 ClassLoader 数组
    for (ClassLoader cl : classLoader) {

      if (null != cl) {
        // 获得 URL ，不带 /
        url = cl.getResource(resource);
        // 获得 URL ，带 /
        if (null == url) {
          url = cl.getResource("/" + resource);
        }
        // "总是到最后，才找到它！"... 因为只有傻逼才会在已经找到过后，还去找，嗯，找到就没再找了！ —————————翻译的原英文注释，挺逗的
        // 成功获得到，返回
        if (null != url) {
          return url;
        }

      }

    }
    // 始终没找到
    return null;
  }

  /**
   * 使用指定的类加载器'们', 获得指定类名对应的类
   */
  Class<?> classForName(String name, ClassLoader[] classLoader) throws ClassNotFoundException {
    // 遍历 ClassLoader 数组
    for (ClassLoader cl : classLoader) {
      if (null != cl) {
        try {
          // 获得类
          Class<?> c = Class.forName(name, true, cl);
          // 成功获得到，返回
          if (null != c) {
            return c;
          }
        // 获得不到，抛出 ClassNotFoundException 异常
        } catch (ClassNotFoundException e) {
          // 一个找不到很正常，个个找不到就👇
        }
      }

    }

    throw new ClassNotFoundException("Cannot find class: " + name);

  }

  /**
   * 获取所有类加载器
   * 类加载器的优先级；ClassLoader A -> System class loader -> Extension class loader -> Bootstrap class loader
   * TODO 强烈推荐看完：https://www.iteye.com/blog/tyrion-1958814
   */
  ClassLoader[] getClassLoaders(ClassLoader classLoader) {
    return new ClassLoader[]{
        classLoader,
        // 默认类加载器
        defaultClassLoader,
        // 当前线程的
        Thread.currentThread().getContextClassLoader(),
        getClass().getClassLoader(),
        systemClassLoader};
  }

}
