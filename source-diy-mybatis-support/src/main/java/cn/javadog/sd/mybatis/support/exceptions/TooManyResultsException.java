package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author 余勇
 * @date 2019-12-02 22:44
 *
 * 查询返回过多结果的异常。期望返回一条，实际返回了多条
 */
public class TooManyResultsException extends BaseException {

  public TooManyResultsException(String message) {
    super(message);
  }

}
