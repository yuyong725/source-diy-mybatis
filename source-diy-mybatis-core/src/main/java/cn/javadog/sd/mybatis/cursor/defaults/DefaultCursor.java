package cn.javadog.sd.mybatis.cursor.defaults;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.resultset.DefaultResultSetHandler;
import cn.javadog.sd.mybatis.executor.resultset.ResultSetWrapper;
import cn.javadog.sd.mybatis.mapping.ResultMap;
import cn.javadog.sd.mybatis.session.ResultContext;
import cn.javadog.sd.mybatis.session.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;

/**
 * @author 余勇
 * @date 2019-12-13 15:50
 * 默认 Cursor 实现类，线程不安全
 */
public class DefaultCursor<T> implements Cursor<T> {

  /**
   * 默认的结果处理器
   */
  private final DefaultResultSetHandler resultSetHandler;

  /**
   * 结果对应的resultMap
   */
  private final ResultMap resultMap;

  /**
   * ResultSet 的包装类，通过操作结果集的工具
   */
  private final ResultSetWrapper rsw;

  /**
   * 分页参数
   */
  private final RowBounds rowBounds;

  /**
   * ObjectWrapperResultHandler 对象，当前类的内部类，包装 {@link ResultContext} 获取结果的过程和结果，每次遍历完都会将result清空
   */
  private final ObjectWrapperResultHandler<T> objectWrapperResultHandler = new ObjectWrapperResultHandler<>();

  /**
   * CursorIterator 对象，游标迭代器。也是内部类
   */
  private final CursorIterator cursorIterator = new CursorIterator();

  /**
   * 是否开始迭代
   */
  private boolean iteratorRetrieved;

  /**
   * 游标状态，默认值是 CREATED，也就是刚创建。也是内部枚举
   */
  private CursorStatus status = CursorStatus.CREATED;

  /**
   * 已完成映射的行数，默认 -1
   */
  private int indexWithRowBound = -1;

  /**
   * 游标状态枚举
   */
  private enum CursorStatus {

    /**
     * 刚刚创建，还没开始对数据库返回的结果进行解析
     */
    CREATED,

    /**
     * 已经开始解析结果
     */
    OPEN,

    /**
     * 已关闭，并未完全消费
     */
    CLOSED,

    /**
     * 已关闭，并且完全消费
     */
    CONSUMED
  }

  /**
   * DefaultCursor 的构造
   */
  public DefaultCursor(DefaultResultSetHandler resultSetHandler, ResultMap resultMap, ResultSetWrapper rsw, RowBounds rowBounds) {
    this.resultSetHandler = resultSetHandler;
    this.resultMap = resultMap;
    this.rsw = rsw;
    this.rowBounds = rowBounds;
  }

  /**
   * 是否已开始解析
   */
  @Override
  public boolean isOpen() {
    return status == CursorStatus.OPEN;
  }

  /**
   * 结果是否完全解析了并关闭了游标
   */
  @Override
  public boolean isConsumed() {
    return status == CursorStatus.CONSUMED;
  }

  /**
   * 获取当前解析的位置，包括偏移量
   */
  @Override
  public int getCurrentIndex() {
    // 分页的offset + 当前解析的位置(从0开始)
    return rowBounds.getOffset() + cursorIterator.iteratorIndex;
  }

  /**
   * 获取迭代器
   */
  @Override
  public Iterator<T> iterator() {
    // 如果已经获取，则抛出 IllegalStateException 异常
    if (iteratorRetrieved) {
      throw new IllegalStateException("Cannot open more than one iterator on a Cursor");
    }
    // 如果游标已经关了，不轮是否完全解析了结果，都报错
    if (isClosed()) {
      throw new IllegalStateException("A Cursor is already closed.");
    }
    // 标记游标已经获取，也就是说，该方法只能调用一次，一个游标只能拿一次
    iteratorRetrieved = true;
    return cursorIterator;
  }

  /**
   * 关闭游标
   */
  @Override
  public void close() {
    if (isClosed()) {
      // 已经关了直接返回，不会去报错
      return;
    }
    // 拿到 ResultSet
    ResultSet rs = rsw.getResultSet();
    try {
      if (rs != null) {
        // 拿到对应的 Statement
        Statement statement = rs.getStatement();
        // 首先关掉ResultSet
        rs.close();
        if (statement != null) {
          // 关掉 Statement
          statement.close();
        }
      }
      // 标记状态为关闭，是否完全解析完，由调用方决定
      status = CursorStatus.CLOSED;
    } catch (SQLException e) {
      // 出错不管
    }
  }

  /**
   * 遍历下一条记录。如果记录的下标小于分页的偏移量，会向后滑动，直到达到分页的偏移量的位置。
   * note 也说明了，mybatis提供的分页是对结果分页，而不是使用SQL的limit语句分页
   */
  protected T fetchNextUsingRowBound() {
    // 遍历下一条记录
    T result = fetchNextObjectFromDatabase();
    // 循环，直到游标跳过 rowBounds 的索引。游标不能直接跳，得一个一个的滑动，而且避免空指针，只有后面还有元素，才继续往下滑
    while (result != null && indexWithRowBound < rowBounds.getOffset()) {
      // 遍历下一条记录
      result = fetchNextObjectFromDatabase();
    }
    // 返回记录，有可能是null的，比如 分页的偏移量比结果集的总数还大
    return result;
  }

  /**
   * 遍历下一条记录
   */
  protected T fetchNextObjectFromDatabase() {
    // 如果已经关闭，返回 null
    if (isClosed()) {
      return null;
    }

    try {
      // 设置状态为 CursorStatus.OPEN
      status = CursorStatus.OPEN;
      // 遍历下一条记录
      if (!rsw.getResultSet().isClosed()) {
        // 使用默认的分页条件，也就是没有偏移量。处理完后，objectWrapperResultHandler 的 result 解析到的结果
        resultSetHandler.handleRowValues(rsw, resultMap, objectWrapperResultHandler, RowBounds.DEFAULT, null);
      }
    } catch (SQLException e) {
      // 遇到SQL异常要丢出
      throw new RuntimeException(e);
    }

    // 将resultSetHandler解析到的结果赋值给 next
    T next = objectWrapperResultHandler.result;
    // 下一条不为空的话，增加 indexWithRowBound
    if (next != null) {
      indexWithRowBound++;
    }
    // 没有更多记录，或者到达 rowBounds 的限制索引位置，则并设置状态为 CursorStatus.CONSUMED
    if (next == null || getReadItemsCount() == rowBounds.getOffset() + rowBounds.getLimit()) {
      // 关闭游标
      close();
      // 标记状态为 CursorStatus.CONSUMED，因为已经完全解析完了
      status = CursorStatus.CONSUMED;
    }
    // 置空 objectWrapperResultHandler.result 属性。可能是保险起见吧，objectWrapperResultHandler只能算是 result 的临时容器
    objectWrapperResultHandler.result = null;
    // 返回下一条结果
    return next;
  }

  /**
   * 判断是否已经关闭
   */
  private boolean isClosed() {
    return status == CursorStatus.CLOSED || status == CursorStatus.CONSUMED;
  }

  /**
   * 获取已经读取的数量，因为 indexWithRowBound 初始值为-1，每读取一条+1，因此实际读取的数量要比indexWithRowBound大1
   */
  private int getReadItemsCount() {
    return indexWithRowBound + 1;
  }

  /**
   * DefaultCursor 的内部静态类，实现 ResultHandler 接口，
   */
  private static class ObjectWrapperResultHandler<T> implements ResultHandler<T> {

    /**
     * 结果对象，记录 handleResult 处理后的结果
     */
    private T result;

    @Override
    public void handleResult(ResultContext<? extends T> context) {
      // 设置结果对象
      this.result = context.getResultObject();
      // 停止对结果对象的解析，或者说，标记 结果已经解析完了
      context.stop();
    }
  }

  /**
   * DefaultCursor 的内部类，实现 Iterator 接口，游标的迭代器实现类
   */
  private class CursorIterator implements Iterator<T> {

    /**
     * 记录游标拿到的对象，每次拿到返回后，都要置空，避免影响判断
     */
    T object;

    /**
     * 当前索引位置，从 -1 开始，拿到结果就 +1
     */
    int iteratorIndex = -1;

    /**
     * 是否有下一条记录
     */
    @Override
    public boolean hasNext() {
      // 如果 object 为空，则遍历下一条记录, 因为一开始 object 是空的，且每次调用 next() 也是会 置空 object 的
      if (object == null) {
        object = fetchNextUsingRowBound();
      }
      // 判断 object 是否非空
      return object != null;
    }

    @Override
    public T next() {
      // 将 object 赋值给 next，这个结果来自于👆的 hasNext()
      T next = object;
      // 如果 next 为空，则遍历下一条记录。这种场景出现在，直接调用 next()，而没有调用 hasNext() 进行判断
      if (next == null) {
        next = fetchNextUsingRowBound();
      }
      // 如果 next 非空，说明有记录，则进行返回
      if (next != null) {
        // 置空 object 对象
        object = null;
        // 增加 iteratorIndex
        iteratorIndex++;
        // 返回 next
        return next;
      }

      // 如果 next 为空，说明没有记录，抛出 NoSuchElementException 异常
      throw new NoSuchElementException();
    }

    /**
     * 不实现移除操作，调用直接GG
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException("Cannot remove element from Cursor");
    }
  }
}
