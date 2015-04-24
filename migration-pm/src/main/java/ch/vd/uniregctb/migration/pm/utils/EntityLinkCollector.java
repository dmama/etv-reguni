package ch.vd.uniregctb.migration.pm.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;

public class EntityLinkCollector {

	// TODO il y en a sans doute d'autre...
	public enum LinkType {

		/**
		 * Lien entre un établissement et son entreprise parente
		 */
		ETABLISSEMENT_ENTREPRISE,

		/**
		 * Lien entre un établissement et la personne physique exerçant une activité indépendante
		 */
		ETABLISSEMENT_PERSONNE_PHYSIQUE,

		/**
		 * Lien entre un mandant et son mandataire
		 */
		MANDANT_MANDATAIRE,

		/**
		 * Lien entre des entreprises avant fusion vers l'entreprise après
		 */
		FUSION_ENTREPRISES
	}

	public static abstract class EntityLink<S extends Tiers, D extends Tiers, R extends RapportEntreTiers> implements DateRange {

		private final LinkType type;
		private final Supplier<S> source;
		private final Supplier<D> destination;
		private final RegDate dateDebut;
		private final RegDate dateFin;

		protected EntityLink(LinkType type, Supplier<S> source, Supplier<D> destination, RegDate dateDebut, RegDate dateFin) {
			this.type = type;
			this.source = source;
			this.destination = destination;
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
		}

		public final LinkType getType() {
			return type;
		}

		protected final Supplier<S> getSource() {
			return source;
		}

		protected final Supplier<D> getDestination() {
			return destination;
		}

		protected final S resolveSource() {
			return source.get();
		}

		protected final D resolveDestination() {
			return destination.get();
		}

		@Override
		public final boolean isValidAt(RegDate date) {
			return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
		}

		@Override
		public final RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public final RegDate getDateFin() {
			return dateFin;
		}

		/**
		 * @return le rapport entre tiers
		 */
		public abstract R toRapportEntreTiers();
	}

	public static final class EtablissementEntrepriseLink extends EntityLink<Etablissement, Entreprise, RapportEntreTiers> {

		public EtablissementEntrepriseLink(Supplier<Etablissement> etablissement, Supplier<Entreprise> entreprise, RegDate dateDebut, RegDate dateFin) {
			super(LinkType.ETABLISSEMENT_ENTREPRISE, etablissement, entreprise, dateDebut, dateFin);
		}

		public Etablissement resolveEtablissement() {
			return resolveSource();
		}

		public Entreprise resolveEntreprise() {
			return resolveDestination();
		}

		@Override
		public RapportEntreTiers toRapportEntreTiers() {
			// TODO implémenter le rapport entre tiers qui va bien et l'instancier avec les bonnes dates,
			throw new NotImplementedException("A implémenter...");
		}
	}

	public static final class EtablissementPersonnePhysiqueLink extends EntityLink<Etablissement, PersonnePhysique, RapportEntreTiers> {

		public EtablissementPersonnePhysiqueLink(Supplier<Etablissement> etablissement, Supplier<PersonnePhysique> personnePhysique, RegDate dateDebut, RegDate dateFin) {
			super(LinkType.ETABLISSEMENT_PERSONNE_PHYSIQUE, etablissement, personnePhysique, dateDebut, dateFin);
		}

		public Etablissement resolveEtablissement() {
			return resolveSource();
		}

		public PersonnePhysique resolvePersonnePhysique() {
			return resolveDestination();
		}

		@Override
		public RapportEntreTiers toRapportEntreTiers() {
			// TODO implémenter le rapport entre tiers qui va bien et l'instancier avec les bonnes dates,
			throw new NotImplementedException("A implémenter...");
		}
	}

	public static final class MandantMandataireLink<S extends Tiers, D extends Tiers> extends EntityLink<S, D, RapportEntreTiers> {

		// TODO rajouter un type de mandat ?
		public MandantMandataireLink(Supplier<S> mandant, Supplier<D> mandataire, RegDate dateDebut, RegDate dateFin) {
			super(LinkType.MANDANT_MANDATAIRE, mandant, mandataire, dateDebut, dateFin);
		}

		public S resolveMandant() {
			return resolveSource();
		}

		public D resolveMandataire() {
			return resolveDestination();
		}

		@Override
		public RapportEntreTiers toRapportEntreTiers() {
			// TODO implémenter le rapport entre tiers qui va bien et l'instancier avec les bonnes dates,
			throw new NotImplementedException("A implémenter...");
		}
	}

	public static final class FusionEntreprisesLink extends EntityLink<Entreprise, Entreprise, RapportEntreTiers> {

		public FusionEntreprisesLink(Supplier<Entreprise> avant, Supplier<Entreprise> apres, RegDate dateDebut, RegDate dateFin) {
			super(LinkType.FUSION_ENTREPRISES, avant, apres, dateDebut, dateFin);
		}

		public Entreprise resolveEntrepriseFusionnante() {
			return resolveSource();
		}

		public Entreprise resolveEntrepriseFusionnee() {
			return resolveDestination();
		}

		@Override
		public RapportEntreTiers toRapportEntreTiers() {
			// TODO implémenter le rapport entre tiers qui va bien et l'instancier avec les bonnes dates,
			throw new NotImplementedException("A implémenter...");
		}
	}

	private final List<EntityLink> collectedLinks = new LinkedList<>();

	public void addLink(EntityLink link) {
		collectedLinks.add(link);
	}

	public List<EntityLink> getCollectedLinks() {
		return collectedLinks;
	}
}
