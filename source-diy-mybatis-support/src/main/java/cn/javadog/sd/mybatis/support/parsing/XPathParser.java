package cn.javadog.sd.mybatis.support.parsing;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cn.javadog.sd.mybatis.support.exceptions.ParsingException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author 余勇
 * @date 2019年11月29日 14:47:00
 * 基于Java Xpath的解析器，用于解析 mybatis-config.xml 和 **Mapper.xml 等 XML 配置文件
 * note:
 * 		1. Xpath 是jdk内置的解析xml的工具包，本类主要依靠`#xpath.evaluate`方法来实现解析功能
 * 		2. Document, String, Reader/Inputstream 是 资源读取类 的参数非常喜欢的几种形式，内部最终都是通过
 * 		 	string(路径) => Reader/Inputstream => InputSource => Document 的形式最终使用 Document 格式进行解析，
 * 		 	也因此一般都会有多种构造方法
 *		3. 由于解析的对象即可能是顶级的 Document，也可能是解析的某一个 Node 节点，因此 eval 方法都有两种形态。
 *		4. 由原生方法evalBoolean可以看出，即使 properties 有某个属性的值是boolean，如 'log.enable = true', 但解析<log.enable>${log.enable}</log
 *		.enable>是解析不出boolean类型的，占位符只对部分类型有效，这一点感觉相当不合理
 * todo:
 * 		1. 三种类型的构造函数，在什么场景下使用，给标记上
 * 		2. eval方法暂时只写几个类型用于测试，后续用到再补全
 */
public class XPathParser {

	/**
	 * XML Document 对象，xml文件的包装格式，xpath基于这种格式进行解析
	 */
	private final Document document;

	/**
	 * Properties 对象，用于替代xml中的占位符，如 mybatis-config.xml 中的 ${username}
	 */
	private Properties variables;

	/**
	 * Java XPath 对象，解析的底层实现交给它完成
	 */
	private XPath xPath;

	public XPathParser(String xml, Properties variables) {
		this.variables = variables;
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}

	public XPathParser(StringReader reader, Properties variables) {
		this.variables = variables;
		this.document = createDocument(new InputSource(reader));
	}

	public XPathParser(InputStream inputStream, Properties variables) {
		this.variables = variables;
		this.document = createDocument(new InputSource(inputStream));
	}

	/**
	 * 使用 string(路径)， Reader 转换的 InputSource 创建 Document 对象
	 * note
	 * 		1、核心是使用 DocumentBuilderFactory 相关API进行创建 Document
	 * 		2、factory大部分属性有默认值，这里不做过多解释，感兴趣的可以谷歌相关文档进行深入
	 */
	private Document createDocument(InputSource inputSource){
		try {
			// 创建DocumentBuilderFactory对象，并设置相关属性
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// 是否将工厂配置为生成解析器，该解析器在解析时验证 XML 内容，通俗的说就是 是否校验XML的格式，默认为false
			factory.setValidating(false);

			// 创建DocumentBuilder对象，由于我强制关闭了格式验证，因此这里不需要配置 builder.setEntityResolver()
			DocumentBuilder builder = factory.newDocumentBuilder();
			// 解析遇到任何问题都直接扔出去
			builder.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(SAXParseException exception) throws SAXException {
					throw  exception;
				}

				@Override
				public void error(SAXParseException exception) throws SAXException {
					throw  exception;
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					throw  exception;
				}
			});

			// 解析XML
			return builder.parse(inputSource);
		}catch (Exception e){
			throw new ParsingException("Error creating document instance, Cause: " + e, e);
		}
	}


	/**
	 * 获取指定元素或节点的值
	 *
	 * @param expression xpath语法解析表达式，如 '/employee/@id'
	 * @param root 要解析的对象，可能是顶级的 Document ，也可能是解析出的某一个 Node
	 * @param returnType 指定返回的类型，参见 {@link javax.xml.xpath.XPathConstants}
	 * @return 解析的结果，一般都会被强转成returnType
	 */
	private Object evaluate(String expression, Object root, QName returnType) {
		try {
			return xPath.evaluate(expression, root, returnType);
		} catch (Exception e) {
			throw new ParsingException("Error evaluating XPath. Cause: " + e, e);
		}
	}

	public String evalString(String expression) {
		return evalString(expression, document);
	}

	public String evalString(String expression, Object root) {
		// 获取解析的值
		String result = (String) evaluate(expression, root, XPathConstants.STRING);
		// todo 替换掉占位符

		return result;
	}

	public Boolean evalBoolean(String expression) {
		return evalBoolean(expression, document);
	}

	public Boolean evalBoolean(String expression, Object root) {
		return Boolean.valueOf(evalString(expression, root));
	}

	public Integer evalInteger(String expression) {
		return evalInteger(expression, document);
	}

	public Integer evalInteger(String expression, Object root) {
		return Integer.valueOf(evalString(expression, root));
	}

	public Short evalShort(String expression) {
		return evalShort(expression, document);
	}

	public Short evalShort(String expression, Object root) {
		return Short.valueOf(evalString(expression, root));
	}

	public Long evalLong(String expression) {
		return evalLong(expression, document);
	}

	public Long evalLong(String expression, Object root) {
		return Long.valueOf(evalString(expression, root));
	}

	public Double evalDouble(String expression) {
		return evalDouble(expression, document);
	}

	public Double evalDouble(String expression, Object root) {
		return Double.valueOf(evalString(expression, root));
	}

	public Float evalFloat(String expression) {
		return evalFloat(expression, document);
	}

	public Float evalFloat(String expression, Object root) {
		return Float.valueOf(evalString(expression, root));
	}

	public List<XNode> evalNodes(String expression){
		return evalNodes(expression, document);
	}

	public List<XNode> evalNodes(String expression, Object root){
		NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
		// 转成XNode数组，方便对XNode的解析
		List<XNode> xnodes = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			xnodes.add(new XNode(this, nodes.item(i), variables));
		}
		return xnodes;
	}

	public XNode evalNode(String expression){
		return evalNode(expression, document);
	}

	public XNode evalNode(String expression, Object root){
		Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
		return new XNode(this, node, variables);
	}
}
