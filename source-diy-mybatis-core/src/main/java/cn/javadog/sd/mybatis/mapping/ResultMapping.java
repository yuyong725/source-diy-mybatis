package cn.javadog.sd.mybatis.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author 余勇
 * @date 2019-12-13 23:27
 *
 * resultMap的子元素。对应xml 中 <resultMap /> 的子标签，不一定是<result />标签，还可能是其他各种标签
 */
public class ResultMapping {

  /**
   * 全局配置
   */
  private Configuration configuration;

  /**
   * 对应的字段
   */
  private String property;

  /**
   * 对应的类名
   */
  private String column;

  /**
   * 字段的Java类型
   */
  private Class<?> javaType;

  /**
   * 字段的jdbc类型
   */
  private JdbcType jdbcType;

  /**
   * 字段的类型处理器，用于 javaType 与 jdbcType 的相互转换
   */
  private TypeHandler<?> typeHandler;

  /**
   * 内嵌的ResultMapId
   */
  private String nestedResultMapId;

  /**
   * 内存的select
   */
  private String nestedQueryId;

  /**
   * 非空列名。指定后，Mybatis 将只在这些列非空时才创建一个子对象。
   */
  private Set<String> notNullColumns;

  /**
   * 列名前缀，一般用于 resultMap 的复用
   * 如：<collection property="posts" ofType="Post" resultMap="blogPostResult" columnPrefix="post_"/>
   */
  private String columnPrefix;

  /**
   * 特殊标签
   */
  private List<ResultFlag> flags;

  /**
   * 内嵌子元素
   */
  private List<ResultMapping> composites;

  /**
   * 是否懒加载，用于关联查询
   */
  private boolean lazy;

  /**
   * 构造函数，不对外开放。由👇的构造器调用
   */
  ResultMapping() {
  }

  /**
   * ResultMapping 的 构造器
   */
  public static class Builder {

    /**
     * ResultMapping 空对象
     */
    private ResultMapping resultMapping = new ResultMapping();

    /**
     * 构造函数。会将接收的参数设置到 resultMapping
     */
    public Builder(Configuration configuration, String property, String column, TypeHandler<?> typeHandler) {
      this(configuration, property);
      resultMapping.column = column;
      resultMapping.typeHandler = typeHandler;
    }

    /**
     * 构造函数。会将接收的参数设置到 resultMapping。
     * 与👆的构造的区别，在于一个是 typeHandler，一个是javaType，最终还是会以javaType找到typeHandler并赋值
     */
    public Builder(Configuration configuration, String property, String column, Class<?> javaType) {
      this(configuration, property);
      resultMapping.column = column;
      resultMapping.javaType = javaType;
    }

    /**
     * 构造函数。针对的是非常规的标签，通俗点说，就是除 <result /> 以外的标签
     */
    public Builder(Configuration configuration, String property) {
      resultMapping.configuration = configuration;
      resultMapping.property = property;
      // 设置默认值为空数组
      resultMapping.flags = new ArrayList<>();
      resultMapping.composites = new ArrayList<>();
      // 使用全局属性
      resultMapping.lazy = configuration.isLazyLoadingEnabled();
    }

    /**
     * 设置 javaType
     */
    public Builder javaType(Class<?> javaType) {
      resultMapping.javaType = javaType;
      return this;
    }

    /**
     * 设置 jdbcType
     */
    public Builder jdbcType(JdbcType jdbcType) {
      resultMapping.jdbcType = jdbcType;
      return this;
    }

    /**
     * 设置 nestedResultMapId
     */
    public Builder nestedResultMapId(String nestedResultMapId) {
      resultMapping.nestedResultMapId = nestedResultMapId;
      return this;
    }

    /**
     * 设置 nestedQueryId
     */
    public Builder nestedQueryId(String nestedQueryId) {
      resultMapping.nestedQueryId = nestedQueryId;
      return this;
    }

    /**
     * 设置 notNullColumns
     */
    public Builder notNullColumns(Set<String> notNullColumns) {
      resultMapping.notNullColumns = notNullColumns;
      return this;
    }

    /**
     * 设置 columnPrefix
     */
    public Builder columnPrefix(String columnPrefix) {
      resultMapping.columnPrefix = columnPrefix;
      return this;
    }

    /**
     * 设置 flags
     */
    public Builder flags(List<ResultFlag> flags) {
      resultMapping.flags = flags;
      return this;
    }

    /**
     * 设置 typeHandler
     */
    public Builder typeHandler(TypeHandler<?> typeHandler) {
      resultMapping.typeHandler = typeHandler;
      return this;
    }

    /**
     * 设置 composites
     */
    public Builder composites(List<ResultMapping> composites) {
      resultMapping.composites = composites;
      return this;
    }

    /**
     * 设置 lazy
     */
    public Builder lazy(boolean lazy) {
      resultMapping.lazy = lazy;
      return this;
    }

    /**
     * 执行构建
     */
    public ResultMapping build() {
      // 将集合属性锁住
      resultMapping.flags = Collections.unmodifiableList(resultMapping.flags);
      resultMapping.composites = Collections.unmodifiableList(resultMapping.composites);
      // 解析typeHandler
      resolveTypeHandler();
      validate();
      return resultMapping;
    }

    /**
     * 校验属性的值，相互的组合是否合法
     */
    private void validate() {
      // 不能同时定义 nestedQueryId 和 nestedResultMapId，参考：Issue #697
      if (resultMapping.nestedQueryId != null && resultMapping.nestedResultMapId != null) {
        throw new IllegalStateException("Cannot define both nestedQueryId and nestedResultMapId in property " + resultMapping.property);
      }
      // 所有的 mappings 都必须有 typehandler，参考：Issue #5
      if (resultMapping.nestedQueryId == null && resultMapping.nestedResultMapId == null && resultMapping.typeHandler == null) {
        throw new IllegalStateException("No typehandler found for property " + resultMapping.property);
      }
      // 只有在有内嵌的resultmaps的情况下，column 属性才可以为空，参考：Issue #4 和 GH #39
      if (resultMapping.nestedResultMapId == null && resultMapping.column == null && resultMapping.composites.isEmpty()) {
        throw new IllegalStateException("Mapping is missing column attribute for property " + resultMapping.property);
      }
    }

    /**
     * 解析typeHandler
     */
    private void resolveTypeHandler() {
      if (resultMapping.typeHandler == null && resultMapping.javaType != null) {
        Configuration configuration = resultMapping.configuration;
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        resultMapping.typeHandler = typeHandlerRegistry.getTypeHandler(resultMapping.javaType, resultMapping.jdbcType);
      }
    }

    /**
     * 设置 column
     */
    public Builder column(String column) {
      resultMapping.column = column;
      return this;
    }
  }

  /*所有属性的get方法👇*/

  public String getProperty() {
    return property;
  }

  public String getColumn() {
    return column;
  }

  public Class<?> getJavaType() {
    return javaType;
  }

  public JdbcType getJdbcType() {
    return jdbcType;
  }

  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  public String getNestedResultMapId() {
    return nestedResultMapId;
  }

  public String getNestedQueryId() {
    return nestedQueryId;
  }

  public Set<String> getNotNullColumns() {
    return notNullColumns;
  }

  public String getColumnPrefix() {
    return columnPrefix;
  }

  public List<ResultFlag> getFlags() {
    return flags;
  }

  public List<ResultMapping> getComposites() {
    return composites;
  }

  public boolean isCompositeResult() {
    return this.composites != null && !this.composites.isEmpty();
  }

  public boolean isLazy() {
    return lazy;
  }

  public void setLazy(boolean lazy) {
    this.lazy = lazy;
  }

  /**
   * 重写equal，只要类型相同，字段名相同，就相等。
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ResultMapping that = (ResultMapping) o;

    if (property == null || !property.equals(that.property)) {
      return false;
    }

    return true;
  }

  /**
   * 重写hashcode，使用的就是 property 或 column 的code
   */
  @Override
  public int hashCode() {
    if (property != null) {
      return property.hashCode();
    } else if (column != null) {
      return column.hashCode();
    } else {
      return 0;
    }
  }

  /**
   * 重写toString，打印简单的信息
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ResultMapping{");
    //sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
    sb.append("property='").append(property).append('\'');
    sb.append(", column='").append(column).append('\'');
    sb.append(", javaType=").append(javaType);
    sb.append(", jdbcType=").append(jdbcType);
    //sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
    sb.append(", nestedResultMapId='").append(nestedResultMapId).append('\'');
    sb.append(", nestedQueryId='").append(nestedQueryId).append('\'');
    sb.append(", notNullColumns=").append(notNullColumns);
    sb.append(", columnPrefix='").append(columnPrefix).append('\'');
    sb.append(", flags=").append(flags);
    sb.append(", composites=").append(composites);
    sb.append(", lazy=").append(lazy);
    sb.append('}');
    return sb.toString();
  }

}
