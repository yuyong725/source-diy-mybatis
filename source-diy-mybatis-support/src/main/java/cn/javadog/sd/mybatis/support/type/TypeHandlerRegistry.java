package cn.javadog.sd.mybatis.support.type;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.JapaneseDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.javadog.sd.mybatis.support.exceptions.TypeException;
import cn.javadog.sd.mybatis.support.type.handler.ArrayTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.BigDecimalTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.BigIntegerTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.BlobByteObjectArrayTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.BlobInputStreamTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.BlobTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.BooleanTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ByteArrayTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ByteObjectArrayTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ByteTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.CharacterTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ClobReaderTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ClobTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.DateOnlyTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.DateTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.DoubleTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.EnumTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.FloatTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.InstantTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.IntegerTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.JapaneseDateTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.LocalDateTimeTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.LocalDateTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.LocalTimeTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.LongTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.MonthTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.NClobTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.NStringTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.OffsetDateTimeTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.OffsetTimeTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ShortTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.SqlDateTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.SqlTimeTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.SqlTimestampTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.SqlxmlTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.StringTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.TimeOnlyTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.UnknownTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.YearMonthTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.YearTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ZonedDateTimeTypeHandler;

/**
 * @author: 余勇
 * @date: 2019-12-06 17:14
 * TypeHandler 注册表，相当于管理 TypeHandler 的容器，从其中能获取到对应的 TypeHandler 。
 */
public final class TypeHandlerRegistry {

  /**
   * JDBC Type 和 {@link TypeHandler} 的映射
   *
   * {@link #register(JdbcType, TypeHandler)}
   */
  private final Map<JdbcType, TypeHandler<?>> JDBC_TYPE_HANDLER_MAP = new EnumMap<>(JdbcType.class);

  /**
   * {@link TypeHandler} 的映射
   *
   * 外面的KEY：Java Type
   * 里面的KEY：JDBC Type
   * VALUE：{@link TypeHandler} 对象
   */
  private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new ConcurrentHashMap<>();

  /**
   * {@link UnknownTypeHandler} 对象
   */
  private final TypeHandler<Object> UNKNOWN_TYPE_HANDLER = new UnknownTypeHandler(this);

  /**
   * 所有 TypeHandler 的“集合”
   *
   * KEY：{@link TypeHandler#getClass()}
   * VALUE：{@link TypeHandler} 对象
   */
  private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<>();

  /**
   * 空 TypeHandler 集合的标识，
   * 即使 {@link #TYPE_HANDLER_MAP} 中，某个 KEY1 对应的 Map<JdbcType, TypeHandler<?>> 为空。
   *
   * @see #getJdbcHandlerMap(Type)
   */
  private static final Map<JdbcType, TypeHandler<?>> NULL_TYPE_HANDLER_MAP = Collections.emptyMap();

  /**
   * 默认的枚举类型的 TypeHandler 对象
   */
  private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumTypeHandler.class;

  /**
   * 构造函数，初始化时去注册所有的类型处理器
   */
  public TypeHandlerRegistry() {
    register(Boolean.class, new BooleanTypeHandler());
    register(boolean.class, new BooleanTypeHandler());
    register(JdbcType.BOOLEAN, new BooleanTypeHandler());
    register(JdbcType.BIT, new BooleanTypeHandler());

    register(Byte.class, new ByteTypeHandler());
    register(byte.class, new ByteTypeHandler());
    register(JdbcType.TINYINT, new ByteTypeHandler());

    register(Short.class, new ShortTypeHandler());
    register(short.class, new ShortTypeHandler());
    register(JdbcType.SMALLINT, new ShortTypeHandler());

    register(Integer.class, new IntegerTypeHandler());
    register(int.class, new IntegerTypeHandler());
    register(JdbcType.INTEGER, new IntegerTypeHandler());

    register(Long.class, new LongTypeHandler());
    register(long.class, new LongTypeHandler());

    register(Float.class, new FloatTypeHandler());
    register(float.class, new FloatTypeHandler());
    register(JdbcType.FLOAT, new FloatTypeHandler());

    register(Double.class, new DoubleTypeHandler());
    register(double.class, new DoubleTypeHandler());
    register(JdbcType.DOUBLE, new DoubleTypeHandler());


    register(Reader.class, new ClobReaderTypeHandler());
    register(String.class, new StringTypeHandler());
    register(String.class, JdbcType.CHAR, new StringTypeHandler());
    register(String.class, JdbcType.CLOB, new ClobTypeHandler());
    register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
    register(String.class, JdbcType.LONGVARCHAR, new ClobTypeHandler());
    register(String.class, JdbcType.NVARCHAR, new NStringTypeHandler());
    register(String.class, JdbcType.NCHAR, new NStringTypeHandler());
    register(String.class, JdbcType.NCLOB, new NClobTypeHandler());
    register(JdbcType.CHAR, new StringTypeHandler());
    register(JdbcType.VARCHAR, new StringTypeHandler());
    register(JdbcType.CLOB, new ClobTypeHandler());
    register(JdbcType.LONGVARCHAR, new ClobTypeHandler());
    register(JdbcType.NVARCHAR, new NStringTypeHandler());
    register(JdbcType.NCHAR, new NStringTypeHandler());
    register(JdbcType.NCLOB, new NClobTypeHandler());

    register(Object.class, JdbcType.ARRAY, new ArrayTypeHandler());
    register(JdbcType.ARRAY, new ArrayTypeHandler());

    register(BigInteger.class, new BigIntegerTypeHandler());
    register(JdbcType.BIGINT, new LongTypeHandler());

    register(BigDecimal.class, new BigDecimalTypeHandler());
    register(JdbcType.REAL, new BigDecimalTypeHandler());
    register(JdbcType.DECIMAL, new BigDecimalTypeHandler());
    register(JdbcType.NUMERIC, new BigDecimalTypeHandler());

    register(InputStream.class, new BlobInputStreamTypeHandler());
    register(Byte[].class, new ByteObjectArrayTypeHandler());
    register(Byte[].class, JdbcType.BLOB, new BlobByteObjectArrayTypeHandler());
    register(Byte[].class, JdbcType.LONGVARBINARY, new BlobByteObjectArrayTypeHandler());
    register(byte[].class, new ByteArrayTypeHandler());
    register(byte[].class, JdbcType.BLOB, new BlobTypeHandler());
    register(byte[].class, JdbcType.LONGVARBINARY, new BlobTypeHandler());
    register(JdbcType.LONGVARBINARY, new BlobTypeHandler());
    register(JdbcType.BLOB, new BlobTypeHandler());

    register(Object.class, UNKNOWN_TYPE_HANDLER);
    register(Object.class, JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);
    register(JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);

    register(Date.class, new DateTypeHandler());
    register(Date.class, JdbcType.DATE, new DateOnlyTypeHandler());
    register(Date.class, JdbcType.TIME, new TimeOnlyTypeHandler());
    register(JdbcType.TIMESTAMP, new DateTypeHandler());
    register(JdbcType.DATE, new DateOnlyTypeHandler());
    register(JdbcType.TIME, new TimeOnlyTypeHandler());

    register(java.sql.Date.class, new SqlDateTypeHandler());
    register(java.sql.Time.class, new SqlTimeTypeHandler());
    register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());

    register(String.class, JdbcType.SQLXML, new SqlxmlTypeHandler());

    register(Instant.class, InstantTypeHandler.class);
    register(LocalDateTime.class, LocalDateTimeTypeHandler.class);
    register(LocalDate.class, LocalDateTypeHandler.class);
    register(LocalTime.class, LocalTimeTypeHandler.class);
    register(OffsetDateTime.class, OffsetDateTimeTypeHandler.class);
    register(OffsetTime.class, OffsetTimeTypeHandler.class);
    register(ZonedDateTime.class, ZonedDateTimeTypeHandler.class);
    register(Month.class, MonthTypeHandler.class);
    register(Year.class, YearTypeHandler.class);
    register(YearMonth.class, YearMonthTypeHandler.class);
    register(JapaneseDate.class, JapaneseDateTypeHandler.class);

    // issue #273
    register(Character.class, new CharacterTypeHandler());
    register(char.class, new CharacterTypeHandler());
  }

  /**
   * 设置默认的枚举类型处理器。不设置的话默认是EnumTypeHandler
   */
  public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
    this.defaultEnumTypeHandler = typeHandler;
  }

  /**
   * 指定Java类型，未指定JDBC类型 是否有对应的处理器
   */
  public boolean hasTypeHandler(Class<?> javaType) {
    return hasTypeHandler(javaType, null);
  }

  /**
   * 指定Java类型，指定JDBC类型 是否有对应的处理器，
   */
  public boolean hasTypeHandler(Class<?> javaType, JdbcType jdbcType) {
    return javaType != null && getTypeHandler((Type) javaType, jdbcType) != null;
  }

  /**
   * 指定Java类型（支持范型），未指定JDBC类型 是否有对应的处理器
   */
  public boolean hasTypeHandler(TypeReference<?> javaTypeReference) {
    return hasTypeHandler(javaTypeReference, null);
  }

  /**
   * 指定Java类型（支持范型），指定JDBC类型 是否有对应的处理器
   */
  public boolean hasTypeHandler(TypeReference<?> javaTypeReference, JdbcType jdbcType) {
    return javaTypeReference != null && getTypeHandler(javaTypeReference, jdbcType) != null;
  }

  /**
   * 获取指定的TypeHandler类对应的实例
   */
  public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
    return ALL_TYPE_HANDLERS_MAP.get(handlerType);
  }

  /**
   * 获取指定Java类型，未指定JDBC类型 对应的处理器
   */
  public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
    return getTypeHandler((Type) type, null);
  }

  /**
   * 获取指定Java类型（支持范型），未指定JDBC类型 对应的处理器
   */
  public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference) {
    return getTypeHandler(javaTypeReference, null);
  }

  /**
   * 获取指定JDBC类型 对应的处理器。也就它不用继续往下调用其他的，因为不需要范型支持，从JDBC_TYPE_HANDLER_MAP拿就好
   */
  public TypeHandler<?> getTypeHandler(JdbcType jdbcType) {
    return JDBC_TYPE_HANDLER_MAP.get(jdbcType);
  }

  /**
   * 指定JDBC类型 对应的处理器
   */
  public <T> TypeHandler<T> getTypeHandler(Class<T> type, JdbcType jdbcType) {
    return getTypeHandler((Type) type, jdbcType);
  }

  /**
   * 获取Java类型（支持范型），指定JDBC类型 对应的处理器
   */
  public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference, JdbcType jdbcType) {
    return getTypeHandler(javaTypeReference.getRawType(), jdbcType);
  }

  /**
   * 获得 TypeHandler
   * 这个是👆一堆方法的逻辑根源
   */
  @SuppressWarnings("unchecked")
  private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
    // 忽略 ParamMap 的情况，TODO 为毛呢
    if (ParamMap.class.equals(type)) {
      return null;
    }
    // 获得 Java Type 对应的 TypeHandler 集合
    Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = getJdbcHandlerMap(type);
    TypeHandler<?> handler = null;
    if (jdbcHandlerMap != null) {
      // 优先，使用 jdbcType 获取对应的
      handler = jdbcHandlerMap.get(jdbcType);
      // 其次，使用 null 获取对应的 TypeHandler ，可以认为是默认的 TypeHandler
      if (handler == null) {
        handler = jdbcHandlerMap.get(null);
      }
      // 最差，从 TypeHandler 集合中选择一个唯一的 TypeHandler
      if (handler == null) {
        handler = pickSoleHandler(jdbcHandlerMap);
      }
    }
    // 强转类型
    return (TypeHandler<T>) handler;
  }

  /**
   * 获得 Java Type 对应的 TypeHandler 集合
   */
  private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMap(Type type) {
    // 获得 Java Type 对应的 TypeHandler 集合
    Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(type);
    // 如果为 NULL_TYPE_HANDLER_MAP ，意味着为空，直接返回
    if (NULL_TYPE_HANDLER_MAP.equals(jdbcHandlerMap)) {
      return null;
    }
    // 如果找不到，且是Class类型，也就是没有范型
    if (jdbcHandlerMap == null && type instanceof Class) {
      Class<?> clazz = (Class<?>) type;
      // 枚举类型
      if (clazz.isEnum()) {
        // 获得父类对应的 TypeHandler 集合
        jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(clazz, clazz);
        // 如果找不到
        if (jdbcHandlerMap == null) {
          // 注册 defaultEnumTypeHandler ，并使用它
          register(clazz, getInstance(clazz, defaultEnumTypeHandler));
          // 返回结果
          return TYPE_HANDLER_MAP.get(clazz);
        }
      // 非枚举类型
      } else {
        // 获得父类对应的 TypeHandler 集合
        jdbcHandlerMap = getJdbcHandlerMapForSuperclass(clazz);
      }
    }
    // 如果结果为空，设置为 NULL_TYPE_HANDLER_MAP ，提升查找速度，避免二次查找
    TYPE_HANDLER_MAP.put(type, jdbcHandlerMap == null ? NULL_TYPE_HANDLER_MAP : jdbcHandlerMap);
    // 返回结果
    return jdbcHandlerMap;
  }

  /**
   * 对于枚举类，通过接口判定对应的类型处理器
   */
  private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForEnumInterfaces(Class<?> clazz, Class<?> enumClazz) {
    // 遍历枚举类的所有接口
    for (Class<?> iface : clazz.getInterfaces()) {
      // 获得该接口对应的 jdbcHandlerMap 集合
      Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(iface);
      // 为空，递归 getJdbcHandlerMapForEnumInterfaces 方法，继续从父类对应的 TypeHandler 集合
      if (jdbcHandlerMap == null) {
        jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(iface, enumClazz);
      }
      // 如果找到，则从 jdbcHandlerMap 初始化中 newMap 中，并进行返回
      if (jdbcHandlerMap != null) {
        HashMap<JdbcType, TypeHandler<?>> newMap = new HashMap<>();
        for (Entry<JdbcType, TypeHandler<?>> entry : jdbcHandlerMap.entrySet()) {
          // 将从接口找到的Map<JdbcType, TypeHandler<?>>处理下，放到newMap
          newMap.put(entry.getKey(), getInstance(enumClazz, entry.getValue().getClass()));
        }
        return newMap;
      }
    }
    // 找不到，则返回 null
    return null;
  }

  /**
   * 对于非枚举类，通过父类去查找对应的类型处理器
   */
  private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForSuperclass(Class<?> clazz) {
    // 获得父类
    Class<?> superclass =  clazz.getSuperclass();
    // 不存在非 Object 的父类，返回 null
    if (superclass == null || Object.class.equals(superclass)) {
      return null;
    }
    // 获得父类对应的 TypeHandler 集合
    Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(superclass);
    // 找到，则直接返回
    if (jdbcHandlerMap != null) {
      return jdbcHandlerMap;
    } else {
      // 找不到，则递归 getJdbcHandlerMapForSuperclass 方法，继续获得父类对应的 TypeHandler 集合
      return getJdbcHandlerMapForSuperclass(superclass);
    }
  }

  /**
   * 在找不对指定类对应的处理器，由没有默认的处理器(key为null)选择最合适的
   * 核心逻辑就是要求jdbcHandlerMap只有一种TypeHandler
   */
  private TypeHandler<?> pickSoleHandler(Map<JdbcType, TypeHandler<?>> jdbcHandlerMap) {
    TypeHandler<?> soleHandler = null;
    for (TypeHandler<?> handler : jdbcHandlerMap.values()) {
      // 选择一个
      if (soleHandler == null) {
        soleHandler = handler;
      } else if (!handler.getClass().equals(soleHandler.getClass())) {
        // 如果还有，并且不同类，那么不好选择，所以返回 null
        return null;
      }
    }
    return soleHandler;
  }

  /**
   * 获取UNKNOWN_TYPE_HANDLER
   */
  public TypeHandler<Object> getUnknownTypeHandler() {
    return UNKNOWN_TYPE_HANDLER;
  }

  /**
   * 注册指定jdbcType对应的处理器
   */
  public void register(JdbcType jdbcType, TypeHandler<?> handler) {
    JDBC_TYPE_HANDLER_MAP.put(jdbcType, handler);
  }

  /**
   * 注册处理器，没声明指定的jdbcType或javaType；这种一般是通过类上的注解
   */
  @SuppressWarnings("unchecked")
  public <T> void register(TypeHandler<T> typeHandler) {
    boolean mappedTypeFound = false;
    // 获得 @MappedTypes 注解
    MappedTypes mappedTypes = typeHandler.getClass().getAnnotation(MappedTypes.class);
    // 优先，使用 @MappedTypes 注解的 Java Type 进行注册
    if (mappedTypes != null) {
      for (Class<?> handledType : mappedTypes.value()) {
        register(handledType, typeHandler);
        mappedTypeFound = true;
      }
    }
    // 3.1.0以后的版本，会尝试去自动发现对应的类型
    // 当 typeHandler 为 TypeReference 子类时，进行注册
    if (!mappedTypeFound && typeHandler instanceof TypeReference) {
      try {
        TypeReference<T> typeReference = (TypeReference<T>) typeHandler;
        // Java Type 为 <T> 泛型
        register(typeReference.getRawType(), typeHandler);
        mappedTypeFound = true;
      } catch (Throwable t) {
        // 可能用户定义了类型不同的TypeReference(标记的和处理的类型不同)，忽略这个异常
      }
    }
    // 最差，使用 Java Type 为 null 进行注册
    if (!mappedTypeFound) {
      register((Class<T>) null, typeHandler);
    }
  }

  /**
   * 注册处理器，使用指定的javaType；这种一般是通过类上的注解
   */
  public <T> void register(Class<T> javaType, TypeHandler<? extends T> typeHandler) {
    register((Type) javaType, typeHandler);
  }

  /**
   * 注册指定 Java Type 的 TypeHandler 对象
   */
  private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
    // 获得 MappedJdbcTypes 注解
    MappedJdbcTypes mappedJdbcTypes = typeHandler.getClass().getAnnotation(MappedJdbcTypes.class);
    if (mappedJdbcTypes != null) {
      // 遍历 MappedJdbcTypes 注册的 JDBC Type 进行注册
      for (JdbcType handledJdbcType : mappedJdbcTypes.value()) {
        register(javaType, handledJdbcType, typeHandler);
      }
      if (mappedJdbcTypes.includeNullJdbcType()) {
        register(javaType, null, typeHandler);
      }
    } else {
      register(javaType, null, typeHandler);
    }
  }

  /**
   * 注册指定TypeReference 的 TypeHandler 对象，如List<T>
   */
  public <T> void register(TypeReference<T> javaTypeReference, TypeHandler<? extends T> handler) {
    // 本质还是对应的rawType，即List<T>的list
    register(javaTypeReference.getRawType(), handler);
  }

  /**
   * 注册指定JavaType和JdbcType对应的处理器
   */
  public <T> void register(Class<T> type, JdbcType jdbcType, TypeHandler<? extends T> handler) {
    register((Type) type, jdbcType, handler);
  }

  /**
   * 注册指定Type和JdbcType对应的处理器。大量方法最终的归宿
   */
  private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
    // 添加 handler 到 TYPE_HANDLER_MAP 中
    if (javaType != null) {
      // 获得 Java Type 对应的 map
      Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.get(javaType);
      // 如果不存在，则进行创建
      if (map == null || map == NULL_TYPE_HANDLER_MAP) {
        map = new HashMap<>();
        TYPE_HANDLER_MAP.put(javaType, map);
      }
      // 添加到 handler 中 map 中
      map.put(jdbcType, handler);
    }
    // 添加 handler 到 ALL_TYPE_HANDLERS_MAP 中
    ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
  }

  /**
   * 注册指定 TypeHandler 类，相对于👆的{@link #register(TypeHandler)}, 没有TypeReference的解析，拿不到范型
   */
  public void register(Class<?> typeHandlerClass) {
    boolean mappedTypeFound = false;
    // 获得 @MappedTypes 注解
    MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
    if (mappedTypes != null) {
      // 遍历注解的 Java Type 数组，逐个进行注册
      for (Class<?> javaTypeClass : mappedTypes.value()) {
        register(javaTypeClass, typeHandlerClass);
        mappedTypeFound = true;
      }
    }
    // 未使用 @MappedTypes 注解，则直接注册
    if (!mappedTypeFound) {
      register(getInstance(null, typeHandlerClass));
    }
  }

  // java type + handler type

  /**
   * 根据javaType的全类名和typeHandler的全类名注册处理器。
   * TODO 这种实现哪里会用？
   */
  public void register(String javaTypeClassName, String typeHandlerClassName) throws ClassNotFoundException {
    register(Resources.classForName(javaTypeClassName), Resources.classForName(typeHandlerClassName));
  }

  public void register(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
    register(javaTypeClass, getInstance(javaTypeClass, typeHandlerClass));
  }

  /**
   * 注册指定Type和JdbcType对应的处理器。参数中的处理器是class对象，需要先实例化
   */
  public void register(Class<?> javaTypeClass, JdbcType jdbcType, Class<?> typeHandlerClass) {
    register(javaTypeClass, jdbcType, getInstance(javaTypeClass, typeHandlerClass));
  }

  /**
   * 创建 TypeHandler 对象
   */
  @SuppressWarnings("unchecked")
  public <T> TypeHandler<T> getInstance(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
    // 获得 Class 类型的构造方法
    if (javaTypeClass != null) {
      try {
        Constructor<?> c = typeHandlerClass.getConstructor(Class.class);
        // 符合这个条件的，例如 EnumTypeHandler的构造需要传一个class类型的参数
        return (TypeHandler<T>) c.newInstance(javaTypeClass);
      } catch (NoSuchMethodException ignored) {
        // ignored 忽略该异常，继续向下
      } catch (Exception e) {
        throw new TypeException("Failed invoking constructor for handler " + typeHandlerClass, e);
      }
    }
    // 获得空参的构造方法
    try {
      Constructor<?> c = typeHandlerClass.getConstructor();
      // 符合这个条件的，例如 IntegerTypeHandler
      return (TypeHandler<T>) c.newInstance();
    } catch (Exception e) {
      throw new TypeException("Unable to find a usable constructor for " + typeHandlerClass, e);
    }
  }

  /**
   * 扫描指定包下的所有 TypeHandler 类，并发起注册
   */
  public void register(String packageName) {
    // 扫描指定包下的所有 TypeHandler 类
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
    resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);
    Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
    // 遍历 TypeHandler 数组，发起注册
    for (Class<?> type : handlerSet) {
      //Ignore inner classes and interfaces (including package-info.java) and abstract classes
      // 排除匿名类、接口、抽象类
      if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
        register(type);
      }
    }
  }
  

  /**
   * 获取所有的处理器
   */
  public Collection<TypeHandler<?>> getTypeHandlers() {
    return Collections.unmodifiableCollection(ALL_TYPE_HANDLERS_MAP.values());
  }
  
}
