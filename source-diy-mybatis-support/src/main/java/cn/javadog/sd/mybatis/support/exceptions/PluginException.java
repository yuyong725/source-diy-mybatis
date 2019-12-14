package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author 余勇
 * @date 2019-12-13 23:55
 *
 * 插件模块的异常
 */
public class PluginException extends BaseException {

  public PluginException() {
    super();
  }

  public PluginException(String message) {
    super(message);
  }

  public PluginException(String message, Throwable cause) {
    super(message, cause);
  }

  public PluginException(Throwable cause) {
    super(cause);
  }
}
