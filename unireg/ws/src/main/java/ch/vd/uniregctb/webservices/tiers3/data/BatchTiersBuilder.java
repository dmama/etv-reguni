package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.Map;

import ch.vd.unireg.webservices.tiers3.BatchTiers;
import ch.vd.unireg.webservices.tiers3.BatchTiersEntry;
import ch.vd.unireg.webservices.tiers3.Tiers;
import ch.vd.unireg.webservices.tiers3.WebServiceException;

public class BatchTiersBuilder {
	public static BatchTiers newBatchTiers(Map<Long, Object> map) {
		final BatchTiers b = new BatchTiers();
		if (map != null) {
			for (Map.Entry<Long, Object> e : map.entrySet()) {
				final Long id = e.getKey();
				if (id != null) {
					b.getEntries().add(newBatchTiersEntry(id, e.getValue()));
				}
			}
		}
		return b;
	}

	public static BatchTiersEntry newBatchTiersEntry(long numero, Object value) {
		final BatchTiersEntry b = new BatchTiersEntry();
		b.setNumber(numero);
		if (value == null) {
			b.setTiers(null);
			b.setExceptionInfo(null);
		}
		else if (value instanceof Tiers) {
			b.setTiers((Tiers) value);
			b.setExceptionInfo(null);
		}
		else if (value instanceof WebServiceException) {
			b.setTiers(null);
			b.setExceptionInfo(((WebServiceException) value).getFaultInfo());
		}
		else {
			throw new IllegalArgumentException("Type de valeur inconnu = [" + value + "]");
		}
		return b;
	}
}
