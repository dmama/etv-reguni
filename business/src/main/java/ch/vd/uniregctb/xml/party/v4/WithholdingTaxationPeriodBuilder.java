package ch.vd.uniregctb.xml.party.v4;

import ch.vd.unireg.xml.party.taxresidence.v3.WithholdingTaxationPeriod;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class WithholdingTaxationPeriodBuilder {

	public static WithholdingTaxationPeriod newWithholdingTaxationPeriod(ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSource periode) {
		final WithholdingTaxationPeriod p = new WithholdingTaxationPeriod();
		p.setDateFrom(DataHelper.coreToXMLv2(periode.getDateDebut()));
		p.setDateTo(DataHelper.coreToXMLv2(periode.getDateFin()));
		p.setTaxationAuthority(EnumHelper.coreToXMLv3(periode.getTypeAutoriteFiscale()));
		p.setTaxationAuthorityFSOId(periode.getNoOfs());
		p.setType(EnumHelper.coreToXMLv3(periode.getType()));
		return p;
	}
}
