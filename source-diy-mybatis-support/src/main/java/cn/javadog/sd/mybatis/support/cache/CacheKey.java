package cn.javadog.sd.mybatis.support.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cn.javadog.sd.mybatis.support.util.ArrayUtil;

/**
 * @author 余勇
 * @date 2019-12-04 21:24
 *
 * 实现 Cloneable、Serializable 接口，缓存键
 */
public class CacheKey implements Cloneable, Serializable {

  private static final long serialVersionUID = 1146682552656046210L;

  /**
   * 单例 - 空缓存键
   */
  public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();

  /**
   * 默认 {@link #multiplier} 的值
   */
  private static final int DEFAULT_MULTIPLYER = 37;

  /**
   * 默认 {@link #hashcode} 的值
   */
  private static final int DEFAULT_HASHCODE = 17;

  /**
   * hashcode 求值的系数
   */
  private final int multiplier;

  /**
   * 缓存键的 hashcode
   */
  private int hashcode;

  /**
   * 校验和
   */
  private long checksum;

  /**
   * {@link #update(Object)} 的数量
   */
  private int count;

  /**
   * 这个key对应的值的历史记录
   * Sonarlint(一个功能非常强大的代码质量检查、管理的工具)标记这个类应该声明为transient。
   * 如果说没有内容需要被序列化，这里确实应该标记为transient，但偶尔还是有内容需要被序列化的，因此这里没有标记transient
   *
   * TODO 上面翻译的不对！
   */
  private List<Object> updateList;

  /**
   * 构造函数
   */
  public CacheKey() {
    this.hashcode = DEFAULT_HASHCODE;
    this.multiplier = DEFAULT_MULTIPLYER;
    this.count = 0;
    this.updateList = new ArrayList<>();
  }

  /**
   * 构造， 这个objects就是👆{@link #updateList} 存的值
   */
  public CacheKey(Object[] objects) {
    this();
    // 基于 objects ，更新相关属性
    updateAll(objects);
  }

  /**
   * 值被更新的次数
   */
  public int getUpdateCount() {
    return updateList.size();
  }

  /**
   * 更新当前key对应的value
   * TODO 这么麻烦的计算hashcode干嘛，这个方法啥时候调用？只是updateAll？
   */
  public void update(Object object) {
    // 方法参数 object 的 hashcode
    int baseHashCode = object == null ? 1 : ArrayUtil.hashCode(object);

    count++;

    // checksum 为 baseHashCode 的求和
    checksum += baseHashCode;

    // 计算新的 hashcode 值
    baseHashCode *= count;
    hashcode = multiplier * hashcode + baseHashCode;

    // 添加 object 到 updateList 中
    updateList.add(object);
  }

  /**
   * 将objects刷新到updateList里面
   */
  public void updateAll(Object[] objects) {
    for (Object o : objects) {
      update(o);
    }
  }

  /**
   * 重写equal，比较赋值，感觉除非地址相同，很难有两个CacheKey相同
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof CacheKey)) {
      return false;
    }

    final CacheKey cacheKey = (CacheKey) object;

    if (hashcode != cacheKey.hashcode) {
      return false;
    }
    if (checksum != cacheKey.checksum) {
      return false;
    }
    if (count != cacheKey.count) {
      return false;
    }

    for (int i = 0; i < updateList.size(); i++) {
      Object thisObject = updateList.get(i);
      Object thatObject = cacheKey.updateList.get(i);
      if (!ArrayUtil.equals(thisObject, thatObject)) {
        return false;
      }
    }
    return true;
  }

  /**
   * 返回hashcode
   */
  @Override
  public int hashCode() {
    return hashcode;
  }

  /**
   * 重写tostring
   */
  @Override
  public String toString() {
    StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
    for (Object object : updateList) {
      returnValue.append(':').append(ArrayUtil.toString(object));
    }
    return returnValue.toString();
  }

  /**
   * 重写
   * TODO 感觉写的那么麻烦，必有大用
   */
  @Override
  public CacheKey clone() throws CloneNotSupportedException {
    // 克隆 CacheKey 对象
    CacheKey clonedCacheKey = (CacheKey) super.clone();
    // 创建 updateList 数组，避免原数组修改
    clonedCacheKey.updateList = new ArrayList<>(updateList);
    return clonedCacheKey;
  }

}
