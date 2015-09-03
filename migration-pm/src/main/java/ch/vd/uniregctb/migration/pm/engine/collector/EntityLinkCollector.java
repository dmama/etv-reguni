package ch.vd.uniregctb.migration.pm.engine.collector;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.KeyedSupplier;
import ch.vd.uniregctb.tiers.ActiviteEconomique;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.FusionEntreprises;
import ch.vd.uniregctb.tiers.Mandat;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeMandat;

public class EntityLinkCollector {

	private final List<EntityLink> collectedLinks = new LinkedList<>();

	public void addLink(EntityLink link) {
		collectedLinks.add(link);
	}

	public List<EntityLink> getCollectedLinks() {
		return collectedLinks;
	}

	// TODO il y en a sans doute d'autre...
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
		FUSION_ENTREPRISES
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

		public EtablissementEntiteJuridiqueLink(KeyedSupplier<Etablissement> etablissement, KeyedSupplier<T> entiteJuridique, RegDate dateDebut, RegDate dateFin) {
			super(LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, etablissement, entiteJuridique, dateDebut, dateFin);
		}

		public EtablissementEntiteJuridiqueLink(Supplier<Etablissement> etablissement, KeyedSupplier<T> entiteJuridique, RegDate dateDebut, RegDate dateFin) {
			super(LinkType.ETABLISSEMENT_ENTITE_JURIDIQUE, etablissement, entiteJuridique, dateDebut, dateFin);
		}

		public Etablissement resolveEtablissement() {
			return resolveSource();
		}

		public T resolveEntiteJuridique() {
			return resolveDestination();
		}

		@Override
		public RapportEntreTiers toRapportEntreTiers() {
			return new ActiviteEconomique(getDateDebut(), getDateFin(), resolveEntiteJuridique(), resolveEtablissement());
		}
	}

	public static final class MandantMandataireLink<S extends Contribuable, D extends Contribuable> extends EntityLink<S, D, RapportEntreTiers> {

		private final TypeMandat typeMandat;
		private final String iban;
		private final String bicSwift;

		public MandantMandataireLink(KeyedSupplier<S> mandant, KeyedSupplier<D> mandataire, RegDate dateDebut, RegDate dateFin, TypeMandat typeMandat, String iban, String bicSwift) {
			super(LinkType.MANDANT_MANDATAIRE, mandant, mandataire, dateDebut, dateFin);
			this.typeMandat = typeMandat;
			this.iban = iban;
			this.bicSwift = bicSwift;
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
}
