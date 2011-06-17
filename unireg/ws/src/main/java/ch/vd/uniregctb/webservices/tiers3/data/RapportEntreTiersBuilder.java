package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.RapportEntreTiers;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class RapportEntreTiersBuilder {
	public static RapportEntreTiers newRapportEntreTiers(ch.vd.uniregctb.tiers.RapportEntreTiers rapport, Long autreTiersNumero) {
		final RapportEntreTiers r = new RapportEntreTiers();
		r.setType(EnumHelper.coreToWeb(rapport.getType()));
		r.setDateDebut(DataHelper.coreToWeb(rapport.getDateDebut()));
		r.setDateFin(DataHelper.coreToWeb(rapport.getDateFin()));
		r.setDateAnnulation(DataHelper.coreToWeb(rapport.getAnnulationDate()));
		r.setAutreTiersNumero(autreTiersNumero);

		if (rapport instanceof ch.vd.uniregctb.tiers.RapportPrestationImposable) {
			final ch.vd.uniregctb.tiers.RapportPrestationImposable rpi = (ch.vd.uniregctb.tiers.RapportPrestationImposable) rapport;

			r.setTypeActivite(EnumHelper.coreToWeb(rpi.getTypeActivite()));
			r.setTauxActivite(rpi.getTauxActivite());
			r.setFinDernierElementImposable(DataHelper.coreToWeb(rpi.getFinDernierElementImposable()));
		}

		// [UNIREG-2662] ajout de l'attribut extensionExecutionForcee
		if (rapport instanceof ch.vd.uniregctb.tiers.RepresentationConventionnelle) {
			final ch.vd.uniregctb.tiers.RepresentationConventionnelle repres = (ch.vd.uniregctb.tiers.RepresentationConventionnelle) rapport;
			r.setExtensionExecutionForcee(repres.getExtensionExecutionForcee());
		}
		return r;
	}
}
