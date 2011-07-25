package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class TaxResidenceBuilder {
	public static TaxResidence newMainTaxResidence(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = newOtherTaxResidence(forFiscal, virtuel);

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) {
			final ch.vd.uniregctb.tiers.ForFiscalRevenuFortune forRevenu = (ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) forFiscal;

			f.setTaxLiabilityReason(EnumHelper.coreToWeb(forRevenu.getMotifRattachement()));
			f.setStartReason(EnumHelper.coreToWeb(forRevenu.getMotifOuverture()));
			f.setEndReason(EnumHelper.coreToWeb(forRevenu.getMotifFermeture()));

			if (forRevenu instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipal) {
				final ch.vd.uniregctb.tiers.ForFiscalPrincipal forPrincipal = (ch.vd.uniregctb.tiers.ForFiscalPrincipal) forRevenu;
				f.setTaxationMethod(EnumHelper.coreToWeb(forPrincipal.getModeImposition()));
			}
		}
		return f;
	}

	public static TaxResidence newOtherTaxResidence(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = new TaxResidence();
		f.setDateFrom(DataHelper.coreToWeb(forFiscal.getDateDebut()));
		f.setDateTo(DataHelper.coreToWeb(forFiscal.getDateFin()));
		f.setCancellationDate(DataHelper.coreToWeb(forFiscal.getAnnulationDate()));
		f.setTaxType(EnumHelper.coreToWeb(forFiscal.getGenreImpot()));
		f.setTaxationAuthorityType(EnumHelper.coreToWeb(forFiscal.getTypeAutoriteFiscale()));
		f.setTaxationAuthorityFSOId(forFiscal.getNumeroOfsAutoriteFiscale());
		f.setVirtual(virtuel);
		return f;
	}
}
