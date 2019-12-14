package cn.javadog.sd.mybatis.support.exceptions;


/**
 * @author 余勇
 * @date 2019-12-12 12:50
 * Script 模块异常
 */
public class ScriptingException extends BaseException {

  public ScriptingException() {
    super();
  }

  public ScriptingException(String message) {
    super(message);
  }

  public ScriptingException(String message, Throwable cause) {
    super(message, cause);
  }

  public ScriptingException(Throwable cause) {
    super(cause);
  }

}
