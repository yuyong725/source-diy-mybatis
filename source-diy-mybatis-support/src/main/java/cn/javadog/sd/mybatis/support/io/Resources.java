package cn.javadog.sd.mybatis.support.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * @author: 余勇
 * @date: 2019-12-06 20:17
 * Resource 工具类，通过classloader操作资源
 */
public class Resources {

  /**
   * 类加载时就初始化一个ClassLoaderWrapper
   */
  private static ClassLoaderWrapper classLoaderWrapper = new ClassLoaderWrapper();

  /**
   * 字符集
   * 当调用 {@link #getResourceAsReader(String)} 时会用到。null代表系统默认字符
   */
  private static Charset charset;

  /**
   * 构造函数
   */
  Resources() {
  }

  /**
   * 返回默认的类加载器
   */
  public static ClassLoader getDefaultClassLoader() {
    return classLoaderWrapper.defaultClassLoader;
  }

  /**
   * 设置默认的类加载器
   */
  public static void setDefaultClassLoader(ClassLoader defaultClassLoader) {
    // 修改 ClassLoaderWrapper.
    classLoaderWrapper.defaultClassLoader = defaultClassLoader;
  }

  /**
   * 获得指定资源的 URL
   */
  public static URL getResourceURL(String resource) throws IOException {
      return getResourceURL(null, resource);
  }

  /**
   * 使用指定类加载器获得classpath下指定路径的 URL
   */
  public static URL getResourceURL(ClassLoader loader, String resource) throws IOException {
    URL url = classLoaderWrapper.getResourceAsURL(resource, loader);
    if (url == null) {
      throw new IOException("Could not find resource " + resource);
    }
    return url;
  }

  /**
   * 获得classpath下指定路径的 InputStream
   */
  public static InputStream getResourceAsStream(String resource) throws IOException {
    return getResourceAsStream(null, resource);
  }

  /**
   * 使用指定类加载器获得classpath下指定路径的 InputStream
   */
  public static InputStream getResourceAsStream(ClassLoader loader, String resource) throws IOException {
    InputStream in = classLoaderWrapper.getResourceAsStream(resource, loader);
    if (in == null) {
      throw new IOException("Could not find resource " + resource);
    }
    return in;
  }

  /**
   * 获得classpath下指定路径的 Properties
   */
  public static Properties getResourceAsProperties(String resource) throws IOException {
    Properties props = new Properties();
    try (InputStream in = getResourceAsStream(resource)) {
      props.load(in);
    }
    return props;
  }

  /**
   * 使用指定类加载器获得classpath下指定路径的 Properties
   */
  public static Properties getResourceAsProperties(ClassLoader loader, String resource) throws IOException {
    Properties props = new Properties();
    try (InputStream in = getResourceAsStream(loader, resource)) {
      props.load(in);
    }
    return props;
  }

  /**
   * 获得classpath下指定路径的 Reader
   */
  public static Reader getResourceAsReader(String resource) throws IOException {
    Reader reader;
    if (charset == null) {
      reader = new InputStreamReader(getResourceAsStream(resource));
    } else {
      reader = new InputStreamReader(getResourceAsStream(resource), charset);
    }
    return reader;
  }

  /**
   * 使用指定类加载器获得classpath下指定路径的 Reader
   */
  public static Reader getResourceAsReader(ClassLoader loader, String resource) throws IOException {
    Reader reader;
    if (charset == null) {
      reader = new InputStreamReader(getResourceAsStream(loader, resource));
    } else {
      reader = new InputStreamReader(getResourceAsStream(loader, resource), charset);
    }
    return reader;
  }

  /**
   * 获得classpath下指定路径的 File
   */
  public static File getResourceAsFile(String resource) throws IOException {
    return new File(getResourceURL(resource).getFile());
  }

  /**
   * 使用指定类加载器获得classpath下指定路径的 File
   */
  public static File getResourceAsFile(ClassLoader loader, String resource) throws IOException {
    return new File(getResourceURL(loader, resource).getFile());
  }

  /**
   * 通过URL(而不是classpath)获得 InputStream
   */
  public static InputStream getUrlAsStream(String urlString) throws IOException {
    URL url = new URL(urlString);
    URLConnection conn = url.openConnection();
    return conn.getInputStream();
  }

  /**
   * 通过URL(而不是classpath)获得 Reader
   */
  public static Reader getUrlAsReader(String urlString) throws IOException {
    Reader reader;
    if (charset == null) {
      reader = new InputStreamReader(getUrlAsStream(urlString));
    } else {
      reader = new InputStreamReader(getUrlAsStream(urlString), charset);
    }
    return reader;
  }

  /**
   * 通过URL(而不是classpath)获得 Properties
   */
  public static Properties getUrlAsProperties(String urlString) throws IOException {
    Properties props = new Properties();
    try (InputStream in = getUrlAsStream(urlString)) {
      props.load(in);
    }
    return props;
  }

  /**
   * 获得指定类名对应的类
   */
  public static Class<?> classForName(String className) throws ClassNotFoundException {
    return classLoaderWrapper.classForName(className);
  }

  /**
   * 获取字符集
   */
  public static Charset getCharset() {
    return charset;
  }

  /**
   * 设置字符集
   */
  public static void setCharset(Charset charset) {
    Resources.charset = charset;
  }

}
