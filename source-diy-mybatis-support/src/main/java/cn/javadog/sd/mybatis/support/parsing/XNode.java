package cn.javadog.sd.mybatis.support.parsing;

import java.util.Properties;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author 余勇
 * @date 2019年11月29日 16:58:00
 * xml的节点描述，是对xpath解析出来的node的封装
 */
public class XNode {

	/**
	 * xPathParser解析出来的节点
	 */
	private final Node node;

	/**
	 * 节点的name，都是w3c的标准，具体含义不追究
	 * todo 用处
	 */
	private final String name;

	/**
	 * 节点的body
	 */
	private final String body;
	private final Properties attributes;
	private final Properties variables;
	private final XPathParser xPathParser;

	public XNode(XPathParser xPathParser, Node node, Properties variables){
		this.xPathParser = xPathParser;
		this.node = node;
		this.name = node.getNodeName();
		this.variables = variables;
		this.attributes = parseAttributes(node);
		this.body = parseBody(node);
	}

	/**
	 * 创建新节点
	 */
	public XNode newXNode(Node node){
		return new XNode(xPathParser, node, variables);
	}

	public String getName() {
		return this.name;
	}

	/**
	 * 获取父节点
	 */
	public XNode getParent() {
		Node parent = node.getParentNode();
		if (parent == null || !(parent instanceof Element)){
			return null;
		}else {
			return new XNode(xPathParser, parent, variables);
		}
	}

	/**
	 * 基于标识符获取 属性
	 * 举例：如下这样的节点：
	 * <employee id="${id_var}">
 	 * 	<height units="ft">5.8</height>
	 * </employee>
	 * 首先获取 XNode node = parser.evalNode("/employee/height")，再调用此方法，返回的额值为：employee[${id_var}]_height
	 * todo 作用
	 */
	public String getvaluebasedIdentifier() {
		StringBuilder builder = new StringBuilder();
		XNode current = this;
		while (current != null){
			if (current != this){
				builder.insert(0, "-");
			}
			// 获取节点的'id'属性值，没有则取'value'属性值，依然没有就取'property'属性值，否则返回null
			String value = current.getStringAttribute("id",
				current.getStringAttribute("value",
					current.getStringAttribute("property", null)));
			if (value != null){
				value.replace('.','-');
				builder.insert(0,"]");
				builder.insert(0,value);
				builder.insert(0,"[");
			}
			builder.insert(0, current.getName());

			current = current.getParent();
		}
		return builder.toString();
	}

	/**
	 * 获取指定的属性，为空时使用默认值
	 */
	public String getStringAttribute(String name, String defaultValue) {
		// 注意，这里attributes不会为空，不需要非空判断
		return attributes.getProperty(name, defaultValue);
	}

	/**
	 * 解析node节点的属性
	 * 举例：如下面的xml，'year'、'month'、'day'就是 'birth_date'节点的属性
	 * <birth_date>
	 *     <year>1970</year>
	 *     <month>6</month>
	 *     <day>15</day>
	 * </birth_date>
	 */
	private Properties parseAttributes(Node n) {
		Properties properties = new Properties();
		NamedNodeMap namedNodeMap = n.getAttributes();
		if (namedNodeMap != null){
			for (int i = 0; i < namedNodeMap.getLength(); i++) {
				Node item = namedNodeMap.item(i);
				// todo 解析其中的占位符
				properties.put(item.getNodeName(), item.getNodeValue());
			}
		}
		return properties;
	}

	/**
	 * 获取节点的内容，对于多级节点，如<a><b>tt</b></a>, 会递归调用直到拿到非空值
	 * todo 具体使用的场景，单看逻辑简单，单对于标签之前的空字符串貌似有风险，等待测试！
	 */
	private String parseBody(Node node) {
		String data = getBodyData(node);
		if (data == null){
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				data = getBodyData(child);
				if (data != null){
					break;
				}
			}
		}
		return data;
	}

	/**
	 * 获取节点的body，节点 node 与 元素 element之间的关系不深究
	 * note 有意思的是node没有非空判断，说明框架逻辑能保证不为空，因此为空判断与否看设计的把控力
	 */
	private String getBodyData(Node child) {
		if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
			String data = ((CharacterData) child).getData();
			//todo 解析占位符
			return data;
		}
		return null;
	}


}
