package ch.vd.uniregctb.xml.party.v2;

import ch.vd.unireg.xml.party.taxresidence.v1.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v1.TaxType;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class TaxResidenceBuilder {
	public static TaxResidence newMainTaxResidence(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = newOtherTaxResidence(forFiscal, virtuel);

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipalPP) {
			final ch.vd.uniregctb.tiers.ForFiscalPrincipalPP forPrincipal = (ch.vd.uniregctb.tiers.ForFiscalPrincipalPP) forFiscal;
			f.setTaxationMethod(EnumHelper.coreToXMLv1(forPrincipal.getModeImposition()));
		}

		// [SIFISC-18334] Les version antérieures au v6 du WS ont toujours renvoyé IBC comme genre d'impôt pour les fors principaux PM
		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipalPM) {
			f.setTaxType(TaxType.PROFITS_CAPITAL);
		}

		return f;
	}

	public static TaxResidence newOtherTaxResidence(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = new TaxResidence();

		f.setDateFrom(DataHelper.coreToXMLv1(forFiscal.getDateDebut()));
		f.setDateTo(DataHelper.coreToXMLv1(forFiscal.getDateFin()));
		f.setCancellationDate(DataHelper.coreToXMLv1(forFiscal.getAnnulationDate()));
		f.setTaxType(EnumHelper.coreToXMLv1(forFiscal.getGenreImpot()));
		f.setTaxationAuthorityType(EnumHelper.coreToXMLv1(forFiscal.getTypeAutoriteFiscale()));
		f.setTaxationAuthorityFSOId(forFiscal.getNumeroOfsAutoriteFiscale());
		f.setVirtual(virtuel);

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) {
			final ch.vd.uniregctb.tiers.ForFiscalRevenuFortune forRevenu = (ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) forFiscal;
			f.setTaxLiabilityReason(EnumHelper.coreToXMLv1(forRevenu.getMotifRattachement()));
			f.setStartReason(EnumHelper.coreToXMLv1(forRevenu.getMotifOuverture()));
			f.setEndReason(EnumHelper.coreToXMLv1(forRevenu.getMotifFermeture()));
		}

		return f;
	}
}
