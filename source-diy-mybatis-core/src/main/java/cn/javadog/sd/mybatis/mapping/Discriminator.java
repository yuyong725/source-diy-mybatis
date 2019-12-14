package cn.javadog.sd.mybatis.mapping;

import java.util.Collections;
import java.util.Map;

import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author 余勇
 * @date 2019-12-11 21:41
 *
 * 鉴别器，如:
 * <discriminator javaType="int" column="draft">
 *   <case value="1" resultType="DraftPost"/>
 * </discriminator>
 */
public class Discriminator {

  /**
   * 鉴别器对应的 ResultMapping 对象，因为解析时，也会将鉴别器解析为一个 ResultMapping
   */
  private ResultMapping resultMapping;

  /**
   * discriminator 下的case标签属性，key 是case的value，如'1'，而value呢是拼接了对应的resultMapId, 如'blogs-1'
   */
  private Map<String, String> discriminatorMap;

  /**
   * 空构造
   */
  Discriminator() {
  }

  /**
   * 内部类，Discriminator的构造器
   */
  public static class Builder {

    /**
     * 需要构造的Discriminator对象，也就是说，构建是会将很多属性设置到这个对象里面再返回给调用者
     */
    private Discriminator discriminator = new Discriminator();

    /**
     * 构造函数
     */
    public Builder(Configuration configuration, ResultMapping resultMapping, Map<String, String> discriminatorMap) {
      discriminator.resultMapping = resultMapping;
      discriminator.discriminatorMap = discriminatorMap;
    }

    /**
     * 执行构建，主要就是些断言，毕竟方法没有参数
     */
    public Discriminator build() {
      // 断言 resultMapping 不能为空
      assert discriminator.resultMapping != null;
      // 断言 discriminatorMap 不能为空，且起码有一个属性
      assert discriminator.discriminatorMap != null;
      assert !discriminator.discriminatorMap.isEmpty();
      //将其 discriminatorMap 包装成一个不能更改的map
      discriminator.discriminatorMap = Collections.unmodifiableMap(discriminator.discriminatorMap);
      return discriminator;
    }
  }

  /**
   * 获取鉴别器对应的 resultMapping
   */
  public ResultMapping getResultMapping() {
    return resultMapping;
  }

  /**
   * 获取鉴别器的case属性
   */
  public Map<String, String> getDiscriminatorMap() {
    return discriminatorMap;
  }

  /**
   * 或者指定的case
   */
  public String getMapIdFor(String s) {
    return discriminatorMap.get(s);
  }

}
