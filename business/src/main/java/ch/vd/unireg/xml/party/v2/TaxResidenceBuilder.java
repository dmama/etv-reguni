package ch.vd.unireg.xml.party.v2;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;

public class TaxResidenceBuilder {
	public static TaxResidence newMainTaxResidence(ch.vd.unireg.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = newOtherTaxResidence(forFiscal, virtuel);

		if (forFiscal instanceof ch.vd.unireg.tiers.ForFiscalPrincipalPP) {
			final ch.vd.unireg.tiers.ForFiscalPrincipalPP forPrincipal = (ch.vd.unireg.tiers.ForFiscalPrincipalPP) forFiscal;
			f.setTaxationMethod(EnumHelper.coreToXMLv1(forPrincipal.getModeImposition()));
		}

		// [SIFISC-18334] Les version antérieures au v6 du WS ont toujours renvoyé IBC comme genre d'impôt pour les fors principaux PM
		if (forFiscal instanceof ch.vd.unireg.tiers.ForFiscalPrincipalPM) {
			f.setTaxType(TaxType.PROFITS_CAPITAL);
		}

		return f;
	}

	public static TaxResidence newOtherTaxResidence(ch.vd.unireg.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = new TaxResidence();

		f.setDateFrom(DataHelper.coreToXMLv1(forFiscal.getDateDebut()));
		f.setDateTo(DataHelper.coreToXMLv1(forFiscal.getDateFin()));
		f.setCancellationDate(DataHelper.coreToXMLv1(forFiscal.getAnnulationDate()));
		f.setTaxType(EnumHelper.coreToXMLv1(forFiscal.getGenreImpot()));
		f.setTaxationAuthorityType(EnumHelper.coreToXMLv1(forFiscal.getTypeAutoriteFiscale()));
		f.setTaxationAuthorityFSOId(forFiscal.getNumeroOfsAutoriteFiscale());
		f.setVirtual(virtuel);

		if (forFiscal instanceof ch.vd.unireg.tiers.ForFiscalRevenuFortune) {
			final ch.vd.unireg.tiers.ForFiscalRevenuFortune forRevenu = (ch.vd.unireg.tiers.ForFiscalRevenuFortune) forFiscal;
			f.setTaxLiabilityReason(EnumHelper.coreToXMLv1(forRevenu.getMotifRattachement()));
			f.setStartReason(EnumHelper.coreToXMLv1(forRevenu.getMotifOuverture()));
			f.setEndReason(EnumHelper.coreToXMLv1(forRevenu.getMotifFermeture()));
		}

		return f;
	}
}
