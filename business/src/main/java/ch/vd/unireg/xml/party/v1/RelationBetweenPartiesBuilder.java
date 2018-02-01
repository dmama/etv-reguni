package ch.vd.unireg.xml.party.v1;

import ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.unireg.tiers.Parente;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;

public class RelationBetweenPartiesBuilder {

	public static RelationBetweenParties newRelationBetweenParties(ch.vd.unireg.tiers.RapportEntreTiers rapport, Long autreTiersNumero) {
		final RelationBetweenParties r = new RelationBetweenParties();
		r.setType(EnumHelper.coreToXMLv1(rapport.getType()));
		r.setDateFrom(DataHelper.coreToXMLv1(rapport.getDateDebut()));
		r.setDateTo(DataHelper.coreToXMLv1(rapport.getDateFin()));
		r.setCancellationDate(DataHelper.coreToXMLv1(rapport.getAnnulationDate()));
		r.setOtherPartyNumber(autreTiersNumero.intValue());

		if (rapport instanceof ch.vd.unireg.tiers.RapportPrestationImposable) {
			final ch.vd.unireg.tiers.RapportPrestationImposable rpi = (ch.vd.unireg.tiers.RapportPrestationImposable) rapport;

			r.setEndDateOfLastTaxableItem(DataHelper.coreToXMLv1(rpi.getFinDernierElementImposable()));
		}

		// [UNIREG-2662] ajout de l'attribut extensionExecutionForcee
		if (rapport instanceof ch.vd.unireg.tiers.RepresentationConventionnelle) {
			final ch.vd.unireg.tiers.RepresentationConventionnelle repres = (ch.vd.unireg.tiers.RepresentationConventionnelle) rapport;
			r.setExtensionToForcedExecution(repres.getExtensionExecutionForcee());
		}
		return r;
	}

	/**
	 * @param child un rapport de filiation vers un enfant
	 * @return un objet {@link ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties}
	 */
	public static RelationBetweenParties newFiliationTowardsChild(Parente child) {
		return newFiliation(child, true);
	}

	/**
	 * @param parent un rapport de filiation vers un parent
	 * @return un objet {@link ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties}
	 */
	public static RelationBetweenParties newFiliationTowardsParent(Parente parent) {
		return newFiliation(parent, false);
	}

	private static RelationBetweenParties newFiliation(Parente parente, boolean towardsChild) {
		final RelationBetweenParties r = new RelationBetweenParties();
		r.setType(towardsChild ? RelationBetweenPartiesType.CHILD : RelationBetweenPartiesType.PARENT);
		r.setDateFrom(DataHelper.coreToXMLv1(parente.getDateDebut()));
		r.setDateTo(DataHelper.coreToXMLv1(parente.getDateFin()));
		r.setOtherPartyNumber(towardsChild ? parente.getSujetId().intValue() : parente.getObjetId().intValue());
		return r;
	}
}
