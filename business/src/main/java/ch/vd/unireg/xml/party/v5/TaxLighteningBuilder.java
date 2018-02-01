package ch.vd.uniregctb.xml.party.v5;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.corporation.v5.LighteningType;
import ch.vd.unireg.xml.party.corporation.v5.TaxLightening;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.AllegementFiscalCantonCommune;
import ch.vd.uniregctb.tiers.AllegementFiscalCommune;
import ch.vd.uniregctb.tiers.AllegementFiscalConfederation;
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
		tl.setTaxType(EnumHelper.coreToXMLv5(allegementFiscal.getTypeImpot()));
		tl.setTargetCollectivity(EnumHelper.coreToXMLv5(allegementFiscal.getTypeCollectivite(),
		                                                allegementFiscal instanceof AllegementFiscalCommune ? ((AllegementFiscalCommune) allegementFiscal).getNoOfsCommune() : null));
		tl.setLighteningType(getLighteningType(allegementFiscal));
		return tl;
	}

	@Nullable
	private static LighteningType getLighteningType(@NotNull AllegementFiscal allegement) {
		final LighteningType type;
		if (allegement instanceof AllegementFiscalConfederation) {
			AllegementFiscalConfederation ch = (AllegementFiscalConfederation) allegement;
			type = EnumHelper.coreToXMLv5(ch.getType());
		}
		else if (allegement instanceof AllegementFiscalCantonCommune) {
			AllegementFiscalCantonCommune vd =(AllegementFiscalCantonCommune) allegement;
			type = EnumHelper.coreToXMLv5(vd.getType());
		}
		else {
			throw new IllegalArgumentException("Type d'allègement inconnu = [" + allegement.getClass().getName() + "]");
		}
		return type;
	}

}
