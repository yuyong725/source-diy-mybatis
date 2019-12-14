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
 * @author 余勇
 * @date 2019-12-06 20:16
 * 继承 VFS 抽象类，默认的 VFS 实现类
 */
public class DefaultVFS extends VFS {
  private static final Log log = LogFactory.getLog(DefaultVFS.class);

  /**
   * JAR (ZIP)问价文件的头文件数字
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
   * TODO URL与path的关系
   */
  @Override
  public List<String> list(URL url, String path) throws IOException {
    InputStream is = null;
    try {
      List<String> resources = new ArrayList<>();

      // 如果 url 指向的是 Jar Resource ，则返回该 Jar Resource ，否则返回 null。这里判断的标准URL指向的文件是否以.jar结尾
      URL jarUrl = findJarForResource(url);
      if (jarUrl != null) {
        is = jarUrl.openStream();
        if (log.isDebugEnabled()) {
          log.debug("Listing " + url);
        }
        // 遍历 Jar Resource
        resources = listResources(new JarInputStream(is), path);
      } else {
        // 不是以.jar结尾
        List<String> children = new ArrayList<>();
        try {
          // 判断为 JAR URL
          if (isJar(url)) {
            // 某些版本的JBoss VFS会将URL转成一个JAR流，即使实际上不是jar。意思就是URL指向的文件不是以.jar结尾，但内容就是jar
            is = url.openStream();
            try (JarInputStream jarInput = new JarInputStream(is)) {
              if (log.isDebugEnabled()) {
                log.debug("Listing " + url);
              }
              for (JarEntry entry; (entry = jarInput.getNextJarEntry()) != null; ) {
                if (log.isDebugEnabled()) {
                  log.debug("Jar entry: " + entry.getName());
                }
                // 拿下所有class文件的名字
                children.add(entry.getName());
              }
            }
          } else {
            /**
             * 某些servlet容器，会把文件夹当成一个text文档，文档的每一行就是子文件。
             * 但是这样的话，通过简单的读取文件，是无法区分文件夹和文件，因为所有的文件夹都变成类文件。
             * 为了解决这个问题呢，我们在读每一行的时候 (👆提到，每一行就是一个资源)，使用类加载器去加载它，一旦某一行失败了，就说明，这不是一个
             * 目录，因为目录下每一行应该都是资源文件(类加载器即可以加载class文件，也可以用于查找资源文件)，是可以被加载的
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
              if (getResources(path + "/" + line).isEmpty()) {
                // 一旦某一行的资源找不到，说明这是一个资源文件，而不是资源文件夹
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
           * 文件的openStream()有时会调用失败，因为某些servlet容器，可能涉及到文件读取权限的问题，如果遇到这种清空，直接当作文件夹进行列举
           */
          // 如果使用的是file协议，就是file:~/Desktop/1.txt 这种
          if ("file".equals(url.getProtocol())) {
            File file = new File(url.getFile());
            if (log.isDebugEnabled()) {
                log.debug("Listing directory " + file.getAbsolutePath());
            }
            if (file.isDirectory()) {
              // 针对的清空是没有打开权限的文件夹
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

        // 到此，已经记录下类所有的jar包下的class文件，文件下所有文件，但貌似只取一层，并未递归嵌套

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
          // 如果resourcePath是一个文件夹，这里就会解释👆的递归问题
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
   * 列举jar包下，以指定的path开头的文件的名字(名字，不是全类名)
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
    // 给path头尾都加上'/'
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    if (!path.endsWith("/")) {
      path = path + "/";
    }

    // 遍历条目并收集以请求路径开头的条目
    List<String> resources = new ArrayList<>();
    for (JarEntry entry; (entry = jar.getNextJarEntry()) != null;) {
      // 不是文件夹的话
      if (!entry.isDirectory()) {
        // 前面加上 '/' TODO debug下这些名字到底是啥
        String name = entry.getName();
        if (!name.startsWith("/")) {
          name = "/" + name;
        }

        // 检查文件的名称
        if (name.startsWith(path)) {
          if (log.isDebugEnabled()) {
            log.debug("Found resource: " + name);
          }
          // 去掉前面的'/'再加到resources
          resources.add(name.substring(1));
        }
      }
    }
    return resources;
  }

  /**
   * 尝试去解构URL，找到这个URL对应的JAR文件。也就是说，如果这个URL关联的是一个JAR文件，就返回这个URL，否则，返回null
   * Attempts to deconstruct the given URL to find a JAR file containing the resource referenced
   * by the URL. That is, assuming the URL references a JAR entry, this method will return a URL
   * that references the JAR file containing the entry. If the JAR cannot be located, then this
   * method returns null.
   * 
   * @param url JAR文件对应的URL.
   * @return 找到就返回URL，没找到就是null
   */
  protected URL findJarForResource(URL url) throws MalformedURLException {
    if (log.isDebugEnabled()) {
      log.debug("Find JAR URL: " + url);
    }

    // 这段代码看起来比较神奇，虽然看起来没有 break 的条件，但是是通过 MalformedURLException 异常进行
    // 正如上面英文注释，如果 URL 的文件部分本身就是 URL ，那么该 URL 可能指向 JAR
    try {
      for (;;) {
        // TODO url.getFile() 这个API
        url = new URL(url.getFile());
        if (log.isDebugEnabled()) {
          log.debug("Inner URL: " + url);
        }
      }
    } catch (MalformedURLException e) {
      // 某些情况可能会报错，就会触发break
    }

    // 判断是否意 .jar 结尾，然后砍掉后面的部分
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
      // 如果不包含 .jar，则直接返回 null
      return null;
    }

    // 尝试去打开这个文件测试下是否是jar
    try {
      URL testUrl = new URL(jarUrl.toString());
      // 判断是否为 Jar 文件
      if (isJar(testUrl)) {
        return testUrl;
      }
      else {
        // 检查URL对应的文件在当前文件系统是否存在
        if (log.isDebugEnabled()) {
          log.debug("Not a JAR: " + jarUrl);
        }
        // 获得文件，替换 TODO 看的劳资一愣一愣的
        jarUrl.replace(0, jarUrl.length(), testUrl.getFile());
        File file = new File(jarUrl.toString());

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
   * 将包名转换成路径(可以调用{@link ClassLoader#getResources(String)}查到的)，其实将'.'转成'/'
   */
  protected String getPackagePath(String packageName) {
    return packageName == null ? null : packageName.replace('.', '/');
  }

  /**
   * 判断URL对应的文件是不是JAR
   */
  protected boolean isJar(URL url) {
    return isJar(url, new byte[JAR_MAGIC.length]);
  }

  /**
   * 判断是否为 JAR URL
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
