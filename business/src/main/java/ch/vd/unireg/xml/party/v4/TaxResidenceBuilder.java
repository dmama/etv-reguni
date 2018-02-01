package ch.vd.unireg.xml.party.v4;

import ch.vd.unireg.xml.party.taxresidence.v3.TaxResidence;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;

public class TaxResidenceBuilder {
	public static TaxResidence newMainTaxResidence(ch.vd.unireg.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = newOtherTaxResidence(forFiscal, virtuel);

		if (forFiscal instanceof ch.vd.unireg.tiers.ForFiscalPrincipalPP) {
			final ch.vd.unireg.tiers.ForFiscalPrincipalPP forPrincipal = (ch.vd.unireg.tiers.ForFiscalPrincipalPP) forFiscal;
			f.setTaxationMethod(EnumHelper.coreToXMLv3(forPrincipal.getModeImposition()));
		}
		return f;
	}

	public static TaxResidence newOtherTaxResidence(ch.vd.unireg.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = new TaxResidence();

		f.setDateFrom(DataHelper.coreToXMLv2(forFiscal.getDateDebut()));
		f.setDateTo(DataHelper.coreToXMLv2(forFiscal.getDateFin()));
		f.setCancellationDate(DataHelper.coreToXMLv2(forFiscal.getAnnulationDate()));
		f.setTaxType(EnumHelper.coreToXMLv3(forFiscal.getGenreImpot()));
		f.setTaxationAuthorityType(EnumHelper.coreToXMLv3(forFiscal.getTypeAutoriteFiscale()));
		f.setTaxationAuthorityFSOId(forFiscal.getNumeroOfsAutoriteFiscale());
		f.setVirtual(virtuel);

		if (forFiscal instanceof ch.vd.unireg.tiers.ForFiscalAvecMotifs) {
			final ch.vd.unireg.tiers.ForFiscalAvecMotifs forMotifs = (ch.vd.unireg.tiers.ForFiscalAvecMotifs) forFiscal;
			f.setStartReason(EnumHelper.coreToXMLv3(forMotifs.getMotifOuverture()));
			f.setEndReason(EnumHelper.coreToXMLv3(forMotifs.getMotifFermeture()));
		}

		if (forFiscal instanceof ch.vd.unireg.tiers.ForFiscalRevenuFortune) {
			final ch.vd.unireg.tiers.ForFiscalRevenuFortune forRevenu = (ch.vd.unireg.tiers.ForFiscalRevenuFortune) forFiscal;
			f.setTaxLiabilityReason(EnumHelper.coreToXMLv3(forRevenu.getMotifRattachement()));
		}
		return f;
	}
}
