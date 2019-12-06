package cn.javadog.sd.mybatis.support.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.javadog.sd.mybatis.support.io.VFS;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * @author: 余勇
 * @date: 2019-12-06 20:46
 * 解析器工具类，用于获得指定目录符合条件的类们
 * TODO 这是一种设计模式，叫啥呢，我也不知道，哈哈
 */
public class ResolverUtil<T> {

  private static final Log log = LogFactory.getLog(ResolverUtil.class);

  /**
   * 匹配判断接口。用于测试类，确定这个类是否包含在ResolverUtil计算出来的结果中
   */
  public interface Test {
    /**
     * 判断是否匹配。这个方法在测试候选类时，会被重复调用。如果候选类在结果中，就返回true，不然，就返回false
     */
    boolean matches(Class<?> type);
  }

  /**
   * 实现 Test 接口，判断是否为指定类(的子类)
   */
  public static class IsA implements Test {

    /**
     * 指定类
     */
    private Class<?> parent;

    /**
     * 构造
     */
    public IsA(Class<?> parentType) {
      this.parent = parentType;
    }

    /**
     * 判断type能不能被parent所代表
     */
    @Override
    public boolean matches(Class<?> type) {
      return type != null && parent.isAssignableFrom(type);
    }

    /**
     * 重写toString
     */
    @Override
    public String toString() {
      return "is assignable to " + parent.getSimpleName();
    }
  }

  /**
   * 判断是否有指定注解
   */
  public static class AnnotatedWith implements Test {
    /**
     * 注解
     */
    private Class<? extends Annotation> annotation;

    /**
     * 构造
     */
    public AnnotatedWith(Class<? extends Annotation> annotation) {
      this.annotation = annotation;
    }

    /**
     * 判断是否标记有指定注解
     */
    @Override
    public boolean matches(Class<?> type) {
      return type != null && type.isAnnotationPresent(annotation);
    }

    /**
     * 重写toString
     */
    @Override
    public String toString() {
      return "annotated with @" + annotation.getSimpleName();
    }
  }

  /**
   * 符合条件的类的集合
   */
  private Set<Class<? extends T>> matches = new HashSet<>();

  /**
   * 类加载器。用于查找类，如果为null，也就是没有设置的话，就使用Thread.currentThread().getContextClassLoader()赋值
   */
  private ClassLoader classloader;

  /**
   * 返回目前符合条件的类的集合，数据来源于下面的{@link #find(Test, String)}
   */
  public Set<Class<? extends T>> getClasses() {
    return matches;
  }

  /**
   * 返回类加载器，如果没有被显式的设置过，就使用contextClassLoader
   */
  public ClassLoader getClassLoader() {
    return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
  }

  /**
   * 设置类加载器
   */
  public void setClassLoader(ClassLoader classloader) {
    this.classloader = classloader;
  }

  /**
   * 判断指定目录下面，实现了指定接口的类，或者继承类指定类的子类
   */
  public ResolverUtil<T> findImplementations(Class<?> parent, String... packageNames) {
    if (packageNames == null) {
      return this;
    }

    Test test = new IsA(parent);
    for (String pkg : packageNames) {
      find(test, pkg);
    }

    return this;
  }

  /**
   * 判断指定目录下面，标记有指定注解的类们
   */
  public ResolverUtil<T> findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
    if (packageNames == null) {
      return this;
    }

    Test test = new AnnotatedWith(annotation);
    for (String pkg : packageNames) {
      find(test, pkg);
    }

    return this;
  }

  /**
   *  获得指定包下，符合条件的类
   */
  public ResolverUtil<T> find(Test test, String packageName) {
    // 获得包的路径
    String path = getPackagePath(packageName);

    try {
      // 获得路径下的所有文件
      List<String> children = VFS.getInstance().list(path);
      // 遍历
      for (String child : children) {
        // 是 Java Class
        if (child.endsWith(".class")) {
          // 如果匹配，则添加到结果集
          addIfMatching(test, child);
        }
      }
    } catch (IOException ioe) {
      log.error("Could not read package: " + packageName, ioe);
    }

    return this;
  }

  /**
   * 获得包的路径
   */
  protected String getPackagePath(String packageName) {
    return packageName == null ? null : packageName.replace('.', '/');
  }

  /**
   * 如果匹配，则添加到结果集
   */
  @SuppressWarnings("unchecked")
  protected void addIfMatching(Test test, String fqn) {
    try {
      // 获得全类名
      String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
      ClassLoader loader = getClassLoader();
      if (log.isDebugEnabled()) {
        log.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
      }

      // 加载类
      Class<?> type = loader.loadClass(externalName);

      // 判断是否匹配
      if (test.matches(type)) {
        matches.add((Class<T>) type);
      }
    } catch (Throwable t) {
      log.warn("Could not examine class '" + fqn + "'" + " due to a " +
          t.getClass().getName() + " with message: " + t.getMessage());
    }
  }
}