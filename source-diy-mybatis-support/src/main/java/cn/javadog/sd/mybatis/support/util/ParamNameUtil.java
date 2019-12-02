package cn.javadog.sd.mybatis.support.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class ParamNameUtil {

  /**
   * 获得普通方法的参数列表
   *
   * @param method 普通方法
   * @return 参数集合
   */
  public static List<String> getParamNames(Method method) {
    return getParameterNames(method);
  }

  /**
   * 获得构造方法的参数列表
   *
   * @param constructor 构造方法
   * @return 参数集合
   */
  public static List<String> getParamNames(Constructor<?> constructor) {
    return getParameterNames(constructor);
  }

  private static List<String> getParameterNames(Executable executable) {
    final List<String> names = new ArrayList<>();
    final Parameter[] params = executable.getParameters();
    for (Parameter param : params) {
      names.add(param.getName());
    }
    return names;
  }

  private ParamNameUtil() {
    super();
  }
}
