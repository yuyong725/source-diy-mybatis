package cn.javadog.sd.mybatis.support.util;

/**
 * @author 余勇
 * @date 2019-12-04 23:01
 * 字节数组转换工具，源码是在 type 包下，工具统一移到了这里
 */
public class ByteArrayUtils {

  /**
   * 关闭构造
   */
  private ByteArrayUtils() {
  }

  /**
   * Byte[] => byte[]
   */
  public static byte[] convertToPrimitiveArray(Byte[] objects) {
    final byte[] bytes = new byte[objects.length];
    for (int i = 0; i < objects.length; i++) {
      bytes[i] = objects[i];
    }
    return bytes;
  }

  /**
   * byte[] => Byte[]
   */
  public static Byte[] convertToObjectArray(byte[] bytes) {
    final Byte[] objects = new Byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      objects[i] = bytes[i];
    }
    return objects;
  }
}
