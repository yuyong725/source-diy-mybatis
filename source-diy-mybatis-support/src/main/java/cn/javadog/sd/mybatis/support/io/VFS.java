package cn.javadog.sd.mybatis.support.io;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * @author 余勇
 * @date 2019-12-06 21:58
 * 虚拟文件系统( Virtual File System )抽象类，用来查找指定路径下的的文件们
 */
public abstract class VFS {
  private static final Log log = LogFactory.getLog(VFS.class);

  /**
   * 内置的 VFS 实现类的数组，源码里还有JBoss6VFS，这里移掉
   */
  public static final Class<?>[] IMPLEMENTATIONS = { DefaultVFS.class };

  /**
   * 自定义的 VFS 实现类的数组
   */
  public static final List<Class<? extends VFS>> USER_IMPLEMENTATIONS = new ArrayList<>();

  /**
   * 使用jvm类加载机制保证单例
   * 参考：https://juejin.im/post/5c94654e6fb9a071090d63ac
   */
  private static class VFSHolder {

    static final VFS INSTANCE = createVFS();

    /**
     * 创建一个VFS实例
     */
    @SuppressWarnings("unchecked")
    static VFS createVFS() {
      /**
       * 优先使用自定义的，其次再用内置的
       */
      List<Class<? extends VFS>> impls = new ArrayList<>();
      impls.addAll(USER_IMPLEMENTATIONS);
      impls.addAll(Arrays.asList((Class<? extends VFS>[]) IMPLEMENTATIONS));

      // 创建 VFS 对象，直到找到符合条件的
      VFS vfs = null;
      for (int i = 0; vfs == null || !vfs.isValid(); i++) {
        Class<? extends VFS> impl = impls.get(i);
        try {
          vfs = impl.newInstance();
          if (vfs == null || !vfs.isValid()) {
            if (log.isDebugEnabled()) {
              log.debug("VFS implementation " + impl.getName() +
                  " is not valid in this environment.");
            }
          }

          // 出错了直接返回null，不再遍历后面的实现
        } catch (InstantiationException e) {
          log.error("Failed to instantiate " + impl, e);
          return null;
        } catch (IllegalAccessException e) {
          log.error("Failed to instantiate " + impl, e);
          return null;
        }
      }

      if (log.isDebugEnabled()) {
        log.debug("Using VFS adapter " + vfs.getClass().getName());
      }

      return vfs;
    }
  }

  /**
   * 获取单例的VFS，如果当前环境下没有相应的实现，就会返回null
   */
  public static VFS getInstance() {
    return VFSHolder.INSTANCE;
  }

  /**
   * 添加VFS自定义的实现，会早于默认的实现
   */
  public static void addImplClass(Class<? extends VFS> clazz) {
    if (clazz != null) {
      USER_IMPLEMENTATIONS.add(clazz);
    }
  }

  /**
   * 加载指定的类，找不到返回null
   */
  protected static Class<?> getClass(String className) {
    try {
      // 加载指定的类
      return Thread.currentThread().getContextClassLoader().loadClass(className);
      // TODO 如下，一开始是使用ReflectUtil的，为什么不用了，直接使用ContextClassLoader
//      return ReflectUtil.findClass(className);
    } catch (ClassNotFoundException e) {
      if (log.isDebugEnabled()) {
        log.debug("Class not found: " + className);
      }
      return null;
    }
  }

  /**
   * 获取指定类含有指定的参数的指定方法，找不到返回null
   */
  protected static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
    if (clazz == null) {
      return null;
    }
    try {
      return clazz.getMethod(methodName, parameterTypes);
    } catch (SecurityException e) {
      log.error("Security exception looking for method " + clazz.getName() + "." + methodName + ".  Cause: " + e);
      return null;
    } catch (NoSuchMethodException e) {
      log.error("Method not found " + clazz.getName() + "." + methodName + "." + methodName + ".  Cause: " + e);
      return null;
    }
  }

  /**
   * 使用反射，执行方法
   */
  @SuppressWarnings("unchecked")
  protected static <T> T invoke(Method method, Object object, Object... parameters)
      throws IOException, RuntimeException {
    try {
      return (T) method.invoke(object, parameters);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      if (e.getTargetException() instanceof IOException) {
        throw (IOException) e.getTargetException();
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * 获得指定路径下的 URL 数组，这个路径就不是classpath了，URL可能是文件夹，也可能是文件(jar,说明文档等)
   */
  protected static List<URL> getResources(String path) throws IOException {
    return Collections.list(Thread.currentThread().getContextClassLoader().getResources(path));
  }

  /**
   * 判断是否为合法的 VFS
   */
  public abstract boolean isValid();

  /**
   * 递归获得指定路径下的所有资源。
   * 俩参数看起来很懵逼，看下 {@link #list(URL, String)}
   *
   *
   * @param url 资源的URL，可能是文件夹，也可能是文件，文件夹的话就要递归遍历了
   * @param forPath 用于获取资源URL的路径，会调用 {@link #getResources(String)}
   */
  protected abstract List<String> list(URL url, String forPath) throws IOException;

  /**
   * 获取指定路径下的所有资源文件
   */
  public List<String> list(String path) throws IOException {
    List<String> names = new ArrayList<>();
    for (URL url : getResources(path)) {
      names.addAll(list(url, path));
    }
    return names;
  }
}
