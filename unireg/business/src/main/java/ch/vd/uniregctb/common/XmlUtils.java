package ch.vd.uniregctb.common;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class XmlUtils {

	/**
	 * Converti un document généré avec XmlBeans dans un document généré par JaxB.
	 *
	 * @param beanDoc un document généré avec XmlBeans
	 * @return le même document regénéré avex JaxB
	 * @throws ParserConfigurationException si ça foire
	 */
	public static Document xmlbeans2Jaxb(Document beanDoc) throws ParserConfigurationException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document jaxbDoc = builder.newDocument();
		Node newRoot = jaxbDoc.importNode(beanDoc.getDocumentElement(), true);
		jaxbDoc.appendChild(newRoot);
		return jaxbDoc;
	}

	/**
	 * Converti un object généré avec XmlBeans dans sa représentation String.
	 *
	 * @param object un object XMl généré avec XmlBeans
	 * @return la représentation String de l'object passé en paramètre.
	 * @throws IOException si ça foire
	 */
	public static String xmlbeans2string(XmlObject object) throws IOException {
		XmlOptions xmlOptions = new XmlOptions();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		object.save(out, xmlOptions);
		return out.toString();
	}

}
