package cn.javadog.sd.mybatis.support.reflection.property;

import java.util.Iterator;

/**
 * @author 余勇
 * @date 2019年11月30日 22:39:00
 * 属性分词器，实现 Iterator 接口，支持迭代器的访问方式；这个类用处很多，尤其是对mapper.xml 文件的占位符读取的时候
 */
public class PropertyTokenizer  implements Iterator<PropertyTokenizer> {

	/**
	 * 当前字符串
	 */
	private String name;

	/**
	 * 索引的 {@link #name} ，因为 {@link #name} 如果存在 {@link #index} 会被更改
	 * 如 list[1].name indexedName 就是list[1]
	 */
	private final String indexedName;

	/**
	 * 编号。如 list[1].name index 就是 1
	 *
	 * 对于数组 name[0] ，则 index = 0
	 * 对于 Map map[key] ，则 index = key
	 */
	private String index;

	/**
	 * 剩余字符串
	 */
	private final String children;

	/**
	 * 构造函数，切割逻辑也在这里面
	 *
	 * list[1].age，最终 name=list, index=1, indexedName=list[1], children=age
	 * list[1][2] ，最终 name=list, index=1, indexedName=list[1][2], children=null
	 */
	public PropertyTokenizer(String fullName) {
		// 初始化 name、children 字符串，使用 . 作为分隔，如student.age
		int delim = fullName.indexOf('.');
		if (delim > -1) {
			// 取前面的部分，如 student.age 的student
			name = fullName.substring(0, delim);
			// 取后面的部分，如 age
			children = fullName.substring(delim + 1);
		} else {
			name = fullName;
			children = null;
		}
		// 记录当前 name
		indexedName = name;
		// 若存在 [ ，则获得 index ，并修改 name 。
		delim = name.indexOf('[');
		if (delim > -1) {
			index = name.substring(delim + 1, name.length() - 1);
			name = name.substring(0, delim);
		}
	}

	/**
	 * 几个get方法
	 */
	public String getName() {
		return name;
	}

	public String getIndex() {
		return index;
	}

	public String getIndexedName() {
		return indexedName;
	}

	public String getChildren() {
		return children;
	}

	/**
	 * 判断是否有下一个元素
	 */
	@Override
	public boolean hasNext() {
		return children != null;
	}

	/**
	 * 迭代获得下一个 PropertyTokenizer 对象
	 */
	@Override
	public PropertyTokenizer next() {
		return new PropertyTokenizer(children);
	}

	/**
	 * 删除元素，未实现，最直接抛错
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
	}
}

