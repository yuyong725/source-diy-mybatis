package cn.javadog.sd.mybatis.test.jdbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 余勇
 * @date 2019-12-18 13:19
 * 抽象SQL
 */
public abstract class AbstractSQL<T> {

  /**
   * AND 连接符
   */
  private static final String AND = ") \nAND (";

  /**
   * OR 连接符
   */
  private static final String OR = ") \nOR (";

  /**
   * SQL 语句
   */
  private final SQLStatement sql = new SQLStatement();

  /**
   * 获取本身，也就是 T
   */
  public abstract T getSelf();

  /*👇一系列SQL拼接语句*/

  public T UPDATE(String table) {
    sql().statementType = SQLStatement.StatementType.UPDATE;
    sql().tables.add(table);
    return getSelf();
  }

  public T SET(String sets) {
    sql().sets.add(sets);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T SET(String... sets) {
    sql().sets.addAll(Arrays.asList(sets));
    return getSelf();
  }

  public T INSERT_INTO(String tableName) {
    sql().statementType = SQLStatement.StatementType.INSERT;
    sql().tables.add(tableName);
    return getSelf();
  }

  public T VALUES(String columns, String values) {
    sql().columns.add(columns);
    sql().values.add(values);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T INTO_COLUMNS(String... columns) {
    sql().columns.addAll(Arrays.asList(columns));
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T INTO_VALUES(String... values) {
    sql().values.addAll(Arrays.asList(values));
    return getSelf();
  }

  public T SELECT(String columns) {
    sql().statementType = SQLStatement.StatementType.SELECT;
    sql().select.add(columns);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T SELECT(String... columns) {
    sql().statementType = SQLStatement.StatementType.SELECT;
    sql().select.addAll(Arrays.asList(columns));
    return getSelf();
  }

  public T SELECT_DISTINCT(String columns) {
    sql().distinct = true;
    SELECT(columns);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T SELECT_DISTINCT(String... columns) {
    sql().distinct = true;
    SELECT(columns);
    return getSelf();
  }

  public T DELETE_FROM(String table) {
    sql().statementType = SQLStatement.StatementType.DELETE;
    sql().tables.add(table);
    return getSelf();
  }

  public T FROM(String table) {
    sql().tables.add(table);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T FROM(String... tables) {
    sql().tables.addAll(Arrays.asList(tables));
    return getSelf();
  }

  public T JOIN(String join) {
    sql().join.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T JOIN(String... joins) {
    sql().join.addAll(Arrays.asList(joins));
    return getSelf();
  }

  public T INNER_JOIN(String join) {
    sql().innerJoin.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T INNER_JOIN(String... joins) {
    sql().innerJoin.addAll(Arrays.asList(joins));
    return getSelf();
  }

  /**
   * LEFT_OUTER_JOIN 语句
   */
  public T LEFT_OUTER_JOIN(String join) {
    sql().leftOuterJoin.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T LEFT_OUTER_JOIN(String... joins) {
    sql().leftOuterJoin.addAll(Arrays.asList(joins));
    return getSelf();
  }

  public T RIGHT_OUTER_JOIN(String join) {
    sql().rightOuterJoin.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T RIGHT_OUTER_JOIN(String... joins) {
    sql().rightOuterJoin.addAll(Arrays.asList(joins));
    return getSelf();
  }

  public T OUTER_JOIN(String join) {
    sql().outerJoin.add(join);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T OUTER_JOIN(String... joins) {
    sql().outerJoin.addAll(Arrays.asList(joins));
    return getSelf();
  }

  public T WHERE(String conditions) {
    sql().where.add(conditions);
    sql().lastList = sql().where;
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T WHERE(String... conditions) {
    sql().where.addAll(Arrays.asList(conditions));
    sql().lastList = sql().where;
    return getSelf();
  }

  public T OR() {
    sql().lastList.add(OR);
    return getSelf();
  }

  public T AND() {
    sql().lastList.add(AND);
    return getSelf();
  }

  public T GROUP_BY(String columns) {
    sql().groupBy.add(columns);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T GROUP_BY(String... columns) {
    sql().groupBy.addAll(Arrays.asList(columns));
    return getSelf();
  }

  public T HAVING(String conditions) {
    sql().having.add(conditions);
    sql().lastList = sql().having;
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T HAVING(String... conditions) {
    sql().having.addAll(Arrays.asList(conditions));
    sql().lastList = sql().having;
    return getSelf();
  }

  public T ORDER_BY(String columns) {
    sql().orderBy.add(columns);
    return getSelf();
  }

  /**
   * @since 3.4.2
   */
  public T ORDER_BY(String... columns) {
    sql().orderBy.addAll(Arrays.asList(columns));
    return getSelf();
  }

  /**
   * 获取 sql
   */
  private SQLStatement sql() {
    return sql;
  }

  /**
   * 使用指定的 Appender
   */
  public <A extends Appendable> A usingAppender(A a) {
    sql().sql(a);
    return a;
  }

  /**
   * 也就是 SQL
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sql().sql(sb);
    return sb.toString();
  }

  /**
   * 内部类，用于拼接SQL
   */
  private static class SafeAppendable {

    /**
     * 拼接实现类
     */
    private final Appendable a;

    /**
     * 目前是否是空的
     */
    private boolean empty = true;

    /**
     * 构造函数
     */
    public SafeAppendable(Appendable a) {
      super();
      this.a = a;
    }

    /**
     * 拼接
     */
    public SafeAppendable append(CharSequence s) {
      try {
        // 将 empty 置为 false
        if (empty && s.length() > 0) {
          empty = false;
        }
        // 拼接
        a.append(s);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    /**
     * 当前SQL是否为空
     */
    public boolean isEmpty() {
      return empty;
    }

  }

  /**
   * SQL语句
   */
  private static class SQLStatement {

    /**
     * 内部枚举
     * Statement 类型。包括增删改查
     */
    public enum StatementType {
      DELETE, INSERT, SELECT, UPDATE
    }

    /**
     * Statement 类型
     */
    StatementType statementType;

    /*一堆用于拼接SQL的语句*/

    List<String> sets = new ArrayList<String>();
    List<String> select = new ArrayList<String>();
    List<String> tables = new ArrayList<String>();
    List<String> join = new ArrayList<String>();
    List<String> innerJoin = new ArrayList<String>();
    List<String> outerJoin = new ArrayList<String>();
    List<String> leftOuterJoin = new ArrayList<String>();
    List<String> rightOuterJoin = new ArrayList<String>();
    List<String> where = new ArrayList<String>();
    List<String> having = new ArrayList<String>();
    List<String> groupBy = new ArrayList<String>();
    List<String> orderBy = new ArrayList<String>();
    List<String> lastList = new ArrayList<String>();
    List<String> columns = new ArrayList<String>();
    List<String> values = new ArrayList<String>();
    boolean distinct;

    /**
     * 构造函数，不对外部开放
     */
    public SQLStatement() {
        // Prevent Synthetic Access
    }

    /**
     * sql从句
     * @param builder 拼接器，由其完成拼接的工作
     * @param open 开标签
     * @param close 闭标签
     * @param keyword 关键词，如表名，'SELECT'等
     * @param parts 一些要拼接的短句集合，如 select 的字段
     * @param conjunction 短句连接符，如','
     */
    private void sqlClause(SafeAppendable builder, String keyword, List<String> parts, String open, String close,
                           String conjunction) {
      if (!parts.isEmpty()) {
        if (!builder.isEmpty()) {
          builder.append("\n");
        }
        builder.append(keyword);
        builder.append(" ");
        builder.append(open);
        String last = "________";
        for (int i = 0, n = parts.size(); i < n; i++) {
          String part = parts.get(i);
          if (i > 0 && !part.equals(AND) && !part.equals(OR) && !last.equals(AND) && !last.equals(OR)) {
            builder.append(conjunction);
          }
          builder.append(part);
          last = part;
        }
        builder.append(close);
      }
    }

    /**
     * 拼接 查询 语句
     */
    private String selectSQL(SafeAppendable builder) {
      // 拼接 SELECT( DISTINCT)
      if (distinct) {
        sqlClause(builder, "SELECT DISTINCT", select, "", "", ", ");
      } else {
        sqlClause(builder, "SELECT", select, "", "", ", ");
      }
      // 拼接 FROM
      sqlClause(builder, "FROM", tables, "", "", ", ");
      // 拼接 join
      joins(builder);
      // 拼接 WHERE
      sqlClause(builder, "WHERE", where, "(", ")", " AND ");
      // 拼接 GROUP BY
      sqlClause(builder, "GROUP BY", groupBy, "", "", ", ");
      // 拼接 HAVING
      sqlClause(builder, "HAVING", having, "(", ")", " AND ");
      // 拼接 ORDER BY
      sqlClause(builder, "ORDER BY", orderBy, "", "", ", ");
      return builder.toString();
    }

    /**
     * 拼接 join
     */
    private void joins(SafeAppendable builder) {
      sqlClause(builder, "JOIN", join, "", "", "\nJOIN ");
      sqlClause(builder, "INNER JOIN", innerJoin, "", "", "\nINNER JOIN ");
      sqlClause(builder, "OUTER JOIN", outerJoin, "", "", "\nOUTER JOIN ");
      sqlClause(builder, "LEFT OUTER JOIN", leftOuterJoin, "", "", "\nLEFT OUTER JOIN ");
      sqlClause(builder, "RIGHT OUTER JOIN", rightOuterJoin, "", "", "\nRIGHT OUTER JOIN ");
    }

    /**
     * 拼接 插入 语句
     */
    private String insertSQL(SafeAppendable builder) {
      sqlClause(builder, "INSERT INTO", tables, "", "", "");
      sqlClause(builder, "", columns, "(", ")", ", ");
      sqlClause(builder, "VALUES", values, "(", ")", ", ");
      return builder.toString();
    }

    /**
     * 拼接 删除 语句
     */
    private String deleteSQL(SafeAppendable builder) {
      sqlClause(builder, "DELETE FROM", tables, "", "", "");
      sqlClause(builder, "WHERE", where, "(", ")", " AND ");
      return builder.toString();
    }

    /**
     * 拼接 更新 语句
     */
    private String updateSQL(SafeAppendable builder) {
      sqlClause(builder, "UPDATE", tables, "", "", "");
      joins(builder);
      sqlClause(builder, "SET", sets, "", "", ", ");
      sqlClause(builder, "WHERE", where, "(", ")", " AND ");
      return builder.toString();
    }

    /**
     * 将 Appendable 交由当前类发起 SQL 拼接操作
     */
    public String sql(Appendable a) {
      SafeAppendable builder = new SafeAppendable(a);
      if (statementType == null) {
        return null;
      }

      String answer;
      // 根据类型，进行拼接
      switch (statementType) {
        case DELETE:
          answer = deleteSQL(builder);
          break;

        case INSERT:
          answer = insertSQL(builder);
          break;

        case SELECT:
          answer = selectSQL(builder);
          break;

        case UPDATE:
          answer = updateSQL(builder);
          break;

        default:
          answer = null;
      }

      return answer;
    }
  }
}