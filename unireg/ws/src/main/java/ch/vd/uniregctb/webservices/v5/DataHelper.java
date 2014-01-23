package ch.vd.uniregctb.webservices.v5;

import java.util.ArrayList;
import java.util.List;

import ch.vd.unireg.xml.party.corporation.v3.CorporationEvent;
import ch.vd.uniregctb.interfaces.model.EvenementPM;

public abstract class DataHelper {

	public static List<CorporationEvent> coreToXML(List<EvenementPM> events) {
		final List<CorporationEvent> list = new ArrayList<>(events.size());
		for (EvenementPM event : events) {
			list.add(new CorporationEvent(event.getNumeroPM().intValue(), event.getCode(), ch.vd.uniregctb.xml.DataHelper.coreToXMLv2(event.getDate()), null));
		}
		return list;
	}
}
