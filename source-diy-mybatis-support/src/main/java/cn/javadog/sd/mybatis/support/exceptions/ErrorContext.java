package cn.javadog.sd.mybatis.support.exceptions;

/**
 * @author 余勇
 * @date 2019-12-17 13:00
 *
 * 异常上下文。强烈推荐先看看：jianshu.com/p/901e37d05853
 */
public class ErrorContext {

  /**
   * 行分隔符
   */
  private static final String LINE_SEPARATOR = System.getProperty("line.separator","\n");

  /**
   * 真正的记录器，与线程有关
   */
  private static final ThreadLocal<ErrorContext> LOCAL = new ThreadLocal<>();

  /**
   * 已经存储的 ErrorContext
   */
  private ErrorContext stored;

  /**
   * 存储异常存在于哪个资源文件中。
   * ### The error may exist in mapper/AuthorMapper.xml
   */
  private String resource;

  /**
   * 存储异常是做什么操作时发生的。
   * ### The error occurred while setting parameters
   */
  private String activity;

  /**
   * 存储哪个对象操作时发生异常。
   * ### The error may involve defaultParameterMap
   */
  private String object;

  /**
   * message：存储异常的概览信息。
   * ### Error querying database. Cause: java.sql.SQLSyntaxErrorException: Unknown column 'id2' in 'field list'
   */
  private String message;

  /**
   * 存储发生日常的 SQL 语句。
   * ### SQL: select id2, name, sex, phone from author where name = ?
   */
  private String sql;

  /**
   * 存储详细的 Java 异常日志。
   * ### Cause: java.sql.SQLSyntaxErrorException: Unknown column 'id2' in 'field list' at
   * org.apache.ibatis.exceptions.ExceptionFactory.wrapException(ExceptionFactory.java:30) at
   * org.apache.ibatis.session.defaults.DefaultSqlSession.selectList(DefaultSqlSession.java:150) at
   * org.apache.ibatis.session.defaults.DefaultSqlSession.selectList(DefaultSqlSession.java:141) at
   * org.apache.ibatis.binding.MapperMethod.executeForMany(MapperMethod.java:139) at org.apache.ibatis.binding.MapperMethod.execute(MapperMethod.java:76)
   */
  private Throwable cause;

  /**
   * 构造函数，不对外开放
   */
  private ErrorContext() {
  }

  /**
   * 获取当前线程的 异常上下文
   */
  public static ErrorContext instance() {
    ErrorContext context = LOCAL.get();
    if (context == null) {
      context = new ErrorContext();
      LOCAL.set(context);
    }
    return context;
  }

  /**
   * 创建新的 ErrorContext ，将之前的 上下文 记录到该 ErrorContext 中。
   */
  public ErrorContext store() {
    ErrorContext newContext = new ErrorContext();
    newContext.stored = this;
    LOCAL.set(newContext);
    return LOCAL.get();
  }

  /**
   * 将 stored 的该 ErrorContext 实例传递给 LOCAL
   */
  public ErrorContext recall() {
    if (stored != null) {
      LOCAL.set(stored);
      stored = null;
    }
    return LOCAL.get();
  }

  /**
   * 设置 resource
   */
  public ErrorContext resource(String resource) {
    this.resource = resource;
    return this;
  }

  /**
   * 设置 activity
   */
  public ErrorContext activity(String activity) {
    this.activity = activity;
    return this;
  }

  /**
   * 设置 object
   */
  public ErrorContext object(String object) {
    this.object = object;
    return this;
  }

  /**
   * 设置 message
   */
  public ErrorContext message(String message) {
    this.message = message;
    return this;
  }

  /**
   * 设置 sql
   */
  public ErrorContext sql(String sql) {
    this.sql = sql;
    return this;
  }

  /**
   * 设置 cause
   */
  public ErrorContext cause(Throwable cause) {
    this.cause = cause;
    return this;
  }

  /**
   * 重置当前 ErrorContext
   */
  public ErrorContext reset() {
    resource = null;
    activity = null;
    object = null;
    message = null;
    sql = null;
    cause = null;
    LOCAL.remove();
    return this;
  }

  /**
   * 打印重要信息
   */
  @Override
  public String toString() {
    StringBuilder description = new StringBuilder();

    // message
    if (this.message != null) {
      description.append(LINE_SEPARATOR);
      description.append("### ");
      description.append(this.message);
    }

    // resource
    if (resource != null) {
      description.append(LINE_SEPARATOR);
      description.append("### The error may exist in ");
      description.append(resource);
    }

    // object
    if (object != null) {
      description.append(LINE_SEPARATOR);
      description.append("### The error may involve ");
      description.append(object);
    }

    // activity
    if (activity != null) {
      description.append(LINE_SEPARATOR);
      description.append("### The error occurred while ");
      description.append(activity);
    }

    // activity
    if (sql != null) {
      description.append(LINE_SEPARATOR);
      description.append("### SQL: ");
      description.append(sql.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim());
    }

    // cause
    if (cause != null) {
      description.append(LINE_SEPARATOR);
      description.append("### Cause: ");
      description.append(cause.toString());
    }

    return description.toString();
  }

}
