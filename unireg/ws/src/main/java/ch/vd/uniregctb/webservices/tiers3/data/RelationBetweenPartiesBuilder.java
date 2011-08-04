package ch.vd.uniregctb.webservices.tiers3.data;

import ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.EnumHelper;

public class RelationBetweenPartiesBuilder {
	public static RelationBetweenParties newRelationBetweenParties(ch.vd.uniregctb.tiers.RapportEntreTiers rapport, Long autreTiersNumero) {
		final RelationBetweenParties r = new RelationBetweenParties();
		r.setType(EnumHelper.coreToWeb(rapport.getType()));
		r.setDateFrom(DataHelper.coreToWeb(rapport.getDateDebut()));
		r.setDateTo(DataHelper.coreToWeb(rapport.getDateFin()));
		r.setCancellationDate(DataHelper.coreToWeb(rapport.getAnnulationDate()));
		r.setOtherPartyNumber(autreTiersNumero.intValue());

		if (rapport instanceof ch.vd.uniregctb.tiers.RapportPrestationImposable) {
			final ch.vd.uniregctb.tiers.RapportPrestationImposable rpi = (ch.vd.uniregctb.tiers.RapportPrestationImposable) rapport;

			r.setActivityType(EnumHelper.coreToWeb(rpi.getTypeActivite()));
			r.setActivityRate(rpi.getTauxActivite());
			r.setEndDateOfLastTaxableItem(DataHelper.coreToWeb(rpi.getFinDernierElementImposable()));
		}

		// [UNIREG-2662] ajout de l'attribut extensionExecutionForcee
		if (rapport instanceof ch.vd.uniregctb.tiers.RepresentationConventionnelle) {
			final ch.vd.uniregctb.tiers.RepresentationConventionnelle repres = (ch.vd.uniregctb.tiers.RepresentationConventionnelle) rapport;
			r.setExtensionToForcedExecution(repres.getExtensionExecutionForcee());
		}
		return r;
	}
}
