package cn.javadog.sd.mybatis.support.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.javadog.sd.mybatis.support.exceptions.TypeException;
import cn.javadog.sd.mybatis.support.io.Resources;
import cn.javadog.sd.mybatis.support.util.ResolverUtil;

/**
 * @author Clinton Begin
 *
 *
 *
 */
/**
 * @author 余勇
 * @date 2019-12-06 17:07
 * 类型与别名的注册表
 * 通过别名，我们在 Mapper XML 中的 resultType 和 parameterType 属性，直接使用，而不用写全类名
 */
public class TypeAliasRegistry {

  /**
   * 类型与别名的映射。
   */
  private final Map<String, Class<?>> TYPE_ALIASES = new HashMap<>();

  /**
   * 初始化时就注册别名，注意不是类加载时
   *
   * 另外，在 {@link org.apache.ibatis.session.Configuration} 构造方法中，也有默认的注册
   * TODO 下划线的什么时候用到
   */
  public TypeAliasRegistry() {
    registerAlias("string", String.class);

    registerAlias("byte", Byte.class);
    registerAlias("long", Long.class);
    registerAlias("short", Short.class);
    registerAlias("int", Integer.class);
    registerAlias("integer", Integer.class);
    registerAlias("double", Double.class);
    registerAlias("float", Float.class);
    registerAlias("boolean", Boolean.class);

    registerAlias("byte[]", Byte[].class);
    registerAlias("long[]", Long[].class);
    registerAlias("short[]", Short[].class);
    registerAlias("int[]", Integer[].class);
    registerAlias("integer[]", Integer[].class);
    registerAlias("double[]", Double[].class);
    registerAlias("float[]", Float[].class);
    registerAlias("boolean[]", Boolean[].class);

    registerAlias("_byte", byte.class);
    registerAlias("_long", long.class);
    registerAlias("_short", short.class);
    registerAlias("_int", int.class);
    registerAlias("_integer", int.class);
    registerAlias("_double", double.class);
    registerAlias("_float", float.class);
    registerAlias("_boolean", boolean.class);

    registerAlias("_byte[]", byte[].class);
    registerAlias("_long[]", long[].class);
    registerAlias("_short[]", short[].class);
    registerAlias("_int[]", int[].class);
    registerAlias("_integer[]", int[].class);
    registerAlias("_double[]", double[].class);
    registerAlias("_float[]", float[].class);
    registerAlias("_boolean[]", boolean[].class);

    registerAlias("date", Date.class);
    registerAlias("decimal", BigDecimal.class);
    registerAlias("bigdecimal", BigDecimal.class);
    registerAlias("biginteger", BigInteger.class);
    registerAlias("object", Object.class);

    registerAlias("date[]", Date[].class);
    registerAlias("decimal[]", BigDecimal[].class);
    registerAlias("bigdecimal[]", BigDecimal[].class);
    registerAlias("biginteger[]", BigInteger[].class);
    registerAlias("object[]", Object[].class);

    registerAlias("map", Map.class);
    registerAlias("hashmap", HashMap.class);
    registerAlias("list", List.class);
    registerAlias("arraylist", ArrayList.class);
    registerAlias("collection", Collection.class);
    registerAlias("iterator", Iterator.class);

    registerAlias("ResultSet", ResultSet.class);
  }

  /**
   * 获得别名对应的类型
   */
  public <T> Class<T> resolveAlias(String string) {
    try {
      if (string == null) {
        return null;
      }
      // 转换成小写
      String key = string.toLowerCase(Locale.ENGLISH);
      Class<T> value;
      // 首先，从 TYPE_ALIASES 中获取
      if (TYPE_ALIASES.containsKey(key)) {
        value = (Class<T>) TYPE_ALIASES.get(key);
      // 其次，直接获得对应类;所以，这个方法，同时处理了别名与全类名两种情况。
      } else {
        // 这种就是类名全路径
        value = (Class<T>) Resources.classForName(string);
      }
      return value;
    } catch (ClassNotFoundException e) {
      // 最差，找不到对应的类，异常
      throw new TypeException("Could not resolve type alias '" + string + "'.  Cause: " + e, e);
    }
  }

  /**
   * 注册指定包下的别名与类的映射
   *
   * @param packageName 指定包
   */
  public void registerAliases(String packageName){
    registerAliases(packageName, Object.class);
  }

  /**
   * 注册指定包下的别名与类的映射。另外，要求类必须是 {@param superType} 类型（包括子类）。
   *
   * @param packageName 指定包
   * @param superType 指定父类
   */
  public void registerAliases(String packageName, Class<?> superType){
    // 获得指定包下的类门
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
    // 遍历，逐个注册类型与别名的注册表
    for(Class<?> type : typeSet){
      // 排除匿名类 排除接口 排除内部类
      if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
        registerAlias(type);
      }
    }
  }

  /**
   * 注册指定类
   */
  public void registerAlias(Class<?> type) {
    // 默认为，简单类名
    String alias = type.getSimpleName();
    // 如果有注解，使用注册上的名字
    Alias aliasAnnotation = type.getAnnotation(Alias.class);
    if (aliasAnnotation != null) {
      alias = aliasAnnotation.value();
    }
    // 注册类型与别名的注册表
    registerAlias(alias, type);
  }

  /**
   * 注册类型与别名的注册表
   */
  public void registerAlias(String alias, Class<?> value) {
    if (alias == null) {
      throw new TypeException("The parameter alias cannot be null");
    }
    // 转换成小写 这样的话，无论我们在 Mapper XML 中，写 `String` 还是 `string` 甚至是 `STRING` ，都是对应的 String 类型。
    String key = alias.toLowerCase(Locale.ENGLISH);
    if (TYPE_ALIASES.containsKey(key) && TYPE_ALIASES.get(key) != null && !TYPE_ALIASES.get(key).equals(value)) {
      // 冲突，抛出 TypeException 异常;如果已经注册，并且类型不一致，说明有冲突，抛出 TypeException 异常。
      throw new TypeException("The alias '" + alias + "' is already mapped to the value '" + TYPE_ALIASES.get(key).getName() + "'.");
    }
    // 添加到 `TYPE_ALIASES` 中。另外，`#registerAlias(String alias, String value)` 方法，也会调用该方法
    TYPE_ALIASES.put(key, value);
  }

  public void registerAlias(String alias, String value) {
    try {
      // 通过类名的字符串，获得对应的类。
      registerAlias(alias, Resources.classForName(value));
    } catch (ClassNotFoundException e) {
      throw new TypeException("Error registering type alias "+alias+" for "+value+". Cause: " + e, e);
    }
  }
  
  /**
   * @since 3.2.2
   */
  public Map<String, Class<?>> getTypeAliases() {
    return Collections.unmodifiableMap(TYPE_ALIASES);
  }

}
