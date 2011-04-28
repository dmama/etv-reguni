package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.webservices.tiers2.TiersWebService;

/**
 * Classe contenant tous les résultats retournés par un appel à la méthode {@link TiersWebService#getBatchTiers(ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers)}.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BatchTiers", propOrder = {
		"entries"
})
public class BatchTiers {

	@XmlElement(required = true)
	public final List<BatchTiersEntry> entries = new ArrayList<BatchTiersEntry>();

	public BatchTiers(BatchTiersEntry unique) {
		entries.add(unique);
	}

	public BatchTiers() {
	}

	public BatchTiers(Map<Long, Object> map) {
		if (map != null) {
			for (Map.Entry<Long, Object> e : map.entrySet()) {
				entries.add(new BatchTiersEntry(e.getKey(), e.getValue()));
			}
		}
	}

	public boolean isEmpty() {
		return entries == null || entries.isEmpty();
	}
}
