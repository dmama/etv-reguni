package ch.vd.uniregctb.webservices.batch;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.document.Document;

/**
 * Cette classe contient les informations d'un rapport d'exécution d'un batch Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 * @see http://www.java2s.com/Code/Java/Web-Services-SOA/ASOAPmessagewithanattachmentandXMLbinaryOptimizedPackaging.htm
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Report", propOrder = {
		"name", "description", "fileName", "fileExtension", "contentByteStream"
})
public class Report {

	/**
	 * Le nom du rapport.
	 */
	@XmlElement(required = true)
	public String name;

	/**
	 * Une description du rapport.
	 */
	@XmlElement(required = false)
	public String description;

	/**
	 * Le nom du fichier original du rapport.
	 */
	@XmlElement(required = true)
	public String fileName;

	/**
	 * Le type de rapport, représenté par son extension.
	 */
	@XmlElement(required = true)
	public String fileExtension;

	/**
	 * Le contenu binaire du fichier du rapport.
	 */
	@XmlMimeType("application/octet-stream")
	public DataHandler contentByteStream;

	public Report() {
	}

	public Report(Document document) {
		this.name = document.getNom();
		this.description = document.getDescription();
		this.fileName = document.getFileName();
		this.fileExtension = document.getFileExtension();
	}
}
