package ch.vd.uniregctb.webservices.v5;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.common.v2.PartialDate;
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

	public static Date coreToWeb(java.util.Date date) {
		return ch.vd.uniregctb.xml.DataHelper.coreToXMLv2(date);
	}

	public static Date coreToWeb(RegDate date) {
		return ch.vd.uniregctb.xml.DataHelper.coreToXMLv2(date);
	}

	public static RegDate webToRegDate(Date date) {
		return ch.vd.uniregctb.xml.DataHelper.xmlToCore(date);
	}

	public static RegDate webToRegDate(PartialDate date) {
		return ch.vd.uniregctb.xml.DataHelper.xmlToCore(date);
	}
}
