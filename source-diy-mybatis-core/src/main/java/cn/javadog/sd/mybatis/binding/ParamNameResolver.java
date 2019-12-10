package cn.javadog.sd.mybatis.binding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import cn.javadog.sd.mybatis.annotations.Param;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.type.ParamMap;
import cn.javadog.sd.mybatis.support.util.ParamNameUtil;

/**
 * @author: 余勇
 * @date: 2019-12-10 11:16
 *
 * 参数名解析器
 */
public class ParamNameResolver {

  /**
   * 通用的参数前缀，就是如果没有使用@Param方式设定别名，可以通过 #{param1}，#{param2}的方式在xml中读取参数，param后面的数字从1开始
   */
  private static final String GENERIC_NAME_PREFIX = "param";

  /**
   * 参数名映射
   *
   * KEY：参数顺序
   * VALUE：参数名，由{@link Param}指定，没有标记{@link Param}的话，就会使用👆的{@link #GENERIC_NAME_PREFIX}。注意当参数中有特殊的参数
   *  (比如 {@link RowBounds} 或者 {@link ResultHandler})，要区别对待。举几个例子如下：
   * note 角标的数字是从0开始，与param从1开始略有不同
   * aMethod(@Param("M") int a, @Param("N") int b) -> {{0, "M"}, {1, "N"}}
   * aMethod(int a, int b) -> {{0, "0"}, {1, "1"}}
   * aMethod(int a, RowBounds rb, int b) -> {{0, "0"}, {2, "1"}}
   */
  private final SortedMap<Integer, String> names;

  /**
   * 是否有 {@link Param} 注解的参数
   */
  private boolean hasParamAnnotation;

  /**
   * 构造函数
   */
  public ParamNameResolver(Configuration config, Method method) {
    // 获取方法的参数类型列表
    final Class<?>[] paramTypes = method.getParameterTypes();
    // 获取方法各个参数上的注解，这是个二维数组，因为参数有多个，每个参数的注解又可能有多个
    final Annotation[][] paramAnnotations = method.getParameterAnnotations();
    // SortedMap 是 TreeMap 的父接口，这个 map 最终会赋值给 👆的属性 names
    final SortedMap<Integer, String> map = new TreeMap<>();
    // 记录参数的count
    int paramCount = paramAnnotations.length;
    // 从 @Param 注解上获取别名
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      if (isSpecialParameter(paramTypes[paramIndex])) {
        // 跳过特殊类型的参数
        continue;
      }
      String name = null;
      // 首先，从 @Param 注解中获取参数
      for (Annotation annotation : paramAnnotations[paramIndex]) {
        if (annotation instanceof Param) {
          hasParamAnnotation = true;
          name = ((Param) annotation).value();
          break;
        }
      }
      // 没有 @Param 的
      if (name == null) {
        // 如果开启了使用参数名作为name，默认是开启的，就获取真实的参数名
        if (config.isUseActualParamName()) {
          name = getActualParamName(method, paramIndex);
        }
        // 最差，使用 map 的顺序，作为编号，从0开始
        if (name == null) {
          name = String.valueOf(map.size());
        }
      }
      // 添加到 map 中
      map.put(paramIndex, name);
    }
    // 构建不可变集合
    names = Collections.unmodifiableSortedMap(map);
  }

  /**
   * 获取方法指定角标的参数的名称
   */
  private String getActualParamName(Method method, int paramIndex) {
    return ParamNameUtil.getParamNames(method).get(paramIndex);
  }

  /**
   * 是否特殊类型的参数，就两种 {@link RowBounds}, {@link ResultHandler}
   */
  private static boolean isSpecialParameter(Class<?> clazz) {
    return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
  }

  /**
   * 获取SQL参数用到的名称
   */
  public String[] getNames() {
    return names.values().toArray(new String[0]);
  }

  /**
   *
   * 获得参数名与值的映射
   *
   * 如果只有一个参数，也不是特殊类型的，那就直接返回那个值就好。如果多个参数值，就使用特定的命名方式(@param注解)，
   * 除此之外，还会加上(param1, param2,...)
   */
  public Object getNamedParams(Object[] args) {
    // 获取参数的数量
    final int paramCount = names.size();
    // 无参数，则返回 null
    if (args == null || paramCount == 0) {
      return null;
    } else if (!hasParamAnnotation && paramCount == 1) {
      // 只有一个参数，并且没有标记@param注解，直接返回首元素
      return args[names.firstKey()];
    } else {
      // 集合。
      // 组合 1 ：KEY：参数名，VALUE：参数值
      // 组合 2 ：KEY：GENERIC_NAME_PREFIX + 参数顺序，VALUE ：参数值
      final Map<String, Object> param = new ParamMap<>();
      int i = 0;
      // 遍历 names 集合，entry的key是方法参数角标，从0开始，跳过特殊参数(RowBounds, ResultHandler)，value是参数上@param的值，或者参数名，或者角标，从0开始
      for (Map.Entry<Integer, String> entry : names.entrySet()) {
        // 组合 1 ：添加到 param 中，可能是参数上@param的值，或者参数名，或者角标，从0开始，因为UseActualParamName默认是开启的，所以要么是@param的值，要么是参数名
        param.put(entry.getValue(), args[entry.getKey()]);
        // 组合 2 ：添加到 param 中 (param1, param2, ...)，这里将角标+1，所有param后面的数字从1开始
        final String genericParamName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);
        // 确保没有覆盖添加了@Param注解的属性，因为很可能@param注解上的值就是'param1'，'param2'这种
        if (!names.containsValue(genericParamName)) {
          param.put(genericParamName, args[entry.getKey()]);
        }
        i++;
      }
      return param;
    }
  }
}
