package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author: 余勇
 * @date: 2019-12-04 22:38
 *
 * ResultMap解析异常
 */
public class ResultMapException extends BaseException {

    public ResultMapException() {
    }

    public ResultMapException(String message) {
        super(message);
    }

    public ResultMapException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResultMapException(Throwable cause) {
        super(cause);
    }
}
