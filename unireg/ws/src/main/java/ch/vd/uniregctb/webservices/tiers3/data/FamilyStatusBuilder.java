package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.webservices.tiers3.FamilyStatus;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class FamilyStatusBuilder {
	public static FamilyStatus newFamilyStatus(VueSituationFamille situation) {
		final FamilyStatus s = new FamilyStatus();
		s.setDateFrom(DataHelper.coreToWeb(situation.getDateDebut()));
		s.setDateTo(DataHelper.coreToWeb(situation.getDateFin()));
		s.setCancellationDate(DataHelper.coreToWeb(situation.getAnnulationDate()));
		s.setNumberOfChildren(situation.getNombreEnfants());

		if (situation instanceof ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun) {
			final ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun situtationMenage = (ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun) situation;

			s.setApplicableTariff(EnumHelper.coreToWeb(situtationMenage.getTarifApplicable()));
			final Long numeroContribuablePrincipal = situtationMenage.getNumeroContribuablePrincipal();
			s.setMainTaxpayerNumber(numeroContribuablePrincipal == null ? null : numeroContribuablePrincipal.intValue());
		}

		s.setMaritalStatus(EnumHelper.coreToWeb(situation.getEtatCivil()));
		return s;
	}
}
