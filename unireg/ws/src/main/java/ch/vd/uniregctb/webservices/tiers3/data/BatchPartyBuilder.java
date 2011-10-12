package ch.vd.uniregctb.webservices.tiers3.data;

import java.util.Map;

import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.BatchPartyEntry;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.party.v1.Party;

public class BatchPartyBuilder {
	public static BatchParty newBatchParty(Map<Long, Object> map) {
		final BatchParty b = new BatchParty();
		if (map != null) {
			for (Map.Entry<Long, Object> e : map.entrySet()) {
				final Long id = e.getKey();
				if (id != null) {
					b.getEntries().add(newBatchPartyEntry(id.intValue(), e.getValue()));
				}
			}
		}
		return b;
	}

	public static BatchPartyEntry newBatchPartyEntry(int numero, Object value) {
		final BatchPartyEntry b = new BatchPartyEntry();
		b.setNumber(numero);
		if (value == null) {
			b.setParty(null);
			b.setExceptionInfo(null);
		}
		else if (value instanceof Party) {
			b.setParty((Party) value);
			b.setExceptionInfo(null);
		}
		else if (value instanceof WebServiceException) {
			b.setParty(null);
			b.setExceptionInfo(((WebServiceException) value).getFaultInfo());
		}
		else {
			throw new IllegalArgumentException("Type de valeur inconnu = [" + value + "]");
		}
		return b;
	}
}
