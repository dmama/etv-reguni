package ch.vd.uniregctb.xml.party.v4;

import ch.vd.unireg.xml.party.corporation.v4.TaxLightening;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCommune;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class TaxLighteningBuilder {

	public static TaxLightening newTaxLightening(AllegementFiscal allegementFiscal) {
		final TaxLightening tl = new TaxLightening();
		tl.setDateFrom(DataHelper.coreToXMLv2(allegementFiscal.getDateDebut()));
		tl.setDateTo(DataHelper.coreToXMLv2(allegementFiscal.getDateFin()));
		if (allegementFiscal.isAllegementMontant()) {
			tl.setAmountBased(new TaxLightening.AmountBased());
		}
		else {
			tl.setLighteningPercentage(allegementFiscal.getPourcentageAllegement());
		}
		tl.setTaxType(EnumHelper.coreToXMLv4(allegementFiscal.getTypeImpot()));
		tl.setTargetCollectivity(EnumHelper.coreToXMLv4(allegementFiscal.getTypeCollectivite(),
		                                                allegementFiscal instanceof AllegementFiscalCommune ? ((AllegementFiscalCommune) allegementFiscal).getNoOfsCommune() : null));
		return tl;
	}

}
