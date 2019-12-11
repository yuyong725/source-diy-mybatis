package cn.javadog.sd.mybatis.builder;

/**
 * Interface that indicate to provide a initialization method.
 *
 * @since 3.4.2
 * @author Kazuki Shimizu
 */
public interface InitializingObject {

  /**
   * Initialize a instance.
   * <p>
   * This method will be invoked after it has set all properties.
   * </p>
   * @throws Exception in the event of misconfiguration (such as failure to set an essential property) or if initialization fails
   */
  void initialize() throws Exception;

}