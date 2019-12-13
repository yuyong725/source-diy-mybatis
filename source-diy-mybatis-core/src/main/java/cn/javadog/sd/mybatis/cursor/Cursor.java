package cn.javadog.sd.mybatis.cursor;

import java.io.Closeable;

/**
 * @author: 余勇
 * @date: 2019-12-13 15:27
 *
 * 继承 Closeable、Iterable 接口，游标接口。
 * 这个类用于处理懒加载时，获取关联的列表，对应 @many或<association />。
 * 我们知道，对于N+1查询，即先查出n条结果(1次查询)，再对每一条结果去查关联的列表(n次查询)，性能会非常糟糕，这就是懒加载和Cursor的意义。这个方案
 * 完美的避免了海量的查询。
 * 其进行关联SQL查询，就是上面的n，是根据前面结果的resultMap的ID列，遍历的时候按此顺序，再去进行后面的关联查询操作
 *
 * resultOrdered：这个设置仅针对嵌套结果 select 语句适用；
 * 如果为 true，就是假设包含了嵌套结果集或是分组，这样的话当返回一个主结果行的时候，就不会发生有对前面结果集的引用的情况。
 * 这就使得在获取嵌套的结果集的时候不至于导致内存不够用。默认值：false。
 */
public interface Cursor<T> extends Closeable, Iterable<T> {

    /**
     * 是否处于打开状态，也就是是否开始遍历进行子查询
     */
    boolean isOpen();

    /**
     * 是否全部查询完成
     */
    boolean isConsumed();

    /**
     * 获得当前索引，从0开始
     * 如果已经开始遍历子查询，但第一条结果都还没有返回，那么此时返回-1。
     */
    int getCurrentIndex();

}
