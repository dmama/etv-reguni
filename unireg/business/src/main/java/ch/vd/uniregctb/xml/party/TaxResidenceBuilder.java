package ch.vd.uniregctb.xml.party;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class TaxResidenceBuilder {
	public static TaxResidence newMainTaxResidence(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = newOtherTaxResidence(forFiscal, virtuel);

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipal) {
			final ch.vd.uniregctb.tiers.ForFiscalPrincipal forPrincipal = (ch.vd.uniregctb.tiers.ForFiscalPrincipal) forFiscal;
			f.setTaxationMethod(EnumHelper.coreToXML(forPrincipal.getModeImposition()));
		}
		return f;
	}

	public static TaxResidence newOtherTaxResidence(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = new TaxResidence();

		f.setDateFrom(DataHelper.coreToXML(forFiscal.getDateDebut()));
		f.setDateTo(DataHelper.coreToXML(forFiscal.getDateFin()));
		f.setCancellationDate(DataHelper.coreToXML(forFiscal.getAnnulationDate()));
		f.setTaxType(EnumHelper.coreToXML(forFiscal.getGenreImpot()));
		f.setTaxationAuthorityType(EnumHelper.coreToXML(forFiscal.getTypeAutoriteFiscale()));
		f.setTaxationAuthorityFSOId(forFiscal.getNumeroOfsAutoriteFiscale());
		f.setVirtual(virtuel);

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) {
			final ch.vd.uniregctb.tiers.ForFiscalRevenuFortune forRevenu = (ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) forFiscal;
			f.setTaxLiabilityReason(EnumHelper.coreToXML(forRevenu.getMotifRattachement()));
			f.setStartReason(EnumHelper.coreToXML(forRevenu.getMotifOuverture()));
			f.setEndReason(EnumHelper.coreToXML(forRevenu.getMotifFermeture()));
		}

		return f;
	}
}
