package ch.vd.uniregctb.couple;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatCivil;

public interface CoupleManager {

	Couple getCoupleForReconstitution(PersonnePhysique pp1, PersonnePhysique pp2, RegDate date);

	Couple getCoupleForFusion(PersonnePhysique pp1, PersonnePhysique pp2, @Nullable RegDate date);

	/**
	 * A partir des numéros de contribuables des (futurs) composants du ménage, cette méthode détermine le genre de mise en ménage (mariage, réconciliation, ...) ainsi que les éventuels éléments
	 * obligatoires du futur ménage (date de début, numéro de ménage-commun, ...)
	 *
	 * @param pp1Id le numéro de la première personne physique
	 * @param pp2Id le numéro de la seconde personne physique (optionel)
	 * @param mcId  le numéro du futur ménage commun (optionel)
	 * @return les informations sur le futur ménage-commun
	 */
	@NotNull
	CoupleInfo determineInfoFuturCouple(@Nullable Long pp1Id, @Nullable Long pp2Id, @Nullable Long mcId);

	/**
	 * Enregistre un nouveau couple dans la base de données, et retourne-le.
	 *
	 * @param pp1Id     le numéro de la première personne physique
	 * @param pp2Id     le numéro de la seconde personne physique (optionel)
	 * @param mcId      le numéro du futur ménage commun (optionel)
	 * @param dateDebut la date de début de validité du ménage
	 * @param typeUnion le type d'union
	 * @param etatCivil l'état civil des composants du ménage (mariage ou pacs)
	 * @param remarque  une éventuelle remarque
	 * @return le nouveau ménage créé
	 * @throws MetierServiceException en cas d'erreur lors de la création du ménage
	 */
	MenageCommun sauverCouple(@NotNull Long pp1Id, @Nullable Long pp2Id, @Nullable Long mcId, @NotNull RegDate dateDebut, @NotNull TypeUnion typeUnion, @NotNull EtatCivil etatCivil,
	                          @Nullable String remarque) throws MetierServiceException;

	@SuppressWarnings({"UnusedDeclaration"})
	class Couple {
		private final TypeUnion typeUnion;
		private final Contribuable premierTiers;
		private final Contribuable secondTiers;

		public Couple(TypeUnion typeUnion, Contribuable premierTiers, Contribuable secondTiers) {
			this.typeUnion = typeUnion;
			this.premierTiers = premierTiers;
			this.secondTiers = secondTiers;
		}

		public TypeUnion getTypeUnion() {
			return typeUnion;
		}

		public Contribuable getPremierTiers() {
			return premierTiers;
		}

		public Contribuable getSecondTiers() {
			return secondTiers;
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	class CoupleInfo {
		private TypeUnion type;
		private EtatCivil etatCivil;
		private RegDate forceDateDebut;
		private Long forceMcId;

		public CoupleInfo(TypeUnion type, EtatCivil etatCivil, RegDate forceDateDebut, Long forceMcId) {
			this.etatCivil = etatCivil;
			this.type = type;
			this.forceDateDebut = forceDateDebut;
			this.forceMcId = forceMcId;
		}

		public EtatCivil getEtatCivil() {
			return etatCivil;
		}

		public TypeUnion getType() {
			return type;
		}

		public RegDate getForceDateDebut() {
			return forceDateDebut;
		}

		public Long getForceMcId() {
			return forceMcId;
		}
	}
}
