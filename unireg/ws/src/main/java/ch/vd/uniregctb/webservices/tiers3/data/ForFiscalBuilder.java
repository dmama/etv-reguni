package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.uniregctb.webservices.tiers3.ForFiscal;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class ForFiscalBuilder {
	public static ForFiscal newForFiscalPrincipal(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final ForFiscal f = newForFiscal(forFiscal, virtuel);

		if (forFiscal instanceof ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) {
			final ch.vd.uniregctb.tiers.ForFiscalRevenuFortune forRevenu = (ch.vd.uniregctb.tiers.ForFiscalRevenuFortune) forFiscal;

			f.setMotifRattachement(EnumHelper.coreToWeb(forRevenu.getMotifRattachement()));
			f.setMotifDebut(EnumHelper.coreToWeb(forRevenu.getMotifOuverture()));
			f.setMotifFin(EnumHelper.coreToWeb(forRevenu.getMotifFermeture()));

			if (forRevenu instanceof ch.vd.uniregctb.tiers.ForFiscalPrincipal) {
				final ch.vd.uniregctb.tiers.ForFiscalPrincipal forPrincipal = (ch.vd.uniregctb.tiers.ForFiscalPrincipal) forRevenu;
				f.setModeImposition(EnumHelper.coreToWeb(forPrincipal.getModeImposition()));
			}
		}
		return f;
	}

	public static ForFiscal newForFiscal(ch.vd.uniregctb.tiers.ForFiscal forFiscal, boolean virtuel) {
		final ForFiscal f = new ForFiscal();
		f.setDateDebut(DataHelper.coreToWeb(forFiscal.getDateDebut()));
		f.setDateFin(DataHelper.coreToWeb(forFiscal.getDateFin()));
		f.setDateAnnulation(DataHelper.coreToWeb(forFiscal.getAnnulationDate()));
		f.setGenreImpot(EnumHelper.coreToWeb(forFiscal.getGenreImpot()));
		f.setTypeAutoriteFiscale(EnumHelper.coreToWeb(forFiscal.getTypeAutoriteFiscale()));
		f.setNoOfsAutoriteFiscale(forFiscal.getNumeroOfsAutoriteFiscale());
		f.setVirtuel(virtuel);
		return f;
	}
}
