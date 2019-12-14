package cn.javadog.sd.mybatis.session;

import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.support.exceptions.SqlSessionException;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * @author 余勇
 * @date 2019-12-11 13:19
 *
 * 指定 当resultMap对应的POJO中，
 * 没有对应的属性(比如 select * 查出来10列，但POJO只有9个属性)；
 * 或者有属性未映射到(比如 select * 查出来10列，但POJO只有11个属性)
 *
 * TODO 不确定上面的解释对不对，还是仅仅指
 */
public enum AutoMappingUnknownColumnBehavior {

  /**
   * 啥事也不干
   */
  NONE {
    @Override
    public void doAction(MappedStatement mappedStatement, String columnName, String property, Class<?> propertyType) {
      // do nothing
    }
  },

  /**
   * 输出 warn 级别的日志
   * note：源码里提到这个日志级别必须是WARN
   */
  WARNING {
    @Override
    public void doAction(MappedStatement mappedStatement, String columnName, String property, Class<?> propertyType) {
      // 打印warn级别的日志
      log.warn(buildMessage(mappedStatement, columnName, property, propertyType));
    }
  },

  /**
   * 直接映射失败的错
   */
  FAILING {
    @Override
    public void doAction(MappedStatement mappedStatement, String columnName, String property, Class<?> propertyType) {
      throw new SqlSessionException(buildMessage(mappedStatement, columnName, property, propertyType));
    }
  };

  /**
   * Logger，注意这个必须写在枚举实例的后面
   */
  private static final Log log = LogFactory.getLog(AutoMappingUnknownColumnBehavior.class);

  /**
   * 当出现类名或者字段名匹配不上是的行为
   * @param mappedStatement 当前的mappedStatement，一个语句对应一个mappedStatement
   * @param columnName 映射的数据库字段名
   * @param propertyName 映射的POJO字段名
   * @param propertyType 映射的POJO字段类型 (如果这个参数不为空，对应此类型TypeHandler也没有注册) TODO 然后呢，这个英文注释很懵逼
   */
  public abstract void doAction(MappedStatement mappedStatement, String columnName, String propertyName, Class<?> propertyType);

  /**
   * 构建错误信息
   */
  private static String buildMessage(MappedStatement mappedStatement, String columnName, String property, Class<?> propertyType) {
    return new StringBuilder("Unknown column is detected on '")
      .append(mappedStatement.getId())
      .append("' auto-mapping. Mapping parameters are ")
      .append("[")
      .append("columnName=").append(columnName)
      .append(",").append("propertyName=").append(property)
      .append(",").append("propertyType=").append(propertyType != null ? propertyType.getName() : null)
      .append("]")
      .toString();
  }

}
