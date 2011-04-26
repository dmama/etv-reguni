package ch.vd.uniregctb.webservices.batch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Map des paramètres de démarrage d'un batch Unireg.
 * <p>
 * Les valeurs sont représentés sous le format String.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ParamMap", propOrder = {
	"entries"
})
public class ParamMap {

	@XmlElement(required = true)
	public List<ParamMapEntry> entries = new ArrayList<ParamMapEntry>();

	public ParamMap() {
	}

	public ParamMap(Map<String, String> map) {
		for (Map.Entry<String, String> e : map.entrySet()) {
			entries.add(new ParamMapEntry(e));
		}
	}

	public boolean isEmpty() {
		return entries == null || entries.isEmpty();
	}

	@Override
	public String toString() {
		return "ParamMap{" +
				"entries=" + Arrays.toString(entries.toArray()) +
				'}';
	}
}
