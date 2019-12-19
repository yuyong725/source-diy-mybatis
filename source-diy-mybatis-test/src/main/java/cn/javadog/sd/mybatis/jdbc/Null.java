package cn.javadog.sd.mybatis.jdbc;


import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.BigDecimalTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.BlobTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.BooleanTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ByteArrayTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ByteTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ClobTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.DateOnlyTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.DateTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.DoubleTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.FloatTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.IntegerTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.LongTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ObjectTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.ShortTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.SqlDateTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.SqlTimeTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.SqlTimestampTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.StringTypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.TimeOnlyTypeHandler;

/**
 * @author 余勇
 * @date 2019-12-18 15:51
 *
 * null 类型的处理器
 */
public enum Null {

  /*各种类型的null值处理器*/

  BOOLEAN(new BooleanTypeHandler(), JdbcType.BOOLEAN),

  BYTE(new ByteTypeHandler(), JdbcType.TINYINT),
  SHORT(new ShortTypeHandler(), JdbcType.SMALLINT),
  INTEGER(new IntegerTypeHandler(), JdbcType.INTEGER),
  LONG(new LongTypeHandler(), JdbcType.BIGINT),
  FLOAT(new FloatTypeHandler(), JdbcType.FLOAT),
  DOUBLE(new DoubleTypeHandler(), JdbcType.DOUBLE),
  BIGDECIMAL(new BigDecimalTypeHandler(), JdbcType.DECIMAL),

  STRING(new StringTypeHandler(), JdbcType.VARCHAR),
  CLOB(new ClobTypeHandler(), JdbcType.CLOB),
  LONGVARCHAR(new ClobTypeHandler(), JdbcType.LONGVARCHAR),

  BYTEARRAY(new ByteArrayTypeHandler(), JdbcType.LONGVARBINARY),
  BLOB(new BlobTypeHandler(), JdbcType.BLOB),
  LONGVARBINARY(new BlobTypeHandler(), JdbcType.LONGVARBINARY),

  OBJECT(new ObjectTypeHandler(), JdbcType.OTHER),
  OTHER(new ObjectTypeHandler(), JdbcType.OTHER),
  TIMESTAMP(new DateTypeHandler(), JdbcType.TIMESTAMP),
  DATE(new DateOnlyTypeHandler(), JdbcType.DATE),
  TIME(new TimeOnlyTypeHandler(), JdbcType.TIME),
  SQLTIMESTAMP(new SqlTimestampTypeHandler(), JdbcType.TIMESTAMP),
  SQLDATE(new SqlDateTypeHandler(), JdbcType.DATE),
  SQLTIME(new SqlTimeTypeHandler(), JdbcType.TIME);

  /**
   * 处理器
   */
  private TypeHandler<?> typeHandler;

  /**
   * 对应的jdbc类型
   */
  private JdbcType jdbcType;

  /**
   * 构造，不对外暴露，按理这个类用不上
   */
  private Null(TypeHandler<?> typeHandler, JdbcType jdbcType) {
    this.typeHandler = typeHandler;
    this.jdbcType = jdbcType;
  }

  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  public JdbcType getJdbcType() {
    return jdbcType;
  }
}
