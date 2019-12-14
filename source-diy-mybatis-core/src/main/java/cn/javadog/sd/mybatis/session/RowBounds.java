package cn.javadog.sd.mybatis.session;

/**
 * @author 余勇
 * @date 2019-12-13 16:15
 *
 * 分页条件
 */
public class RowBounds {

  /**
   * 默认的偏移量 0
   */
  public static final int NO_ROW_OFFSET = 0;

  /**
   * 默认的数量限制，int的最大值
   */
  public static final int NO_ROW_LIMIT = Integer.MAX_VALUE;

  /**
   * 默认的 RowBounds，使用默认属性
   */
  public static final RowBounds DEFAULT = new RowBounds();

  /**
   * 偏移量，final不可修改
   */
  private final int offset;

  /**
   * 数量限制
   */
  private final int limit;

  /**
   * 默认构造，使用默认值
   */
  public RowBounds() {
    this.offset = NO_ROW_OFFSET;
    this.limit = NO_ROW_LIMIT;
  }

  /**
   * 自定义构造
   */
  public RowBounds(int offset, int limit) {
    this.offset = offset;
    this.limit = limit;
  }

  /**
   * 获取偏移量
   */
  public int getOffset() {
    return offset;
  }

  /**
   * 获取总是限制
   */
  public int getLimit() {
    return limit;
  }

}
