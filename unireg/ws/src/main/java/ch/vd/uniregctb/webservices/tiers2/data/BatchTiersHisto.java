package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Classe contenant tous les résultats retournés par un appel à la méthode {@link ch.vd.uniregctb.webservices.tiers2.TiersWebService#getBatchTiersHisto(ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto)}.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BatchTiersHisto", propOrder = {
		"entries"
})
public class BatchTiersHisto {

	@XmlElement(required = true)
	public List<BatchTiersHistoEntry> entries = new ArrayList<BatchTiersHistoEntry>();

	public BatchTiersHisto() {
	}

	public BatchTiersHisto(BatchTiersHistoEntry unique) {
		entries.add(unique);
	}

	public BatchTiersHisto(Map<Long, Object> map) {
		if (map != null) {
			for (Map.Entry<Long, Object> e : map.entrySet()) {
				entries.add(new BatchTiersHistoEntry(e.getKey(), e.getValue()));
			}
		}
	}

	public boolean isEmpty() {
		return entries == null || entries.isEmpty();
	}
}