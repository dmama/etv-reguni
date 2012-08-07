package ch.vd.uniregctb.xml.party;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportFiliation;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class RelationBetweenPartiesBuilder {
	public static RelationBetweenParties newRelationBetweenParties(ch.vd.uniregctb.tiers.RapportEntreTiers rapport, Long autreTiersNumero) {
		final RelationBetweenParties r = new RelationBetweenParties();
		r.setType(EnumHelper.coreToXML(rapport.getType()));
		r.setDateFrom(DataHelper.coreToXML(rapport.getDateDebut()));
		r.setDateTo(DataHelper.coreToXML(rapport.getDateFin()));
		r.setCancellationDate(DataHelper.coreToXML(rapport.getAnnulationDate()));
		r.setOtherPartyNumber(autreTiersNumero.intValue());

		if (rapport instanceof ch.vd.uniregctb.tiers.RapportPrestationImposable) {
			final ch.vd.uniregctb.tiers.RapportPrestationImposable rpi = (ch.vd.uniregctb.tiers.RapportPrestationImposable) rapport;

			r.setActivityType(EnumHelper.coreToXML(rpi.getTypeActivite()));
			r.setActivityRate(rpi.getTauxActivite());
			r.setEndDateOfLastTaxableItem(DataHelper.coreToXML(rpi.getFinDernierElementImposable()));
		}

		// [UNIREG-2662] ajout de l'attribut extensionExecutionForcee
		if (rapport instanceof ch.vd.uniregctb.tiers.RepresentationConventionnelle) {
			final ch.vd.uniregctb.tiers.RepresentationConventionnelle repres = (ch.vd.uniregctb.tiers.RepresentationConventionnelle) rapport;
			r.setExtensionToForcedExecution(repres.getExtensionExecutionForcee());
		}
		return r;
	}

	/**
	 * @param filiation un rapport de filiation
	 * @return un objet {@link RelationBetweenParties}, ou <code>null</code> si l'une des personnes physiques du rapport de filiation est inconnue
	 */
	@Nullable
	public static RelationBetweenParties newFiliation(RapportFiliation filiation) {
		final RelationBetweenParties r;
		final PersonnePhysique autrePersonnePhysique = filiation.getAutrePersonnePhysique();
		if (autrePersonnePhysique != null) {
			r = new RelationBetweenParties();
			r.setType(filiation.getType() == RapportFiliation.Type.ENFANT ? RelationBetweenPartiesType.CHILD : RelationBetweenPartiesType.PARENT);
			r.setDateFrom(DataHelper.coreToXML(filiation.getDateDebut()));
			r.setDateTo(DataHelper.coreToXML(filiation.getDateFin()));
			r.setOtherPartyNumber(autrePersonnePhysique.getNumero().intValue());
		}
		else {
			r = null;
		}
		return r;
	}
}
