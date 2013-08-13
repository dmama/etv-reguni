package ch.vd.uniregctb.xml.party.v2;

import java.util.Comparator;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.xml.common.v1.DateHelper;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v1.RelationBetweenPartiesType;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;

public class RelationBetweenPartiesBuilder {

	public static final Comparator<RelationBetweenParties> COMPARATOR = new Comparator<RelationBetweenParties>() {
		@Override
		public int compare(RelationBetweenParties o1, RelationBetweenParties o2) {
			int comparison = DateHelper.compareTo(o1, o2);
			if (comparison == 0) {
				comparison = compareTo(o1.getType(), o2.getType(), false);
				if (comparison == 0) {
					comparison = compareTo(o1.getEndDateOfLastTaxableItem(), o2.getEndDateOfLastTaxableItem(), false);
					if (comparison == 0) {
						comparison = compareTo(o1.isExtensionToForcedExecution(), o2.isExtensionToForcedExecution(), false);
						if (comparison == 0) {
							comparison = Integer.compare(o1.getOtherPartyNumber(), o2.getOtherPartyNumber());
						}
					}
				}
			}
			return comparison;
		}
	};

	private static <T extends Comparable<T>> int compareTo(@Nullable T o1, @Nullable T o2, boolean nullFirst) {
		if (o1 == o2) {
			return 0;
		}
		else if (o1 == null) {
			return nullFirst ? -1 : 1;
		}
		else if (o2 == null) {
			return nullFirst ? 1 : -1;
		}
		else {
			return o1.compareTo(o2);
		}
	}

	public static RelationBetweenParties newRelationBetweenParties(ch.vd.uniregctb.tiers.RapportEntreTiers rapport, Long autreTiersNumero) {
		final RelationBetweenParties r = new RelationBetweenParties();
		r.setType(EnumHelper.coreToXML(rapport.getType()));
		r.setDateFrom(DataHelper.coreToXML(rapport.getDateDebut()));
		r.setDateTo(DataHelper.coreToXML(rapport.getDateFin()));
		r.setCancellationDate(DataHelper.coreToXML(rapport.getAnnulationDate()));
		r.setOtherPartyNumber(autreTiersNumero.intValue());

		if (rapport instanceof ch.vd.uniregctb.tiers.RapportPrestationImposable) {
			final ch.vd.uniregctb.tiers.RapportPrestationImposable rpi = (ch.vd.uniregctb.tiers.RapportPrestationImposable) rapport;

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
		r.setDateFrom(DataHelper.coreToXML(parente.getDateDebut()));
		r.setDateTo(DataHelper.coreToXML(parente.getDateFin()));
		r.setOtherPartyNumber(towardsChild ? parente.getSujetId().intValue() : parente.getObjetId().intValue());
		return r;
	}
}
