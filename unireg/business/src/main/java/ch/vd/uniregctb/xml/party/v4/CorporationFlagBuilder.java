package ch.vd.uniregctb.xml.party.v4;

import ch.vd.unireg.xml.party.corporation.v4.CorporationFlag;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class CorporationFlagBuilder {

	public static CorporationFlag newFlag(FlagEntreprise flag) {
		final CorporationFlag cf = new CorporationFlag();
		cf.setDateFrom(DataHelper.coreToXMLv2(flag.getDateDebut()));
		cf.setDateTo(DataHelper.coreToXMLv2(flag.getDateFin()));
		cf.setType(EnumHelper.coreToXMLv4(flag.getType()));
		return cf;
	}
}
