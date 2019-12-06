package cn.javadog.sd.mybatis.support.type;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: 余勇
 * @date: 2019-12-06 13:21
 * Jdbc Type 枚举
 * mysql的数据类型参见：https://wiki.jikexueyuan.com/project/mysql/data-types.html
 * 本想细致的讲解一下，顺带复习一下mysql的数据类型与应用场景，一篇好文章都没有。。
 */
public enum JdbcType {

  /**
   * 数组。MySQL是不支持Array的。这个类是为了支持数组类型的Java参数
   */
  ARRAY(Types.ARRAY),

  /**
   * 字节，一般用来处理boolean类型，之前用tinyint，实际应该用这个
   */
  BIT(Types.BIT),

  /**
   * TINYINT
   * 非常小的整数，分为有无符号两种。前有符号时，其允许取值范围是-128~127；无符号时的取值范围为0~255。所以，最高可指定4位数字。
   */
  TINYINT(Types.TINYINT),

  /**
   * SMALLINT
   * 较小的整数，分为有无符号两种。前有符号时，其允许取值范围是-32768~32767；无符号时的取值范围为0~65535。所以最高可指定5位数字
   */
  SMALLINT(Types.SMALLINT),

  /**
   * INTEGER
   */
  INTEGER(Types.INTEGER),

  /**
   * BIGINT
   */
  BIGINT(Types.BIGINT),

  /**
   * FLOAT
   */
  FLOAT(Types.FLOAT),

  /**
   * REAL
   */
  REAL(Types.REAL),

  /**
   * DOUBLE
   */
  DOUBLE(Types.DOUBLE),

  /**
   * NUMERIC
   */
  NUMERIC(Types.NUMERIC),

  /**
   * DECIMAL
   */
  DECIMAL(Types.DECIMAL),

  /**
   * CHAR
   */
  CHAR(Types.CHAR),

  /**
   * VARCHAR
   */
  VARCHAR(Types.VARCHAR),

  /**
   * LONGVARCHAR
   */
  LONGVARCHAR(Types.LONGVARCHAR),

  /**
   * DATE
   */
  DATE(Types.DATE),

  /**
   * TIME
   */
  TIME(Types.TIME),

  /**
   * TIMESTAMP
   */
  TIMESTAMP(Types.TIMESTAMP),

  /**
   * BINARY
   */
  BINARY(Types.BINARY),

  /**
   * VARBINARY
   */
  VARBINARY(Types.VARBINARY),

  /**
   * LONGVARBINARY
   */
  LONGVARBINARY(Types.LONGVARBINARY),

  /**
   * NULL
   */
  NULL(Types.NULL),

  /**
   * OTHER
   */
  OTHER(Types.OTHER),

  /**
   * BLOB
   */
  BLOB(Types.BLOB),

  /**
   * CLOB
   */
  CLOB(Types.CLOB),

  /**
   * BOOLEAN
   */
  BOOLEAN(Types.BOOLEAN),

  /**
   * CURSOR
   * Oracle
   */
  CURSOR(-10),

  /**
   * UNDEFINED
   */
  UNDEFINED(Integer.MIN_VALUE + 1000),

  /**
   * NVARCHAR
   * JDK6
   */
  NVARCHAR(Types.NVARCHAR),

  /**
   * NCHAR
   * JDK6
   */
  NCHAR(Types.NCHAR),

  /**
   * NCLOB
   * JDK6
   */
  NCLOB(Types.NCLOB),

  /**
   * STRUCT
   */
  STRUCT(Types.STRUCT),

  /**
   * JAVA_OBJECT
   */
  JAVA_OBJECT(Types.JAVA_OBJECT),

  /**
   * DISTINCT
   */
  DISTINCT(Types.DISTINCT),

  /**
   * REF
   */
  REF(Types.REF),

  /**
   * DATALINK
   */
  DATALINK(Types.DATALINK),

  /**
   * ROWID
   * JDK6
   */
  ROWID(Types.ROWID),

  /**
   * LONGNVARCHAR
   * JDK6
   */
  LONGNVARCHAR(Types.LONGNVARCHAR),

  /**
   * SQLXML
   * JDK6
   */
  SQLXML(Types.SQLXML),

  /**
   * DATETIMEOFFSET
   * SQL Server 2008
   */
  DATETIMEOFFSET(-155);

  /**
   * 类型编号。嘿嘿，此处代码不规范
   */
  public final int TYPE_CODE;

  /**
   * 代码编号和 {@link JdbcType} 的映射
   */
  private static Map<Integer,JdbcType> codeLookup = new HashMap<>();

  /**
   * 类加载的时候就把所有类型都加进去
   */
  static {
    // 初始化 codeLookup
    for (JdbcType type : JdbcType.values()) {
      codeLookup.put(type.TYPE_CODE, type);
    }
  }

  /**
   * 构造，这个是内部那些枚举用的
   */
  JdbcType(int code) {
    this.TYPE_CODE = code;
  }

  /**
   * 根据code拿类型
   */
  public static JdbcType forCode(int code)  {
    return codeLookup.get(code);
  }

}
