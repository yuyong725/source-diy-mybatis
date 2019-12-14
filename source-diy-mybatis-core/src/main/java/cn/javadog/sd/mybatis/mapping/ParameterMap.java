package cn.javadog.sd.mybatis.mapping;

import java.util.Collections;
import java.util.List;

import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author 余勇
 * @date 2019-12-11 19:26
 *
 * 参数集合，对应 parameterType="" 或 parameterMap="" 标签属性
 * 已被废弃！老式风格的参数映射。更好的办法是使用内联参数和 parameterType 属性.
 * 格式如：
 * <parameterMap type="map" id="testParameterMap">
 *   <parameter property="addend1" jdbcType="INTEGER" mode="IN"/>
 *   <parameter property="sum" jdbcType="INTEGER" mode="OUT"/>
 * </parameterMap>
 */
public class ParameterMap {

  /**
   * 唯一标示
   */
  private String id;

  /**
   * 对应的 type 类型
   */
  private Class<?> type;

  /**
   * <parameterMap /> 下的所有 <parameter />
   */
  private List<ParameterMapping> parameterMappings;

  /**
   * 构造，对外不开放，有👇的构造器调用
   */
  private ParameterMap() {
  }

  /**
   * 内部类，ParameterMap 的构造器
   */
  public static class Builder {

    /**
     * 要构建的ParameterMap对象，空构造，属性由下面的构造方法设置
     */
    private ParameterMap parameterMap = new ParameterMap();

    /**
     * 构造函数
     */
    public Builder(Configuration configuration, String id, Class<?> type, List<ParameterMapping> parameterMappings) {
      parameterMap.id = id;
      parameterMap.type = type;
      parameterMap.parameterMappings = parameterMappings;
    }

    /**
     * 获取parameterMap 的type属性
     */
    public Class<?> type() {
      return parameterMap.type;
    }

    /**
     * 执行构建
     */
    public ParameterMap build() {
      //将 parameterMappings 属性锁起来
      parameterMap.parameterMappings = Collections.unmodifiableList(parameterMap.parameterMappings);
      return parameterMap;
    }
  }

  /**
   * 获取唯一标示
   */
  public String getId() {
    return id;
  }

  /**
   * 获取 type
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * 获取所有的 ParameterMapping
   */
  public List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

}
