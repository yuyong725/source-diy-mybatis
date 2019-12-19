package cn.javadog.sd.mybatis.jdbc;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 余勇
 * @date 2019-12-18 14:12
 * SQL脚本运行器
 */
public class ScriptRunner {

  /**
   * 换行符
   */
  private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

  /**
   * 默认分隔符
   */
  private static final String DEFAULT_DELIMITER = ";";

  /**
   * 分隔符正则
   */
  private static final Pattern DELIMITER_PATTERN = Pattern.compile("^\\s*((--)|(//))?\\s*(//)?\\s*@DELIMITER\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE);

  /**
   * 连接对象
   */
  private final Connection connection;

  /**
   * 遇到错误时是否中止
   */
  private boolean stopOnError;

  /**
   * 是否抛出警告
   */
  private boolean throwWarning;

  /**
   * 是否自动提交
   */
  private boolean autoCommit;

  /**
   * 是否执行整个SQL
   */
  private boolean sendFullScript;

  /**
   * 移除回车 '\r'，但会保留换行 '\n'
   */
  private boolean removeCRs;

  /**
   * 使Jdbc驱动处理转义符
   */
  private boolean escapeProcessing = true;

  /**
   * 日志打印器
   */
  private PrintWriter logWriter = new PrintWriter(System.out);

  /**
   * 错误日志打印器
   */
  private PrintWriter errorLogWriter = new PrintWriter(System.err);

  /**
   * 分隔符
   */
  private String delimiter = DEFAULT_DELIMITER;

  /**
   * 是不是以行分割，有些脚本不换行
   */
  private boolean fullLineDelimiter;

  /**
   * 构造函数
   */
  public ScriptRunner(Connection connection) {
    this.connection = connection;
  }

  /*一些 get/set */

  public void setStopOnError(boolean stopOnError) {
    this.stopOnError = stopOnError;
  }

  public void setThrowWarning(boolean throwWarning) {
    this.throwWarning = throwWarning;
  }

  public void setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  public void setSendFullScript(boolean sendFullScript) {
    this.sendFullScript = sendFullScript;
  }

  public void setRemoveCRs(boolean removeCRs) {
    this.removeCRs = removeCRs;
  }

  /**
   * @since 3.1.1
   */
  public void setEscapeProcessing(boolean escapeProcessing) {
    this.escapeProcessing = escapeProcessing;
  }

  public void setLogWriter(PrintWriter logWriter) {
    this.logWriter = logWriter;
  }

  public void setErrorLogWriter(PrintWriter errorLogWriter) {
    this.errorLogWriter = errorLogWriter;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public void setFullLineDelimiter(boolean fullLineDelimiter) {
    this.fullLineDelimiter = fullLineDelimiter;
  }

  /**
   * 允许脚本
   */
  public void runScript(Reader reader) {
    setAutoCommit();

    try {
      if (sendFullScript) {
        executeFullScript(reader);
      } else {
        executeLineByLine(reader);
      }
    } finally {
      rollbackConnection();
    }
  }

  /**
   * 执行整个脚本语句
   */
  private void executeFullScript(Reader reader) {
    StringBuilder script = new StringBuilder();
    try {
      BufferedReader lineReader = new BufferedReader(reader);
      String line;
      while ((line = lineReader.readLine()) != null) {
        script.append(line);
        script.append(LINE_SEPARATOR);
      }
      String command = script.toString();
      println(command);
      executeStatement(command);
      commitConnection();
    } catch (Exception e) {
      String message = "Error executing: " + script + ".  Cause: " + e;
      printlnError(message);
      throw new RuntimeSqlException(message, e);
    }
  }

  /**
   * 将sql分割成一行一行去执行
   */
  private void executeLineByLine(Reader reader) {
    StringBuilder command = new StringBuilder();
    try {
      BufferedReader lineReader = new BufferedReader(reader);
      String line;
      while ((line = lineReader.readLine()) != null) {
        // 执行一行
        handleLine(command, line);
      }
      // 提交连接
      commitConnection();
      // 检查是不是漏了最后一行
      checkForMissingLineTerminator(command);
    } catch (Exception e) {
      String message = "Error executing: " + command + ".  Cause: " + e;
      printlnError(message);
      throw new RuntimeSqlException(message, e);
    }
  }

  /**
   * 关闭连接
   */
  public void closeConnection() {
    try {
      connection.close();
    } catch (Exception e) {
      // ignore
    }
  }

  /**
   * 是指自动提交属性
   */
  private void setAutoCommit() {
    try {
      if (autoCommit != connection.getAutoCommit()) {
        connection.setAutoCommit(autoCommit);
      }
    } catch (Throwable t) {
      throw new RuntimeSqlException("Could not set AutoCommit to " + autoCommit + ". Cause: " + t, t);
    }
  }

  /**
   * 提交连接
   */
  private void commitConnection() {
    try {
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    } catch (Throwable t) {
      throw new RuntimeSqlException("Could not commit transaction. Cause: " + t, t);
    }
  }

  /**
   * 回滚
   */
  private void rollbackConnection() {
    try {
      if (!connection.getAutoCommit()) {
        connection.rollback();
      }
    } catch (Throwable t) {
      // ignore
    }
  }

  /**
   * 检查是不是漏了行分隔符。因为完整的SQL执行完每一条后，都会被清空调command，没被清空就一种可能，一条SQL没以分隔符结尾
   */
  private void checkForMissingLineTerminator(StringBuilder command) {
    if (command != null && command.toString().trim().length() > 0) {
      throw new RuntimeSqlException("Line missing end-of-line terminator (" + delimiter + ") => " + command);
    }
  }

  /**
   * 执行一行语句
   *
   * @param line 别切割后的一行
   * @param command
   */
  private void handleLine(StringBuilder command, String line) throws SQLException {
    String trimmedLine = line.trim();
    // 注释的话直接GG
    if (lineIsComment(trimmedLine)) {
      Matcher matcher = DELIMITER_PATTERN.matcher(trimmedLine);
      if (matcher.find()) {
        // 啥玩意我也不懂
        delimiter = matcher.group(5);
      }
      println(trimmedLine);
    } else if (commandReadyToExecute(trimmedLine)) {
      // 拼接分隔符之前的内容，因为可能把注释写在尾部
      command.append(line, 0, line.lastIndexOf(delimiter));
      command.append(LINE_SEPARATOR);
      println(command);
      executeStatement(command.toString());
      // 执行完要清空 command
      command.setLength(0);
    } else if (trimmedLine.length() > 0) {
      // 针对语句太长的情况，即一条SQL被分成了多行
      command.append(line);
      command.append(LINE_SEPARATOR);
    }
  }

  /**
   * 判断该行命令是不是注释
   */
  private boolean lineIsComment(String trimmedLine) {
    return trimmedLine.startsWith("//") || trimmedLine.startsWith("--");
  }

  /**
   * 判断是不是要执行的命令, 判断的标准就是有分隔符
   */
  private boolean commandReadyToExecute(String trimmedLine) {
    // 很扯，可以简化成：trimmedLine.contains(delimiter)
    return !fullLineDelimiter && trimmedLine.contains(delimiter) || fullLineDelimiter && trimmedLine.equals(delimiter);
  }

  /**
   * 执行语句
   */
  private void executeStatement(String command) throws SQLException {
    Statement statement = connection.createStatement();
    try {
      statement.setEscapeProcessing(escapeProcessing);
      String sql = command;
      if (removeCRs) {
        sql = sql.replaceAll("\r\n", "\n");
      }
      try {
        boolean hasResults = statement.execute(sql);
        // 每个结果集撸一遍，实际不是存储过程不会多结果集
        while (!(!hasResults && statement.getUpdateCount() == -1)) {
          checkWarnings(statement);
          printResults(statement, hasResults);
          hasResults = statement.getMoreResults();
        }
      } catch (SQLWarning e) {
        throw e;
      } catch (SQLException e) {
        if (stopOnError) {
          throw e;
        } else {
          String message = "Error executing: " + command + ".  Cause: " + e;
          printlnError(message);
        }
      }
    } finally {
      try {
        statement.close();
      } catch (Exception e) {
        // Ignore to workaround a bug in some connection pools
        // (Does anyone know the details of the bug?)
      }
    }
  }

  /**
   * 检查是否有 warn 级别日志
   */
  private void checkWarnings(Statement statement) throws SQLException {
    if (!throwWarning) {
      return;
    }
    // Oracle 数据库，创建存储过程或者方法等，如果有编译错误，可能会返回 warning 代理 直接丢错
    SQLWarning warning = statement.getWarnings();
    if (warning != null) {
      throw warning;
    }
  }

  /**
   * 解析结果，打印
   */
  private void printResults(Statement statement, boolean hasResults) {
    if (!hasResults) {
      return;
    }
    try (ResultSet rs = statement.getResultSet()) {
      ResultSetMetaData md = rs.getMetaData();
      int cols = md.getColumnCount();
      for (int i = 0; i < cols; i++) {
        String name = md.getColumnLabel(i + 1);
        print(name + "\t");
      }
      println("");
      while (rs.next()) {
        for (int i = 0; i < cols; i++) {
          String value = rs.getString(i + 1);
          print(value + "\t");
        }
        println("");
      }
    } catch (SQLException e) {
      printlnError("Error printing results: " + e.getMessage());
    }
  }

  /**
   * 打印对象
   */
  private void print(Object o) {
    if (logWriter != null) {
      logWriter.print(o);
      logWriter.flush();
    }
  }

  /**
   * 换行打印
   */
  private void println(Object o) {
    if (logWriter != null) {
      logWriter.println(o);
      logWriter.flush();
    }
  }

  /**
   * 打印错误
   */
  private void printlnError(Object o) {
    if (errorLogWriter != null) {
      errorLogWriter.println(o);
      errorLogWriter.flush();
    }
  }

}
