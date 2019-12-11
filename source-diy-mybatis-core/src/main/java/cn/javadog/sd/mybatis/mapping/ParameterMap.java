package cn.javadog.sd.mybatis.mapping;

import java.util.Collections;
import java.util.List;

import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author: 余勇
 * @date: 2019-12-11 19:26
 *
 * 参数集合，对应 paramType="" 或 paramMap="" 标签属性
 * 已被废弃！老式风格的参数映射。更好的办法是使用内联参数和 parameterType 属性，此元素可能在将来被移除
 */
public class ParameterMap {

  private String id;
  private Class<?> type;
  private List<ParameterMapping> parameterMappings;

  private ParameterMap() {
  }

  public static class Builder {
    private ParameterMap parameterMap = new ParameterMap();

    public Builder(Configuration configuration, String id, Class<?> type, List<ParameterMapping> parameterMappings) {
      parameterMap.id = id;
      parameterMap.type = type;
      parameterMap.parameterMappings = parameterMappings;
    }

    public Class<?> type() {
      return parameterMap.type;
    }

    public ParameterMap build() {
      //lock down collections
      parameterMap.parameterMappings = Collections.unmodifiableList(parameterMap.parameterMappings);
      return parameterMap;
    }
  }

  public String getId() {
    return id;
  }

  public Class<?> getType() {
    return type;
  }

  public List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

}
