package cn.javadog.sd.mybatis.support.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * @author: 余勇
 * @date: 2019-12-06 20:16
 * 继承 VFS 抽象类，默认的 VFS 实现类
 */
public class DefaultVFS extends VFS {
  private static final Log log = LogFactory.getLog(DefaultVFS.class);

  /**
   * JAR (ZIP)问价的头文件数字
   */
  private static final byte[] JAR_MAGIC = { 'P', 'K', 3, 4 };

  /**
   * 是否合法，默认合法
   */
  @Override
  public boolean isValid() {
    return true;
  }

  /**
   * 列举URL下的文件，可能是文件下的文件，也有可能URL就是个jar
   */
  @Override
  public List<String> list(URL url, String path) throws IOException {
    InputStream is = null;
    try {
      List<String> resources = new ArrayList<>();

      // 如果 url 指向的是 Jar Resource ，则返回该 Jar Resource ，否则返回 null
      URL jarUrl = findJarForResource(url);
      if (jarUrl != null) {
        is = jarUrl.openStream();
        if (log.isDebugEnabled()) {
          log.debug("Listing " + url);
        }
        // 遍历 Jar Resource
        resources = listResources(new JarInputStream(is), path);
      }
      else {
        List<String> children = new ArrayList<>();
        try {
          // 判断为 JAR URL
          if (isJar(url)) {
            // 某些版本的JBoss VFS会将URL转成一个JAR流，即使实际上不是jar
            is = url.openStream();
            try (JarInputStream jarInput = new JarInputStream(is)) {
              if (log.isDebugEnabled()) {
                log.debug("Listing " + url);
              }
              for (JarEntry entry; (entry = jarInput.getNextJarEntry()) != null; ) {
                if (log.isDebugEnabled()) {
                  log.debug("Jar entry: " + entry.getName());
                }
                children.add(entry.getName());
              }
            }
          }
          else {
            /**
             * 某些servlet容器，会把文件夹当初一个text文档，文档的每一行就是子文件。
             * 但是这样的话，通过简单的读取文件，是无法区分文件夹和文件，因为所有的文件夹都变成类文件。
             * 为了解决这个问题呢，我们在读每一行的时候 (👆提到，每一行就是一个资源)，使用类加载器去加载它，一旦某一行失败了，就说明，这不是一个
             * 目录，因为目录下每一行应该都是class文件，是可以被加载的？
             * TODO 这个翻译很牵强
             */
            // 【重点】获得路径下的所有资源
            is = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            List<String> lines = new ArrayList<>();
            for (String line; (line = reader.readLine()) != null;) {
              if (log.isDebugEnabled()) {
                log.debug("Reader entry: " + line);
              }
              lines.add(line);
              //TODO 为什么为空就全部干掉呢？为空就说明这是jar包，莫名其妙啊
              if (getResources(path + "/" + line).isEmpty()) {
                lines.clear();
                break;
              }
            }

            if (!lines.isEmpty()) {
              if (log.isDebugEnabled()) {
                log.debug("Listing " + url);
              }
              children.addAll(lines);
            }
          }
        } catch (FileNotFoundException e) {
          /**
           * 文件的openStream()有时会调用失败，因为某些servlet容器，可能涉及到文件怨毒权限的问题，如果遇到这种清空，
           * 直接当作文件夹进行列举
           */
          // 如果使用的是file协议，就是file:~/Desktop/1.txt 这种
          if ("file".equals(url.getProtocol())) {
            File file = new File(url.getFile());
            if (log.isDebugEnabled()) {
                log.debug("Listing directory " + file.getAbsolutePath());
            }
            if (file.isDirectory()) {
              if (log.isDebugEnabled()) {
                  log.debug("Listing " + url);
              }
              children = Arrays.asList(file.list());
            }
          }
          else {
            // 直接抛错
            throw e;
          }
        }

        // 【重点】计算 prefix
        String prefix = url.toExternalForm();
        if (!prefix.endsWith("/")) {
          // 不是以 "/"结尾的，加上
          prefix = prefix + "/";
        }

        // 【重点】 遍历子路径
        for (String child : children) {
          // 添加到 resources 中
          String resourcePath = path + "/" + child;
          resources.add(resourcePath);
          URL childUrl = new URL(prefix + child);
          resources.addAll(list(childUrl, resourcePath));
        }
      }

      return resources;
    } finally {
      // 关闭文件流
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
          // Ignore
        }
      }
    }
  }

  /**
   * 遍历 Jar Resource
   *
   * List the names of the entries in the given {@link JarInputStream} that begin with the
   * specified {@code path}. Entries will match with or without a leading slash.
   * 
   * @param jar The JAR input stream
   * @param path The leading path to match
   * @return The names of all the matching entries
   * @throws IOException If I/O errors occur
   */
  protected List<String> listResources(JarInputStream jar, String path) throws IOException {
    // Include the leading and trailing slash when matching names
    // 保证头尾都是 /
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    if (!path.endsWith("/")) {
      path = path + "/";
    }

    // Iterate over the entries and collect those that begin with the requested path
    // 遍历条目并收集以请求路径开头的条目
    List<String> resources = new ArrayList<>();
    for (JarEntry entry; (entry = jar.getNextJarEntry()) != null;) {
      if (!entry.isDirectory()) {
        // Add leading slash if it's missing
        String name = entry.getName();
        if (!name.startsWith("/")) {
          name = "/" + name;
        }

        // Check file name
        if (name.startsWith(path)) {
          if (log.isDebugEnabled()) {
            log.debug("Found resource: " + name);
          }
          // Trim leading slash
          resources.add(name.substring(1));
        }
      }
    }
    return resources;
  }

  /**
   * Attempts to deconstruct the given URL to find a JAR file containing the resource referenced
   * by the URL. That is, assuming the URL references a JAR entry, this method will return a URL
   * that references the JAR file containing the entry. If the JAR cannot be located, then this
   * method returns null.
   * 
   * @param url The URL of the JAR entry.
   * @return The URL of the JAR file, if one is found. Null if not.
   * @throws MalformedURLException
   */
  protected URL findJarForResource(URL url) throws MalformedURLException {
    if (log.isDebugEnabled()) {
      log.debug("Find JAR URL: " + url);
    }

    // If the file part of the URL is itself a URL, then that URL probably points to the JAR
    // 这段代码看起来比较神奇，虽然看起来没有 break 的条件，但是是通过 MalformedURLException 异常进行
    // 正如上面英文注释，如果 URL 的文件部分本身就是 URL ，那么该 URL 可能指向 JAR
    try {
      for (;;) {
        url = new URL(url.getFile());
        if (log.isDebugEnabled()) {
          log.debug("Inner URL: " + url);
        }
      }
    } catch (MalformedURLException e) {
      // This will happen at some point and serves as a break in the loop
    }

    // Look for the .jar extension and chop off everything after that
    // 判断是否意 .jar 结尾
    StringBuilder jarUrl = new StringBuilder(url.toExternalForm());
    int index = jarUrl.lastIndexOf(".jar");
    if (index >= 0) {
      jarUrl.setLength(index + 4);
      if (log.isDebugEnabled()) {
        log.debug("Extracted JAR URL: " + jarUrl);
      }
    }
    else {
      if (log.isDebugEnabled()) {
        log.debug("Not a JAR: " + jarUrl);
      }
      // 如果不以 .jar 结尾，则直接返回 null
      return null;
    }

    // Try to open and test it
    try {
      URL testUrl = new URL(jarUrl.toString());
      // 判断是否为 Jar 文件
      if (isJar(testUrl)) {
        return testUrl;
      }
      else {
        // WebLogic fix: check if the URL's file exists in the filesystem.
        if (log.isDebugEnabled()) {
          log.debug("Not a JAR: " + jarUrl);
        }
        // 获得文件，替换
        jarUrl.replace(0, jarUrl.length(), testUrl.getFile());
        File file = new File(jarUrl.toString());

        // File name might be URL-encoded
        // 处理路径编码问题
        if (!file.exists()) {
          try {
            file = new File(URLEncoder.encode(jarUrl.toString(), "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding?  UTF-8?  That's unpossible.");
          }
        }

        // 判断文件存在
        if (file.exists()) {
          if (log.isDebugEnabled()) {
            log.debug("Trying real file: " + file.getAbsolutePath());
          }
          testUrl = file.toURI().toURL();
          // 判断是否为 Jar 文件
          if (isJar(testUrl)) {
            return testUrl;
          }
        }
      }
    } catch (MalformedURLException e) {
      log.warn("Invalid JAR URL: " + jarUrl);
    }

    if (log.isDebugEnabled()) {
      log.debug("Not a JAR: " + jarUrl);
    }
    return null;
  }

  /**
   * Converts a Java package name to a path that can be looked up with a call to
   * {@link ClassLoader#getResources(String)}.
   * 
   * @param packageName The Java package name to convert to a path
   */
  protected String getPackagePath(String packageName) {
    return packageName == null ? null : packageName.replace('.', '/');
  }

  /**
   * Returns true if the resource located at the given URL is a JAR file.
   * 
   * @param url The URL of the resource to test.
   */
  protected boolean isJar(URL url) {
    return isJar(url, new byte[JAR_MAGIC.length]);
  }

  /**
   * 判断是否为 JAR URL
   *
   * Returns true if the resource located at the given URL is a JAR file.
   * 
   * @param url The URL of the resource to test.
   * @param buffer A buffer into which the first few bytes of the resource are read. The buffer
   *            must be at least the size of {@link #JAR_MAGIC}. (The same buffer may be reused
   *            for multiple calls as an optimization.)
   */
  protected boolean isJar(URL url, byte[] buffer) {
    InputStream is = null;
    try {
      is = url.openStream();
      // 读取文件头
      is.read(buffer, 0, JAR_MAGIC.length);
      // 判断文件头的 magic number 是否符合 JAR
      if (Arrays.equals(buffer, JAR_MAGIC)) {
        if (log.isDebugEnabled()) {
          log.debug("Found JAR: " + url);
        }
        return true;
      }
    } catch (Exception e) {
      // Failure to read the stream means this is not a JAR
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
          // Ignore
        }
      }
    }

    return false;
  }
}
