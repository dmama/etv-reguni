package ch.vd.uniregctb.migration.pm.engine.collector;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.KeyedSupplier;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.AdministrationEntreprise;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SocieteDirection;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeMandat;

public class EntityLinkCollector {

	/**
	 * La liste des données de lien reçues
	 */
	private final List<EntityLink> collectedLinks = new LinkedList<>();

	/**
	 * Les entités neutralisées (parce qu'en fait, elles ne seront pas migrées...)
	 */
	private final Set<EntityKey> neutralizedKeys = new HashSet<>();

	/**
	 * Ajout d'un nouveau lien
	 * @param link le lien en question
	 */
	public void addLink(EntityLink link) {
		collectedLinks.add(link);
	}

	/**
	 * Ajout d'une entité déclarée comme finalement non-migrée (cela aura
	 * pour conséquence de ne pas générer les liens de et vers cette entité)
	 * @param key clé de l'entité à neutraliser
	 */
	public void addNeutralizedEntity(EntityKey key) {
		neutralizedKeys.add(key);
	}

	/**
	 * @return la liste des liens finalement concernés par une réelle création
	 * (en particulier, les entités neutralisées n'apparaîtront pas dans cette liste)
	 */
	public List<EntityLink> getCollectedLinks() {
		return collectedLinks.stream()
				.filter(link -> !neutralizedKeys.contains(link.getSourceKey()))
				.filter(link -> !neutralizedKeys.contains(link.getDestinationKey()))
				.collect(Collectors.toList());
	}

	/**
	 * Indicateur de la raison pour laquelle un lien est finalement écarté de la création...
	 */
	public enum NeutralizationReason {

		/**
		 * La source a été neutralisée (mais pas la destination)
		 */
		SOURCE_NEUTRALIZED,

		/**
		 * La destination a été neutralisée (mais pas la source)
		 */
		DESTINATION_NEUTRALIZED,

		/**
		 * La source ET la destination ont été toutes deux neutralisées
		 */
		BOTH_NEUTRALIZED;

		/**
		 * @return <code>true</code> dès que la source est indiquée comme neutralisée
		 */
		public boolean isSourceNeutralisee() {
			return this == SOURCE_NEUTRALIZED || this == BOTH_NEUTRALIZED;
		}

		/**
		 * @return <code>true</code> dès que la destination est indiquée comme neutralisée
		 */
		public boolean isDestinationNeutralisee() {
			return this == DESTINATION_NEUTRALIZED || this == BOTH_NEUTRALIZED;
		}
	}

	/**
	 * @return la liste des liens finalement écartés d'une réelle création car l'un des participants a été neutralisé
	 * @see #addNeutralizedEntity(EntityKey)
	 */
	public List<Pair<NeutralizationReason, EntityLink>> getNeutralizedLinks() {
		return collectedLinks.stream()
				.map(link -> {
					final boolean sourceNeutralisee = neutralizedKeys.contains(link.getSourceKey());
					final boolean destinationNeutralisee = neutralizedKeys.contains(link.getDestinationKey());
					if (sourceNeutralisee && destinationNeutralisee) {
						return Pair.of(NeutralizationReason.BOTH_NEUTRALIZED, link);
					}
					else if (sourceNeutralisee) {
						return Pair.of(NeutralizationReason.SOURCE_NEUTRALIZED, link);
					}
					else if (destinationNeutralisee) {
						return Pair.of(NeutralizationReason.DESTINATION_NEUTRALIZED, link);
					}
					else {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * Pour les tests seulement...
	 * @return une version Read-Only de l'ensemble des clés neutralisées
	 */
	public Set<EntityKey> getNeutralizedKeys() {
		return Collections.unmodifiableSet(neutralizedKeys);
	}

	public enum LinkType {

		/**
		 * Lien entre un établissement et son entreprise/individu parent
		 */
		ETABLISSEMENT_ENTITE_JURIDIQUE,

		/**
		 * Lien entre un mandant et son mandataire
		 */
		MANDANT_MANDATAIRE,

		/**
		 * Lien entre des entreprises avant fusion vers l'entreprise après
		 */
		FUSION_ENTREPRISES,

		/**
		 * Lien entre une entreprise et ses administrateurs
		 */
		ENTREPRISE_ADMINISTRATEUR,

		/**
		 * Lien entre le propriétaire d'un fonds de placement et le fonds de placement
		 */
		PROPRIETAIRE_FONDS_PLACEMENT
	}

	public abstract static class EntityLink<S extends Tiers, D extends Tiers, R extends RapportEntreTiers> implements DateRange {

		private final LinkType type;
		private final Supplier<S> source;
		private final EntityKey sourceKey;
		private final Supplier<D> destination;
		private final EntityKey destinationKey;
		private final RegDate dateDebut;
		private final RegDate dateFin;

		private S resolvedSource = null;            // cache de la donnée initialisée à la première demande
		private D resolvedDestination = null;       // cache de la donnée initialisée à la première demande

		protected EntityLink(LinkType type, KeyedSupplier<S> source, KeyedSupplier<D> destination, RegDate dateDebut, RegDate dateFin) {
			this(type, source, source.getKey(), destination, destination.getKey(), dateDebut, dateFin);
		}

		protected EntityLink(LinkType type, Supplier<S> source, KeyedSupplier<D> destination, RegDate dateDebut, RegDate dateFin) {
			this(type, source, null, destination, destination.getKey(), dateDebut, dateFin);
		}

		protected EntityLink(LinkType type, KeyedSupplier<S> source, Supplier<D> destination, RegDate dateDebut, RegDate dateFin) {
			this(type, source, source.getKey(), destination, null, dateDebut, dateFin);
		}

		private EntityLink(LinkType type, Supplier<S> source, EntityKey sourceKey, Supplier<D> destination, EntityKey destinationKey, RegDate dateDebut, RegDate dateFin) {
			this.type = type;
			this.source = source;
			this.sourceKey = sourceKey;
			this.destination = destination;
			this.destinationKey = destinationKey;
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

		@Nullable
		public final EntityKey getSourceKey() {
			return sourceKey;
		}

		public final S resolveSource() {
			if (resolvedSource == null) {
				resolvedSource = source.get();
			}
			return resolvedSource;
		}

		@Nullable
		public final EntityKey getDestinationKey() {
			return destinationKey;
		}

		public final D resolveDestination() {
			if (resolvedDestination == null) {
				resolvedDestination = destination.get();
			}
			return resolvedDestination;
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

	public static final class EtablissementEntiteJuridiqueLink<T extends Contribuable> extends EntityLink<Etablissement, T, RapportEntreTiers> {

		private final boolean principal;

		public EtablissementEntiteJuridiqueLink(KeyedSupplier<Etablissement> etablissement, KeyedSupplier<T> entiteJuridique, RegDate dateDebut, RegDate dateFin, boolean principal) {
			super(LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, etablissement, entiteJuridique, dateDebut, dateFin);
			this.principal = principal;
		}

		public EtablissementEntiteJuridiqueLink(Supplier<Etablissement> etablissement, KeyedSupplier<T> entiteJuridique, RegDate dateDebut, RegDate dateFin, boolean principal) {
			super(LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, etablissement, entiteJuridique, dateDebut, dateFin);
			this.principal = principal;
		}

		public Etablissement resolveEtablissement() {
			return resolveSource();
		}

		public T resolveEntiteJuridique() {
			return resolveDestination();
		}

		@Override
		public RapportEntreTiers toRapportEntreTiers() {
			return new ActiviteEconomique(getDateDebut(), getDateFin(), resolveEntiteJuridique(), resolveEtablissement(), principal);
		}
	}

	public static final class MandantMandataireLink<S extends Contribuable, D extends Contribuable> extends EntityLink<S, D, RapportEntreTiers> {

		private final TypeMandat typeMandat;
		private final String iban;
		private final String bicSwift;
		private final String nomContact;
		private final String prenomContact;
		private final String noTelephoneContact;

		public MandantMandataireLink(KeyedSupplier<S> mandant,
		                             KeyedSupplier<D> mandataire,
		                             RegDate dateDebut,
		                             RegDate dateFin,
		                             TypeMandat typeMandat,
		                             String iban,
		                             String bicSwift,
		                             String nomContact,
		                             String prenomContact,
		                             String noTelephoneContact) {
			super(LinkType.MANDANT_MANDATAIRE, mandant, mandataire, dateDebut, dateFin);
			this.typeMandat = typeMandat;
			this.iban = iban;
			this.bicSwift = bicSwift;
			this.nomContact = nomContact;
			this.prenomContact = prenomContact;
			this.noTelephoneContact = noTelephoneContact;
		}

		public S resolveMandant() {
			return resolveSource();
		}

		public D resolveMandataire() {
			return resolveDestination();
		}

		@Override
		public RapportEntreTiers toRapportEntreTiers() {
			final CoordonneesFinancieres cf = iban != null || bicSwift != null ? new CoordonneesFinancieres(iban, bicSwift) : null;
			final Mandat mandat = new Mandat(getDateDebut(), getDateFin(), resolveMandant(), resolveMandataire(), typeMandat);
			mandat.setCoordonneesFinancieres(cf);
			mandat.setNomPersonneContact(nomContact);
			mandat.setPrenomPersonneContact(prenomContact);
			mandat.setNoTelephoneContact(noTelephoneContact);
			return mandat;
		}
	}

	public static final class FusionEntreprisesLink extends EntityLink<Entreprise, Entreprise, RapportEntreTiers> {

		public FusionEntreprisesLink(KeyedSupplier<Entreprise> avant, KeyedSupplier<Entreprise> apres, RegDate dateDebut, RegDate dateFin) {
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
			return new FusionEntreprises(getDateDebut(), getDateFin(), resolveEntrepriseFusionnante(), resolveEntrepriseFusionnee());
		}
	}

	public static final class EntrepriseAdministrateurLink extends EntityLink<Entreprise, PersonnePhysique, RapportEntreTiers> {

		private final boolean president;

		public EntrepriseAdministrateurLink(KeyedSupplier<Entreprise> entreprise, KeyedSupplier<PersonnePhysique> administrateur, RegDate dateDebut, RegDate dateFin, boolean president) {
			super(LinkType.ENTREPRISE_ADMINISTRATEUR, entreprise, administrateur, dateDebut, dateFin);
			this.president = president;
		}

		public Entreprise resolveEntreprise() {
			return resolveSource();
		}

		public PersonnePhysique resolveAdministrateur() {
			return resolveDestination();
		}

		@Override
		public RapportEntreTiers toRapportEntreTiers() {
			return new AdministrationEntreprise(getDateDebut(), getDateFin(), resolveEntreprise(), resolveAdministrateur(), president);
		}
	}

	public static final class ProprietaireFondsPlacementLink extends EntityLink<Entreprise, Entreprise, RapportEntreTiers> {

		public ProprietaireFondsPlacementLink(KeyedSupplier<Entreprise> proprietaire, KeyedSupplier<Entreprise> fondsPlacement, RegDate dateDebut, RegDate dateFin) {
			super(LinkType.PROPRIETAIRE_FONDS_PLACEMENT, proprietaire, fondsPlacement, dateDebut, dateFin);
		}

		public Entreprise resolveProprietaireFonds() {
			return resolveSource();
		}

		public Entreprise resolveFondsPlacement() {
			return resolveDestination();
		}

		@Override
		public RapportEntreTiers toRapportEntreTiers() {
			return new SocieteDirection(getDateDebut(), getDateFin(), resolveProprietaireFonds(), resolveFondsPlacement());
		}
	}

}
