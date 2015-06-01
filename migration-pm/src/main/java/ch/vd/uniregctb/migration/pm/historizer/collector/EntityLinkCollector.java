package ch.vd.uniregctb.migration.pm.historizer.collector;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
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

	public static final class EtablissementEntiteJuridiqueLink<T extends Contribuable> extends EntityLink<Etablissement, T, RapportEntreTiers> {

		public EtablissementEntiteJuridiqueLink(Supplier<Etablissement> etablissement, Supplier<T> entiteJuridique, RegDate dateDebut, RegDate dateFin) {
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

		public MandantMandataireLink(Supplier<S> mandant, Supplier<D> mandataire, RegDate dateDebut, RegDate dateFin, TypeMandat typeMandat, String iban, String bicSwift) {
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
			return new FusionEntreprises(getDateDebut(), getDateFin(), resolveEntrepriseFusionnante(), resolveEntrepriseFusionnee());
		}
	}
}
