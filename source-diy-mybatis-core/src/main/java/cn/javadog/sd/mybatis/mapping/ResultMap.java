package cn.javadog.sd.mybatis.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cn.javadog.sd.mybatis.annotations.Param;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import cn.javadog.sd.mybatis.support.util.ParamNameUtil;

/**
 * @author: 余勇
 * @date: 2019-12-13 22:21
 * ResultMap标签的信息，对应 <resultMap /> 标签 或 {@link cn.javadog.sd.mybatis.annotations.ResultMap}
 */
public class ResultMap {

  /**
   * 全局配置
   */
  private Configuration configuration;

  /**
   * 唯一标示
   */
  private String id;

  /**
   * type属性，一般是 POJO 类
   */
  private Class<?> type;

  /**
   * 所有各种 子标签 的大一统
   */
  private List<ResultMapping> resultMappings;

  /**
   * 所有 <id /> 子标签
   */
  private List<ResultMapping> idResultMappings;

  /**
   * 所有 <constructor /> 子标签
   */
  private List<ResultMapping> constructorResultMappings;

  /**
   * 所有除了 <constructor /> 的子标签，可能是 <result /> ,也可能是 <discriminator />
   */
  private List<ResultMapping> propertyResultMappings;

  /**
   * 所有子标签的 column(数据库表字段名) 集合
   */
  private Set<String> mappedColumns;

  /**
   * 所有子标签的 property(POJO字段名) 集合
   */
  private Set<String> mappedProperties;

  /**
   * discriminator 标签，只会有一个
   */
  private Discriminator discriminator;

  /**
   * 是否有嵌套的resultMap，如 association 标签就会引入嵌套的resultMap
   */
  private boolean hasNestedResultMaps;

  /**
   * 是否有嵌套的查询
   */
  private boolean hasNestedQueries;

  /**
   * 是否开启了数据库表列名自动映射POJO类字段名
   */
  private Boolean autoMapping;

  /**
   * 构造函数，不对外暴露，由👇的构造器调用
   */
  private ResultMap() {
  }

  /**
   * 内部类，ResultMap 的构造器
   */
  public static class Builder {

    /**
     * 日志打印器
     */
    private static final Log log = LogFactory.getLog(Builder.class);

    /**
     * 空的 ResultMap 对象，构建时会赋予相应属性
     */
    private ResultMap resultMap = new ResultMap();

    /**
     * 构造函数
     */
    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings) {
      this(configuration, id, type, resultMappings, null);
    }

    /**
     * 构造函数，多了个 autoMapping
     */
    public Builder(Configuration configuration, String id, Class<?> type, List<ResultMapping> resultMappings, Boolean autoMapping) {
      resultMap.configuration = configuration;
      resultMap.id = id;
      resultMap.type = type;
      resultMap.resultMappings = resultMappings;
      resultMap.autoMapping = autoMapping;
    }

    /**
     * 设置 discriminator
     */
    public Builder discriminator(Discriminator discriminator) {
      resultMap.discriminator = discriminator;
      return this;
    }

    /**
     * 获取 type
     */
    public Class<?> type() {
      return resultMap.type;
    }

    /**
     * 执行构建
     */
    public ResultMap build() {
      // 校验 ID，为空直接GG
      if (resultMap.id == null) {
        throw new IllegalArgumentException("ResultMaps must have an id");
      }
      // 设置一些默认属性，都是空集合
      resultMap.mappedColumns = new HashSet<>();
      resultMap.mappedProperties = new HashSet<>();
      resultMap.idResultMappings = new ArrayList<>();
      resultMap.constructorResultMappings = new ArrayList<>();
      resultMap.propertyResultMappings = new ArrayList<>();
      final List<String> constructorArgNames = new ArrayList<>();
      // 遍历 resultMappings
      for (ResultMapping resultMapping : resultMap.resultMappings) {
        // 判断是否有嵌套查询
        resultMap.hasNestedQueries = resultMap.hasNestedQueries || resultMapping.getNestedQueryId() != null;
        // 判断是否有内嵌的ResultMap
        resultMap.hasNestedResultMaps = resultMap.hasNestedResultMaps || (resultMapping.getNestedResultMapId() != null && resultMapping.getResultSet() == null);
        // 拿到 <result /> 标签对应的列名
        final String column = resultMapping.getColumn();
        if (column != null) {
          // 加到 mappedColumns 里面
          resultMap.mappedColumns.add(column.toUpperCase(Locale.ENGLISH));
        } else if (resultMapping.isCompositeResult()) {
          // 针对 有子标签的场景，如 association 标签就没有 column，但提供 resultMap 属性关联很多 子集，进行遍历添加
          for (ResultMapping compositeResultMapping : resultMapping.getComposites()) {
            final String compositeColumn = compositeResultMapping.getColumn();
            if (compositeColumn != null) {
              resultMap.mappedColumns.add(compositeColumn.toUpperCase(Locale.ENGLISH));
            }
          }
        }
        // 拿到 <result /> 标签对应的字段名
        final String property = resultMapping.getProperty();
        if(property != null) {
          // 添加到 mappedProperties
          resultMap.mappedProperties.add(property);
        }
        // 如果是 <constructor /> 子标签的元素
        if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
          // 添加到 constructorResultMappings
          resultMap.constructorResultMappings.add(resultMapping);
          if (resultMapping.getProperty() != null) {
            // 添加到 constructorArgNames
            constructorArgNames.add(resultMapping.getProperty());
          }
        } else {
          // 其他的就加到 propertyResultMappings，note 从这里看，propertyResultMappings 对应的是 constructorResultMappings
          resultMap.propertyResultMappings.add(resultMapping);
        }
        // 将 id标签，包括 <idArg /> 和 <id /> 添加到 idResultMappings
        if (resultMapping.getFlags().contains(ResultFlag.ID)) {
          resultMap.idResultMappings.add(resultMapping);
        }
      }
      // 如果 idResultMappings 是空的，也就是一个都没有，直接放大招，将 resultMappings 全加进去。
      // note 留个心眼，貌似这会造成性能问题
      if (resultMap.idResultMappings.isEmpty()) {
        resultMap.idResultMappings.addAll(resultMap.resultMappings);
      }
      // 如果constructorArgNames，也就是构造函数的参数不为空
      if (!constructorArgNames.isEmpty()) {
        // 校验构造函数的参数名，参数类型是否与xml解析得到的结果完全一致，不一致就返回null，一致将构造函数解析的参数名结果返回
        final List<String> actualArgNames = argNamesOfMatchingConstructor(constructorArgNames);
        if (actualArgNames == null) {
          // 不一致直接GG
          throw new BuilderException("Error in result map '" + resultMap.id
              + "'. Failed to find a constructor in '"
              + resultMap.getType().getName() + "' by arg names " + constructorArgNames
              + ". There might be more info in debug log.");
        }
        // 将 constructorResultMappings 按照 actualArgNames，也就是构造函数里参数的顺序进行排序
        Collections.sort(resultMap.constructorResultMappings, (o1, o2) -> {
          int paramIdx1 = actualArgNames.indexOf(o1.getProperty());
          int paramIdx2 = actualArgNames.indexOf(o2.getProperty());
          return paramIdx1 - paramIdx2;
        });
      }
      // 将几个集合类型的属性锁起来
      resultMap.resultMappings = Collections.unmodifiableList(resultMap.resultMappings);
      resultMap.idResultMappings = Collections.unmodifiableList(resultMap.idResultMappings);
      resultMap.constructorResultMappings = Collections.unmodifiableList(resultMap.constructorResultMappings);
      resultMap.propertyResultMappings = Collections.unmodifiableList(resultMap.propertyResultMappings);
      resultMap.mappedColumns = Collections.unmodifiableSet(resultMap.mappedColumns);
      // 返回
      return resultMap;
    }

    /**
     * 校验通过 <constructor /> 下的子标签拿到的构造函数参数名，与resultMap的type通过反射拿到的参数名是否完全一致，
     * 能完全匹配的上就用，不然就返回null
     */
    private List<String> argNamesOfMatchingConstructor(List<String> constructorArgNames) {
      // 拿到resultMap的type 对应的构造方法
      Constructor<?>[] constructors = resultMap.type.getDeclaredConstructors();
      // 遍历构造方法
      for (Constructor<?> constructor : constructors) {
        // 获取所有参数
        Class<?>[] paramTypes = constructor.getParameterTypes();
        // 先找到参数数量正好一致的
        if (constructorArgNames.size() == paramTypes.length) {
          // 获取该构造方法所有参数的名字
          List<String> paramNames = getArgNames(constructor);
          // 如果 解析xml得到的属性名与解析构造方法得到的属性名完全一致
          if (constructorArgNames.containsAll(paramNames)
              // 并且类型也匹配的话，直接返回
              && argTypesMatch(constructorArgNames, paramTypes, paramNames)) {
            return paramNames;
          }
        }
      }
      // 但凡有一点不匹配，返回null
      return null;
    }

    /**
     * 校验通过构造方法拿到的参数类型，与从xml解析出来的JavaType是否完全一致
     */
    private boolean argTypesMatch(final List<String> constructorArgNames,
        Class<?>[] paramTypes, List<String> paramNames) {
      for (int i = 0; i < constructorArgNames.size(); i++) {
        // 获取构造方法解析得到的参数类型
        Class<?> actualType = paramTypes[paramNames.indexOf(constructorArgNames.get(i))];
        // 获取xml标签小的 javaType 属性
        Class<?> specifiedType = resultMap.constructorResultMappings.get(i).getJavaType();
        // 如果有一个不一致，直接返回 false
        if (!actualType.equals(specifiedType)) {
          if (log.isDebugEnabled()) {
            log.debug("While building result map '" + resultMap.id
                + "', found a constructor with arg names " + constructorArgNames
                + ", but the type of '" + constructorArgNames.get(i)
                + "' did not match. Specified: [" + specifiedType.getName() + "] Declared: ["
                + actualType.getName() + "]");
          }
          return false;
        }
      }
      return true;
    }

    /**
     * 获取指定构造方法所有参数的名字
     */
    private List<String> getArgNames(Constructor<?> constructor) {
      List<String> paramNames = new ArrayList<>();
      List<String> actualParamNames = null;
      // 获取构造方法所有参数的注解，二维数组
      final Annotation[][] paramAnnotations = constructor.getParameterAnnotations();
      // 记录参数的数量
      int paramCount = paramAnnotations.length;
      // 遍历参数
      for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
        String name = null;
        // 遍历参数的注解
        for (Annotation annotation : paramAnnotations[paramIndex]) {
          if (annotation instanceof Param) {
            // 参数上有@param注解的话，就用注解的值
            // note 这里标明，@param 不是只能用在mapper的方法参数上，也可以用在pojo 的构造方法的参数上
            name = ((Param) annotation).value();
            break;
          }
        }
        // 如果注解上没找到@param，而全局配置允许使用参数名
        if (name == null && resultMap.configuration.isUseActualParamName()) {
          if (actualParamNames == null) {
            // 解析构造方法所有的参数
            actualParamNames = ParamNameUtil.getParamNames(constructor);
          }
          // 感觉这个校验没有意义
          if (actualParamNames.size() > paramIndex) {
            // 从 actualParamNames 拿到指定位置的参数名，actualParamNames 与 paramAnnotations 的参数顺序是一致的
            name = actualParamNames.get(paramIndex);
          }
        }
        // 如果还没空，就以 arg开头+下标。note 在这里见到了之前debug遇到的 arg0，不过这不是解析 方法参数，也就是 paramMap
        paramNames.add(name != null ? name : "arg" + paramIndex);
      }
      return paramNames;
    }
  }

  /*所有属性的get方法👇*/

  public String getId() {
    return id;
  }

  public boolean hasNestedResultMaps() {
    return hasNestedResultMaps;
  }

  public boolean hasNestedQueries() {
    return hasNestedQueries;
  }

  public Class<?> getType() {
    return type;
  }

  public List<ResultMapping> getResultMappings() {
    return resultMappings;
  }

  public List<ResultMapping> getConstructorResultMappings() {
    return constructorResultMappings;
  }

  public List<ResultMapping> getPropertyResultMappings() {
    return propertyResultMappings;
  }

  public List<ResultMapping> getIdResultMappings() {
    return idResultMappings;
  }

  public Set<String> getMappedColumns() {
    return mappedColumns;
  }

  public Set<String> getMappedProperties() {
    return mappedProperties;
  }

  public Discriminator getDiscriminator() {
    return discriminator;
  }

  public void forceNestedResultMaps() {
    hasNestedResultMaps = true;
  }
  
  public Boolean getAutoMapping() {
    return autoMapping;
  }

}
