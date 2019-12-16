package cn.javadog.sd.mybatis.binding;

import cn.javadog.sd.mybatis.annotations.Flush;
import cn.javadog.sd.mybatis.annotations.MapKey;
import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.SqlCommandType;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.support.exceptions.BindingException;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.resolver.TypeParameterResolver;

/**
 * @author 余勇
 * @date 2019-12-09 22:38
 *
 * Mapper 方法。在 Mapper 接口中，每个定义的方法，对应一个 MapperMethod 对象
 */
public class MapperMethod {

  /**
   * SqlCommand 对象
   */
  private final SqlCommand command;

  /**
   * MethodSignature 对象
   */
  private final MethodSignature method;

  /**
   * 构造函数
   *
   * @param config 全局配置Configuration
   * @param mapperInterface 加了@mapper的接口
   * @param method mapper接口中一个方法
   */
  public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
    // 初始化SqlCommand，用于判定该方法对应的SQL命令的类型
    this.command = new SqlCommand(config, mapperInterface, method);
    // 初始化方法签名，用于解析方法的参数类型和返回值类型
    this.method = new MethodSignature(config, mapperInterface, method);
  }

  /**
   * 执行方法
   */
  public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    // 根据方法类型不同，使用不同的操作
    switch (command.getType()) {
      case INSERT: {
        // 转换参数，只有两种可能，单参数(没有@param注解)的值，或者paramMap，TODO 什么时候出现 strictMap
        Object param = method.convertArgsToSqlCommandParam(args);
        // 执行 INSERT 操作，并转换 rowCount
        result = rowCountResult(sqlSession.insert(command.getName(), param));
        break;
      }
      case UPDATE: {
        // 转换参数，只有两种可能，单参数(没有@param注解)的值，或者paramMap
        Object param = method.convertArgsToSqlCommandParam(args);
        // 执行更新，转换 rowCount
        result = rowCountResult(sqlSession.update(command.getName(), param));
        break;
      }
      case DELETE: {
        // 转换参数，只有两种可能，单参数(没有@param注解)的值，或者paramMap
        Object param = method.convertArgsToSqlCommandParam(args);
        // 执行删除，转换 rowCount
        result = rowCountResult(sqlSession.delete(command.getName(), param));
        break;
      }
      case SELECT:
        // 方法返回类型为无返回，并且有 ResultHandler 方法参数，则将查询的结果，提交给 ResultHandler 进行处理
        if (method.returnsVoid() && method.hasResultHandler()) {
          executeWithResultHandler(sqlSession, args);
          result = null;
        } else if (method.returnsMany()) {
          // 执行查询，返回列表
          result = executeForMany(sqlSession, args);
        } else if (method.returnsMap()) {
          // 执行查询，返回 Map
          result = executeForMap(sqlSession, args);
        } else if (method.returnsCursor()) {
          // 执行查询，返回 Cursor
          result = executeForCursor(sqlSession, args);
        } else {
          // 执行查询，返回单个对象
          // 转换参数
          Object param = method.convertArgsToSqlCommandParam(args);
          // 查询单条，实际上是selectList后取的1条
          result = sqlSession.selectOne(command.getName(), param);
          // 如果方法的返回类型是Optional，且(查询到的结果是空的，或者结果的类型与方法的返回类型不一致，其实就是要求result的类型不是optional)，那么就返回一个Optional
          if (method.returnsOptional() && (result == null || !method.getReturnType().equals(result.getClass()))) {
            //这个不一定是null，就是将结果包在Optional里面返回
            result = Optional.ofNullable(result);
          }
        }
        break;
      case FLUSH:
        // TODO FLUSH的作用暂且位置
        result = sqlSession.flushStatements();
        break;
      default:
        // 以上都不是，直接呵呵
        throw new BindingException("Unknown execution method for: " + command.getName());
    }
    // 返回结果为 null ，并且返回类型为基本类型，且不是void则抛出 BindingException 异常
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName()
              + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    // 返回结果
    return result;
  }

  /**
   * 将返回的行变更数，转换成方法实际要返回的类型；
   * 其实要么是int或long，就是影响的行数；要么是boolean，就是插入动作是否有影响的行数
   */
  private Object rowCountResult(int rowCount) {
    final Object result;
    if (method.returnsVoid()) {
      // 如果方法返回类型是null，结果就返回null
      result = null;
    } else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
      // 如果返回类型是int，就直接返回rowCount
      result = rowCount;
    } else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
      // 如果返回类型是long，就将 rowCount 转成long类型再返回
      result = (long)rowCount;
    } else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
      // 如果返回类型是boolean，就返回rowCount是否大于0，也就是是否有影响的行数
      result = rowCount > 0;
    } else {
      // 其他类型，直接呵呵
      throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
    }
    return result;
  }

  /**
   * 交给ResultHandler处理
   */
  private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
    // 获得 MappedStatement 对象
    MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
    // 返回结果类型是void，抛出 BindingException 异常。note 这里删除了存储过程的判断
    if (void.class.equals(ms.getResultMaps().get(0).getType())) {
      throw new BindingException("method " + command.getName()
              + " needs either a @ResultMap annotation, a @ResultType annotation,"
              + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
    }
    // 转换参数
    Object param = method.convertArgsToSqlCommandParam(args);
    // 执行 SELECT 操作
    if (method.hasRowBounds()) {
      // 参数中有分页条件的话，提取出参数中的RowBounds
      RowBounds rowBounds = method.extractRowBounds(args);
      // 执行查询操作，将结果交给参数中的ResultHandler处理
      sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
    } else {
      // 直接查询
      sqlSession.select(command.getName(), param, method.extractResultHandler(args));
    }
  }

  /**
   * 执行查询，返回列表
   */
  private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    // 转换参数
    Object param = method.convertArgsToSqlCommandParam(args);
    // 执行 SELECT 操作
    if (method.hasRowBounds()) {
      // 有分页条件的抽取分页
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.selectList(command.getName(), param);
    }
    // 封装 Array 或 Collection 结果。针对结果的类型与方法的返回类型不一致，因为result的class就是list，而我们可能要求返回数组或者set
    if (!method.getReturnType().isAssignableFrom(result.getClass())) {
      if (method.getReturnType().isArray()) {
        // 将list转成Array
        return convertToArray(result);
      } else {
        // 将list转成Collection
        return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
      }
    }
    // 直接返回的结果
    return result;
  }

  /**
   * 执行查询，返回 Cursor
   */
  private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
    Cursor<T> result;
    // 转换参数
    Object param = method.convertArgsToSqlCommandParam(args);
    // 执行 SELECT 操作
    if (method.hasRowBounds()) {
      // 抽离分页参数
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.<T>selectCursor(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.<T>selectCursor(command.getName(), param);
    }
    return result;
  }

  private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
    Object collection = config.getObjectFactory().create(method.getReturnType());
    MetaObject metaObject = config.newMetaObject(collection);
    metaObject.addAll(list);
    return collection;
  }

  /**
   * 将list转成Array
   */
  @SuppressWarnings("unchecked")
  private <E> Object convertToArray(List<E> list) {
    // 获取数组元素的类型
    Class<?> arrayComponentType = method.getReturnType().getComponentType();
    // 初始化一个指定类型的数组
    Object array = Array.newInstance(arrayComponentType, list.size());
    // 如果数组内容的类型的是基本类型
    if (arrayComponentType.isPrimitive()) {
      for (int i = 0; i < list.size(); i++) {
        // 遍历设置值
        Array.set(array, i, list.get(i));
      }
      return array;
    } else {
      // 直接使用list的toArray方法，note 区分基础类型的原因在于，toArray的参数数组的类型应该与list的相同，不能是基础类型
      return list.toArray((E[])array);
    }
  }

  /**
   * 执行查询，返回map
   */
  private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
    Map<K, V> result;
    // 转换参数
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      // 针对分页的情况
      RowBounds rowBounds = method.extractRowBounds(args);
      // 查询
      result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
    } else {
      result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
    }
    return result;
  }

  /**
   * MapperMethod 的内部静态类，处理SQL 命令的类型
   */
  public static class SqlCommand {

    /**
     * {@link MappedStatement#getId()}，对应MappedStatement的ID
     */
    private final String name;

    /**
     * SQL 命令类型，这是个枚举
     */
    private final SqlCommandType type;

    /**
     * 构造函数
     */
    public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
      // 获取方法的名称
      final String methodName = method.getName();
      // 获取方法声明的类，可能是mapperInterface，也可能是它的父类
      final Class<?> declaringClass = method.getDeclaringClass();

      // 获得 MappedStatement 对象
      MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass,
          configuration);

      // 找不到 MappedStatement
      if (ms == null) {
        // 如果有 @Flush 注解，则标记为 FLUSH 类型
        if(method.getAnnotation(Flush.class) != null){
          name = null;
          type = SqlCommandType.FLUSH;
        } else {
          // 抛出 BindingException 异常，如果找不到 MappedStatement
          throw new BindingException("Invalid bound statement (not found): "
              + mapperInterface.getName() + "." + methodName);
        }
      // 找到 MappedStatement
      } else {
        // 获得 name
        name = ms.getId();
        // 获得 type
        type = ms.getSqlCommandType();
        if (type == SqlCommandType.UNKNOWN) {
          // 抛出 BindingException 异常，如果是 UNKNOWN 类型
          throw new BindingException("Unknown execution method for: " + name);
        }
      }
    }

    public String getName() {
      return name;
    }

    public SqlCommandType getType() {
      return type;
    }

    /**
     * 获得 MappedStatement 对象
     */
    private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName,
        Class<?> declaringClass, Configuration configuration) {

      // 获得编号，由当前接口名 + '.' + 方法名
      String statementId = mapperInterface.getName() + "." + methodName;
      // 如果 configuration 中已经有了这个签名的 MappedStatement，就从 configuration 中获得 MappedStatement 对象，并返回
      // TODO configuration中的MappedStatement什么时候加进去的
      if (configuration.hasStatement(statementId)) {
        return configuration.getMappedStatement(statementId);

      } else if (mapperInterface.equals(declaringClass)) {
        // 如果没有，并且当前方法就是 declaringClass 声明的，则说明真的找不到
        return null;
      }
      // 找到方法的声明接口；遍历父接口，继续获得 MappedStatement 对象；因为已知declaringClass肯定是mapperInterface的父接口
      // getInterfaces会返回所有接口，包括父接口继承的接口
      for (Class<?> superInterface : mapperInterface.getInterfaces()) {
        // 遍历的接口必须是declaringClass的子类或者就是declaringClass
        // note 想复杂点，如父接口有A，B，C(extends A)，假如declaringClass是A，那么A，C都会判断为true，进入👇的逻辑，其中一个OK了就直接返回了
        if (declaringClass.isAssignableFrom(superInterface)) {
          // 递归调用
          MappedStatement ms = resolveMappedStatement(superInterface, methodName, declaringClass, configuration);
          if (ms != null) {
            return ms;
          }
        }
      }
      // 真的找不到，返回 null
      return null;
    }
  }

  /**
   * MapperMethod 的内部静态类，方法签名，处理方法的参数类型信息和返回类型信息
   */
  public static class MethodSignature {

    /**
     * 返回类型是否为集合
     */
    private final boolean returnsMany;

    /**
     * 返回类型是否为 Map，这个Map的key为{@link MapKey#value()}，value就是model，而不是key为columnName，value为columnValue
     */
    private final boolean returnsMap;

    /**
     * 返回类型是否为 void
     */
    private final boolean returnsVoid;

    /**
     * 返回类型是否为 {@link Cursor}
     */
    private final boolean returnsCursor;

    /**
     * 返回类型是否为 {@link Optional}
     */
    private final boolean returnsOptional;

    /**
     * 返回类型
     */
    private final Class<?> returnType;

    /**
     * 返回方法上的 {@link MapKey#value()} ，前提是返回类型为 Map
     */
    private final String mapKey;

    /**
     * 获得 {@link ResultHandler} 在方法参数中的位置。
     *
     * 如果为 null ，说明不存在这个类型
     */
    private final Integer resultHandlerIndex;

    /**
     * 获得 {@link RowBounds} 在方法参数中的位置。
     *
     * 如果为 null ，说明不存在这个类型
     */
    private final Integer rowBoundsIndex;

    /**
     * ParamNameResolver 对象
     */
    private final ParamNameResolver paramNameResolver;

    /**
     * 构造函数
     */
    public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
      // 初始化 returnType 属性
      Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
      if (resolvedReturnType instanceof Class<?>) {
        // 普通类
        this.returnType = (Class<?>) resolvedReturnType;
      } else if (resolvedReturnType instanceof ParameterizedType) {
        // 泛型
        this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
      } else {
        // 内部类等等
        this.returnType = method.getReturnType();
      }
      // 初始化 returnsVoid 属性
      this.returnsVoid = void.class.equals(this.returnType);
      // 初始化 returnsMany 属性
      this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
      // 初始化 returnsCursor 属性
      this.returnsCursor = Cursor.class.equals(this.returnType);
      // 初始化 returnsOptional 属性
      this.returnsOptional = Optional.class.equals(this.returnType);
      // 初始化 mapKey
      this.mapKey = getMapKey(method);
      // 初始化 returnsMap
      this.returnsMap = this.mapKey != null;
      // 初始化 rowBoundsIndex
      this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
      // 初始化 resultHandlerIndex
      this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
      // 初始化 paramNameResolver
      this.paramNameResolver = new ParamNameResolver(configuration, method);
    }

    /**
     * 转换SQL命令的参数，就是将方法参数值转换成统一的数据结构，如paramMap，strictMap
     */
    public Object convertArgsToSqlCommandParam(Object[] args) {
      /** paramNameResolver初始化时，已经记录所有参数到{@link ParamNameResolver#names}中 */
      return paramNameResolver.getNamedParams(args);
    }

    /**
     * 参数中是否有rowBounds
     */
    public boolean hasRowBounds() {
      return rowBoundsIndex != null;
    }

    /**
     * 提取出参数中的RowBounds
     */
    public RowBounds extractRowBounds(Object[] args) {
      return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
    }

    /**
     * 参数中是否有ResultHandler类型
     */
    public boolean hasResultHandler() {
      return resultHandlerIndex != null;
    }

    /**
     * 抽取参数中的ResultHandler
     */
    public ResultHandler extractResultHandler(Object[] args) {
      return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
    }

    /**
     * 获取mapKey
     */
    public String getMapKey() {
      return mapKey;
    }

    /**
     * 获取返回类型
     */
    public Class<?> getReturnType() {
      return returnType;
    }

    /**
     * 是否返回多条记录
     */
    public boolean returnsMany() {
      return returnsMany;
    }

    /**
     * 是否返回map类型
     */
    public boolean returnsMap() {
      return returnsMap;
    }

    /**
     * 是否无返回
     */
    public boolean returnsVoid() {
      return returnsVoid;
    }

    /**
     * 是否返回cursor类型
     */
    public boolean returnsCursor() {
      return returnsCursor;
    }

    /**
     * 返回类型是否是Optional
     * @since 3.5.0
     */
    public boolean returnsOptional() {
      return returnsOptional;
    }

    /**
     * 获得指定参数类型在方法参数中的位置
     */
    private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
      Integer index = null;
      // 遍历方法参数
      final Class<?>[] argTypes = method.getParameterTypes();
      for (int i = 0; i < argTypes.length; i++) {
        // 类型符合
        if (paramType.isAssignableFrom(argTypes[i])) {
          // 获得第一次的位置
          if (index == null) {
            index = i;
          } else {
            // 如果重复类型了，比如要查找的类型，在参数中有多个，则抛出 BindingException 异常
            throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
          }
        }
      }
      return index;
    }

    /**
     * 获得注解的 {@link MapKey#value()}
     */
    private String getMapKey(Method method) {
      String mapKey = null;
      // 返回类型为 Map
      if (Map.class.isAssignableFrom(method.getReturnType())) {
        // 使用 @MapKey 注解
        final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
        // 获得 @MapKey 注解的键
        if (mapKeyAnnotation != null) {
          mapKey = mapKeyAnnotation.value();
        }
      }
      return mapKey;
    }
  }

}
