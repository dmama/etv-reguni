package ch.vd.uniregctb.webservices.batch;

import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;

/**
 * Couple clé-valeur représentant un paramètre nommé et sa valeur en représentation String.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ParamMapEntry", propOrder = {
		"key", "value", "bytesValue"
})
public class ParamMapEntry {

	@XmlElement(required = true)
	public String key;

	@XmlElement(required = false)
	public String value;

	/**
	 * Le contenu du fichier pour les paramètres de type fichier.
	 */
	@XmlElement(required = false)
	@XmlMimeType("application/octet-stream")
	public DataHandler bytesValue;

	public ParamMapEntry() {
	}

	public ParamMapEntry(Map.Entry<String, String> e) {
		key = e.getKey();
		value = e.getValue();
	}

	@Override
	public String toString() {
		return "ParamMapEntry{" +
				"key='" + key + '\'' +
				", value='" + value + '\'' +
				'}';
	}
}
