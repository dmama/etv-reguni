package ch.vd.uniregctb.xml.party.v3;

import ch.vd.unireg.xml.party.taxresidence.v2.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxType;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class TaxResidenceBuilder {
	public static TaxResidence newMainTaxResidence(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = newOtherTaxResidence(forFiscal, virtuel);

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipalPP) {
			final ch.vd.uniregctb.tiers.ForFiscalPrincipalPP forPrincipal = (ch.vd.uniregctb.tiers.ForFiscalPrincipalPP) forFiscal;
			f.setTaxationMethod(EnumHelper.coreToXMLv2(forPrincipal.getModeImposition()));
		}

		// [SIFISC-18334] Les version antérieures au v6 du WS ont toujours renvoyé IBC comme genre d'impôt pour les fors principaux PM
		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipalPM) {
			f.setTaxType(TaxType.PROFITS_CAPITAL);
		}

		return f;
	}

	public static TaxResidence newOtherTaxResidence(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final TaxResidence f = new TaxResidence();

		f.setDateFrom(DataHelper.coreToXMLv2(forFiscal.getDateDebut()));
		f.setDateTo(DataHelper.coreToXMLv2(forFiscal.getDateFin()));
		f.setCancellationDate(DataHelper.coreToXMLv2(forFiscal.getAnnulationDate()));
		f.setTaxType(EnumHelper.coreToXMLv2(forFiscal.getGenreImpot()));
		f.setTaxationAuthorityType(EnumHelper.coreToXMLv2(forFiscal.getTypeAutoriteFiscale()));
		f.setTaxationAuthorityFSOId(forFiscal.getNumeroOfsAutoriteFiscale());
		f.setVirtual(virtuel);

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalAvecMotifs) {
			final ch.vd.uniregctb.tiers.ForFiscalAvecMotifs forMotifs = (ch.vd.uniregctb.tiers.ForFiscalAvecMotifs) forFiscal;
			f.setStartReason(EnumHelper.coreToXMLv2(forMotifs.getMotifOuverture()));
			f.setEndReason(EnumHelper.coreToXMLv2(forMotifs.getMotifFermeture()));
		}

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) {
			final ch.vd.uniregctb.tiers.ForFiscalRevenuFortune forRevenu = (ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) forFiscal;
			f.setTaxLiabilityReason(EnumHelper.coreToXMLv2(forRevenu.getMotifRattachement()));
		}
		return f;
	}
}
