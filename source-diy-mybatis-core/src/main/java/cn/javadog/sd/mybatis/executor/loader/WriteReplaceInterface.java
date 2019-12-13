package cn.javadog.sd.mybatis.executor.loader;

import java.io.ObjectStreamException;

/**
 * @author Eduardo Macarron
 */
public interface WriteReplaceInterface {

  Object writeReplace() throws ObjectStreamException;

}
