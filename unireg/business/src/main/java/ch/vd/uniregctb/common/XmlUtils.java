package ch.vd.uniregctb.common;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ch.vd.registre.base.date.RegDate;

public abstract class XmlUtils {

	private static DatatypeFactory datatypeFactory;

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

	public static RegDate xmlcal2regdate(XMLGregorianCalendar cal) {
		if (cal == null) {
			return null;
		}
		final int year = cal.getYear();
		final int month = cal.getMonth();
		final int day = cal.getDay();
		if (month == DatatypeConstants.FIELD_UNDEFINED) {
			return RegDate.get(year);
		}
		else if (day == DatatypeConstants.FIELD_UNDEFINED) {
			return RegDate.get(year, month);
		}
		else {
			return RegDate.get(year, month, day);
		}
	}

	public static Date xmlcal2date(XMLGregorianCalendar cal) {
		if (cal == null) {
			return null;
		}
		return cal.toGregorianCalendar().getTime();
	}

	public static XMLGregorianCalendar regdate2xmlcal(RegDate date) {
		if (date == null) {
			return null;
		}
		final int year = date.year();
		final int month = (date.month() == RegDate.UNDEFINED ? DatatypeConstants.FIELD_UNDEFINED : date.month());
		final int day = (date.day() == RegDate.UNDEFINED ? DatatypeConstants.FIELD_UNDEFINED : date.day());
		return getDataTypeFactory().newXMLGregorianCalendar(year, month, day, 0, 0, 0, 0, DatatypeConstants.FIELD_UNDEFINED);
	}

	private static DatatypeFactory getDataTypeFactory() {
		if (datatypeFactory == null) {
			try {
				datatypeFactory = DatatypeFactory.newInstance();
			}
			catch (DatatypeConfigurationException e) {
				throw new RuntimeException(e);
			}
		}
		return datatypeFactory;
	}
}
