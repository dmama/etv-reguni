package ch.vd.uniregctb.xml.party.v5;

import ch.vd.unireg.xml.party.taxresidence.v4.WithholdingTaxationPeriod;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class WithholdingTaxationPeriodBuilder {

	public static WithholdingTaxationPeriod newWithholdingTaxationPeriod(ch.vd.uniregctb.metier.piis.PeriodeImpositionImpotSource periode) {
		final WithholdingTaxationPeriod p = new WithholdingTaxationPeriod();
		p.setDateFrom(DataHelper.coreToXMLv2(periode.getDateDebut()));
		p.setDateTo(DataHelper.coreToXMLv2(periode.getDateFin()));
		p.setTaxationAuthority(EnumHelper.coreToXMLv4(periode.getTypeAutoriteFiscale()));
		p.setTaxationAuthorityFSOId(periode.getNoOfs());
		p.setType(EnumHelper.coreToXMLv4(periode.getType()));
		return p;
	}
}
