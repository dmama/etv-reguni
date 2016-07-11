package ch.vd.uniregctb.webservices.v7;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.common.v2.PartialDate;

public abstract class DataHelper {

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
