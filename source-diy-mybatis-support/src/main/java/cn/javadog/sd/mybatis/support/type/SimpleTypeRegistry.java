package cn.javadog.sd.mybatis.support.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author 余勇
 * @date 2019-12-06 16:59
 * 简单类型注册表。功能很简单，就暴露一个方法，谈不上注册表
 */
public class SimpleTypeRegistry {

  /**
   * 简单类型的集合
   */
  private static final Set<Class<?>> SIMPLE_TYPE_SET = new HashSet<>();

  /**
   * 类加载时就初始化常用类到 SIMPLE_TYPE_SET 中
   */
  static {
    SIMPLE_TYPE_SET.add(String.class);
    SIMPLE_TYPE_SET.add(Byte.class);
    SIMPLE_TYPE_SET.add(Short.class);
    SIMPLE_TYPE_SET.add(Character.class);
    SIMPLE_TYPE_SET.add(Integer.class);
    SIMPLE_TYPE_SET.add(Long.class);
    SIMPLE_TYPE_SET.add(Float.class);
    SIMPLE_TYPE_SET.add(Double.class);
    SIMPLE_TYPE_SET.add(Boolean.class);
    SIMPLE_TYPE_SET.add(Date.class);
    SIMPLE_TYPE_SET.add(Class.class);
    SIMPLE_TYPE_SET.add(BigInteger.class);
    SIMPLE_TYPE_SET.add(BigDecimal.class);
  }

  /**
   * 关掉构造
   */
  private SimpleTypeRegistry() {
  }

  /**
   * 查看某个类是不是基础类型
   */
  public static boolean isSimpleType(Class<?> clazz) {
    return SIMPLE_TYPE_SET.contains(clazz);
  }

}
