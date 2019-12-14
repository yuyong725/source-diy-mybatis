package cn.javadog.sd.mybatis.scripting;

import java.util.HashMap;
import java.util.Map;

import cn.javadog.sd.mybatis.support.exceptions.ScriptingException;

/**
 * @author 余勇
 * @date 2019-12-14 13:27
 *
 * LanguageDriver 注册表
 * 这个类不是 LanguageDriver 的子类
 */
public class LanguageDriverRegistry {

  /**
   * LanguageDriver 映射
   * key：LanguageDriver 类
   * value：LanguageDriver 实例
   */
  private final Map<Class<? extends LanguageDriver>, LanguageDriver> LANGUAGE_DRIVER_MAP = new HashMap();

  /**
   * 默认的 LanguageDriver 类
   */
  private Class<? extends LanguageDriver> defaultDriverClass;

  /**
   * 根据 类 注册 LanguageDriver
   */
  public void register(Class<? extends LanguageDriver> cls) {
    // 为空直接GG
    if (cls == null) {
      throw new IllegalArgumentException("null is not a valid Language Driver");
    }
    // 创建 cls 对应的对象，并添加到 LANGUAGE_DRIVER_MAP 中
    if (!LANGUAGE_DRIVER_MAP.containsKey(cls)) {
      try {
        LANGUAGE_DRIVER_MAP.put(cls, cls.newInstance());
      } catch (Exception ex) {
        throw new ScriptingException("Failed to load language driver for " + cls.getName(), ex);
      }
    }
  }

  /**
   * 根据 对象 注册 LanguageDriver。
   */
  public void register(LanguageDriver instance) {
    // 为空直接GG
    if (instance == null) {
      throw new IllegalArgumentException("null is not a valid Language Driver");
    }
    // 添加到 LANGUAGE_DRIVER_MAP 中
    Class<? extends LanguageDriver> cls = instance.getClass();
    if (!LANGUAGE_DRIVER_MAP.containsKey(cls)) {
      LANGUAGE_DRIVER_MAP.put(cls, instance);
    }
  }

  /**
   * 获取指定驱动的实例
   */
  public LanguageDriver getDriver(Class<? extends LanguageDriver> cls) {
    return LANGUAGE_DRIVER_MAP.get(cls);
  }

  /**
   * 获取默认驱动对象
   */
  public LanguageDriver getDefaultDriver() {
    return getDriver(getDefaultDriverClass());
  }

  /**
   * 获取默认驱动类
   */
  public Class<? extends LanguageDriver> getDefaultDriverClass() {
    return defaultDriverClass;
  }

  /**
   * 设置 默认的 LanguageDriver 类
   */
  public void setDefaultDriverClass(Class<? extends LanguageDriver> defaultDriverClass) {
    // 注册到 LANGUAGE_DRIVER_MAP 中
    register(defaultDriverClass);
    // 设置 defaultDriverClass 属性
    this.defaultDriverClass = defaultDriverClass;
  }

}
