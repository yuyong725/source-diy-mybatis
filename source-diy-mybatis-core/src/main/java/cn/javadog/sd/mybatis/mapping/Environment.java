package cn.javadog.sd.mybatis.mapping;

import javax.sql.DataSource;

import cn.javadog.sd.mybatis.support.transaction.TransactionFactory;

/**
 * @author: 余勇
 * @date: 2019-12-13 20:33
 *
 * DB 环境，类似Spring的properties是dev，还是local，还是prod这种
 */
public final class Environment {

  /**
   * 环境编号，如dev、local
   */
  private final String id;

  /**
   * TransactionFactory 对象
   */
  private final TransactionFactory transactionFactory;

  /**
   * DataSource 对象
   */
  private final DataSource dataSource;

  /**
   * 构造函数。
   * 属性都不能为空，否则直接GG
   */
  public Environment(String id, TransactionFactory transactionFactory, DataSource dataSource) {
    if (id == null) {
      throw new IllegalArgumentException("Parameter 'id' must not be null");
    }
    if (transactionFactory == null) {
        throw new IllegalArgumentException("Parameter 'transactionFactory' must not be null");
    }
    if (dataSource == null) {
      throw new IllegalArgumentException("Parameter 'dataSource' must not be null");
    }
    this.id = id;
    this.transactionFactory = transactionFactory;
    this.dataSource = dataSource;
  }

  /**
   * 内部类，环境对象的构造器
   */
  public static class Builder {

    /**
     * 环境编号
     */
    private String id;

    /**
     * TransactionFactory 对象
     */
    private TransactionFactory transactionFactory;

    /**
     * DataSource 对象
     */
    private DataSource dataSource;

    /**
     * 构造器的构造函数
     */
    public Builder(String id) {
      this.id = id;
    }

    /**
     * 设置 transactionFactory
     */
    public Builder transactionFactory(TransactionFactory transactionFactory) {
      this.transactionFactory = transactionFactory;
      return this;
    }

    /**
     * 设置 dataSource
     */
    public Builder dataSource(DataSource dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    /**
     * 获取环境的ID
     */
    public String id() {
      return this.id;
    }

    /**
     * 执行构建。note 不然看到👆比如 {@link #dataSource(DataSource)}, {@link #transactionFactory(TransactionFactory)}
     *  都会链式返回this，它返回this是构造器对象，最终必须调用此方法才能构建真正的 Environment 对象
     */
    public Environment build() {
      return new Environment(this.id, this.transactionFactory, this.dataSource);
    }

  }

  /**
   * 获取环境ID
   */
  public String getId() {
    return this.id;
  }

  /**
   * 获取 transactionFactory
   */
  public TransactionFactory getTransactionFactory() {
    return this.transactionFactory;
  }

  /**
   * 获取 dataSource
   */
  public DataSource getDataSource() {
    return this.dataSource;
  }

}
