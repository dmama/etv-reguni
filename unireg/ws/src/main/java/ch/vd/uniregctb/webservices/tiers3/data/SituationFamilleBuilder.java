package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.SituationFamille;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class SituationFamilleBuilder {
	public static SituationFamille newSituationFamille(VueSituationFamille situation) {
		final SituationFamille s = new SituationFamille();
		s.setDateDebut(DataHelper.coreToWeb(situation.getDateDebut()));
		s.setDateFin(DataHelper.coreToWeb(situation.getDateFin()));
		s.setDateAnnulation(DataHelper.coreToWeb(situation.getAnnulationDate()));
		s.setNombreEnfants(situation.getNombreEnfants());

		if (situation instanceof ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun) {
			final ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun situtationMenage = (ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun) situation;

			s.setTarifApplicable(EnumHelper.coreToWeb(situtationMenage.getTarifApplicable()));
			s.setNumeroContribuablePrincipal(situtationMenage.getNumeroContribuablePrincipal());
		}

		s.setEtatCivil(EnumHelper.coreToWeb(situation.getEtatCivil()));
		return s;
	}
}
