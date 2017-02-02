package ch.vd.uniregctb.foncier;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;

public class EnvoiFormulairesDemandeDegrevementICIResults extends AbstractJobResults<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable, EnvoiFormulairesDemandeDegrevementICIResults> {

	public final int nbThreads;
	public final Integer nbMaxEnvois;
	public final RegDate dateTraitement;

	private final List<DemandeDegrevementEnvoyee> envois = new LinkedList<>();
	private final List<DemandeDegrevementNonEnvoyee> ignores = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();
	private int nbDroitsInspectes = 0;
	private int nbDroitsIgnores = 0;
	private boolean wasInterrupted = false;

	public EnvoiFormulairesDemandeDegrevementICIResults(int nbThreads, Integer nbMaxEnvois, RegDate dateTraitement) {
		this.nbThreads = nbThreads;
		this.nbMaxEnvois = nbMaxEnvois;
		this.dateTraitement = dateTraitement;
	}

	/**
	 * Couple d'identifiant Droit / Immeuble
	 */
	public static final class DroitImmeuble {

		/**
		 * Identifiant du droit
		 */
		private final long idDroit;

		/**
		 * Identifiant de l'immeuble
		 */
		private final long idImmeuble;

		public DroitImmeuble(long idDroit, long idImmeuble) {
			this.idDroit = idDroit;
			this.idImmeuble = idImmeuble;
		}

		public long getIdDroit() {
			return idDroit;
		}

		public long getIdImmeuble() {
			return idImmeuble;
		}
	}

	/**
	 * Données d'entrée du processus pour un contribuable (= sa liste de droits)
	 */
	public static final class InformationDroitsContribuable {

		/**
		 * Identifiant du contribuable
		 */
		private final long noContribuable;

		/**
		 * Couples d'identifiants
		 */
		private final List<DroitImmeuble> idsDroitsImmeubles;

		public InformationDroitsContribuable(long noContribuable, List<DroitImmeuble> idsDroitsImmeubles) {
			this.noContribuable = noContribuable;
			this.idsDroitsImmeubles = idsDroitsImmeubles;
		}

		public long getNoContribuable() {
			return noContribuable;
		}

		public List<DroitImmeuble> getIdsDroitsImmeubles() {
			return idsDroitsImmeubles;
		}
	}

	public static abstract class OutputInfoBase<T extends OutputInfoBase<T>> implements Comparable<T> {
		public final long noContribuable;

		public OutputInfoBase(long noContribuable) {
			this.noContribuable = noContribuable;
		}

		@Override
		public int compareTo(@NotNull T o) {
			return Long.compare(noContribuable, o.noContribuable);
		}
	}

	public static abstract class OutputInfoBaseAvecImmeuble<T extends OutputInfoBaseAvecImmeuble<T>> extends OutputInfoBase<T> {
		public final Long idImmeuble;
		public final String nomCommune;
		public final Integer noOfsCommune;
		public final Integer noParcelle;
		public final Integer index1;
		public final Integer index2;
		public final Integer index3;

		public OutputInfoBaseAvecImmeuble(Tiers contribuable, @Nullable ImmeubleRF immeuble, RegDate dateTraitement) {
			this(contribuable.getNumero(), immeuble, dateTraitement);
		}

		public OutputInfoBaseAvecImmeuble(long noContribuable, @Nullable ImmeubleRF immeuble, RegDate dateTraitement) {
			super(noContribuable);
			this.idImmeuble = Optional.ofNullable(immeuble).map(ImmeubleRF::getId).orElse(null);

			final Optional<SituationRF> situation = Optional.ofNullable(immeuble).map(ImmeubleRF::getSituations).orElseGet(Collections::emptySet).stream()
					.filter(sit -> RegDateHelper.isBeforeOrEqual(sit.getDateDebut(), dateTraitement, NullDateBehavior.EARLIEST))
					.max(DateRangeComparator::compareRanges);
			this.nomCommune = situation.map(SituationRF::getCommune).map(CommuneRF::getNomRf).orElse(null);
			this.noOfsCommune = situation.map(SituationRF::getCommune).map(CommuneRF::getNoOfs).orElse(null);
			this.noParcelle = situation.map(SituationRF::getNoParcelle).orElse(null);
			this.index1 = situation.map(SituationRF::getIndex1).orElse(null);
			this.index2 = situation.map(SituationRF::getIndex2).orElse(null);
			this.index3 = situation.map(SituationRF::getIndex3).orElse(null);
		}
	}

	public static final class DemandeDegrevementEnvoyee extends OutputInfoBaseAvecImmeuble<DemandeDegrevementEnvoyee> {
		public final int periodeFiscale;

		public DemandeDegrevementEnvoyee(Tiers contribuable, ImmeubleRF immeuble, RegDate dateTraitement, int periodeFiscale) {
			super(contribuable, immeuble, dateTraitement);
			this.periodeFiscale = periodeFiscale;
		}

		@Override
		public int compareTo(@NotNull DemandeDegrevementEnvoyee o) {
			int comparison = super.compareTo(o);
			if (comparison == 0) {
				comparison = Integer.compare(periodeFiscale, o.periodeFiscale);
			}
			if (comparison == 0) {
				comparison = Long.compare(idImmeuble, o.idImmeuble);
			}
			return comparison;
		}
	}

	public enum RaisonIgnorance {
		CONTRIBUABLE_TOTALEMENT_EXONERE,
		DEGREVEMENT_DEJA_ACTIF_ANNEE_SUIVANT_DEBUT_DROIT,
		DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_SUIVANT_DEBUT_DROIT,
		DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_ESTIMATION_FISCALE,
		DEMANDE_DEGREVEMENT_DEJA_PRESENTE_DEPUIS_DERNIER_CHANGEMENT,
		ESTIMATION_FISCALE_ABSENTE_OU_ZERO,
		DROIT_USUFRUIT_OU_HABITATION
	}

	public static final class DemandeDegrevementNonEnvoyee extends OutputInfoBaseAvecImmeuble<DemandeDegrevementNonEnvoyee> {
		public final RaisonIgnorance raison;
		public final String messageAdditionnel;

		public DemandeDegrevementNonEnvoyee(Tiers contribuable, @Nullable ImmeubleRF immeuble, RegDate dateTraitement, RaisonIgnorance raison, @Nullable String messageAdditionnel) {
			super(contribuable, immeuble, dateTraitement);
			this.raison = raison;
			this.messageAdditionnel = messageAdditionnel;
		}

		@Override
		public int compareTo(@NotNull DemandeDegrevementNonEnvoyee o) {
			int comparison = super.compareTo(o);
			if (comparison == 0) {
				comparison = Comparator.<Long>nullsFirst(Comparator.naturalOrder()).compare(idImmeuble, o.idImmeuble);
			}
			return comparison;
		}
	}

	public static final class Erreur extends OutputInfoBaseAvecImmeuble<Erreur> {
		public final String msg;
		public Erreur(long noContribuable, RegDate dateTraitement, Exception e) {
			super(noContribuable, null, dateTraitement);
			this.msg = ExceptionUtils.extractCallStack(e);
		}
		public Erreur(Tiers ctb, ImmeubleRF immeuble, RegDate dateTraitement, String message) {
			super(ctb, immeuble, dateTraitement);
			this.msg = message;
		}
	}

	@Override
	public void addErrorException(InformationDroitsContribuable info, Exception e) {
		this.erreurs.add(new Erreur(info.noContribuable, null, e));
		this.nbDroitsInspectes += info.idsDroitsImmeubles.size();
	}

	public void addErreurPeriodeFiscaleNonDeterminable(Tiers contribuable, ImmeubleRF immeuble) {
		this.erreurs.add(new Erreur(contribuable, immeuble, dateTraitement, "La période fiscale de la demande de dégrèvement ICI n'a pas pu être déterminée."));
		++ this.nbDroitsInspectes;
	}

	@Override
	public void addAll(EnvoiFormulairesDemandeDegrevementICIResults autre) {
		this.envois.addAll(autre.envois);
		this.ignores.addAll(autre.ignores);
		this.erreurs.addAll(autre.erreurs);
		this.nbDroitsInspectes += autre.nbDroitsInspectes;
		this.nbDroitsIgnores += autre.nbDroitsIgnores;
	}

	public void addContribuableTotalementExonere(Entreprise entreprise, List<DroitImmeuble> idsDroitsImmeubles) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  null,
		                                                  dateTraitement,
		                                                  RaisonIgnorance.CONTRIBUABLE_TOTALEMENT_EXONERE,
		                                                  String.format("%d droit(s) concernés pour %d immeuble(s)",
		                                                                idsDroitsImmeubles.size(),
		                                                                idsDroitsImmeubles.stream().map(DroitImmeuble::getIdImmeuble).distinct().count())));
		this.nbDroitsInspectes += idsDroitsImmeubles.size();
		this.nbDroitsIgnores += idsDroitsImmeubles.size();
	}

	public void addDegrevementActifAnneeSuivantDebutDroit(Entreprise entreprise, int anneeSuivantDebutDroit, ImmeubleRF immeuble) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  immeuble,
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DEGREVEMENT_DEJA_ACTIF_ANNEE_SUIVANT_DEBUT_DROIT,
		                                                  String.format("Année suivant début de droit : %d", anneeSuivantDebutDroit)));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addDemandeDegrevementPourAnneeSuivantDebutDroit(Entreprise entreprise, int anneeSuivantDebutDroit, DemandeDegrevementICI demandeDegrevement) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  demandeDegrevement.getImmeuble(),
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_SUIVANT_DEBUT_DROIT,
		                                                  String.format("Demande émise le %s pour la PF %d",
		                                                                RegDateHelper.dateToDisplayString(demandeDegrevement.getDateEnvoi()),
		                                                                anneeSuivantDebutDroit)));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addDemandeDegrevementEnvoyeeDepuisDernierChangement(Entreprise entreprise, DemandeDegrevementICI demandeDegrevement) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  demandeDegrevement.getImmeuble(),
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_DEPUIS_DERNIER_CHANGEMENT,
		                                                  String.format("Demande émise le %s pour la PF %d",
		                                                                RegDateHelper.dateToDisplayString(demandeDegrevement.getDateEnvoi()),
		                                                                demandeDegrevement.getPeriodeFiscale())));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addDroitNonPropriete(Entreprise entreprise, ImmeubleRF immeuble, Class<? extends DroitRF> classeDroit) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  immeuble,
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DROIT_USUFRUIT_OU_HABITATION,
		                                                  classeDroit.getSimpleName()));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addImmeubleSansEstimationFiscale(Entreprise entreprise, ImmeubleRF immeuble) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  immeuble,
		                                                  dateTraitement,
		                                                  RaisonIgnorance.ESTIMATION_FISCALE_ABSENTE_OU_ZERO,
		                                                  null));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addDemandeDegrevementPourAnneeEstimationFiscale(Entreprise entreprise, int anneeDerniereEstimationFiscale, DemandeDegrevementICI demandeDegrevement) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  demandeDegrevement.getImmeuble(),
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_ESTIMATION_FISCALE,
		                                                  String.format("Demande émise le %s pour la PF %d",
		                                                                RegDateHelper.dateToDisplayString(demandeDegrevement.getDateEnvoi()),
		                                                                anneeDerniereEstimationFiscale)));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addEnvoiDemandeDegrevement(Entreprise entreprise, ImmeubleRF immeuble, int periodeFiscale) {
		this.envois.add(new DemandeDegrevementEnvoyee(entreprise, immeuble, dateTraitement, periodeFiscale));
		++ this.nbDroitsInspectes;
	}

	public void setInterrupted(boolean wasInterrupted) {
		this.wasInterrupted = wasInterrupted;
	}

	public boolean wasInterrupted() {
		return wasInterrupted;
	}

	@Override
	public void end() {
		this.envois.sort(Comparator.naturalOrder());
		this.erreurs.sort(Comparator.naturalOrder());
		this.ignores.sort(Comparator.naturalOrder());
		super.end();
	}

	public int getNbDroitsInspectes() {
		return nbDroitsInspectes;
	}

	public int getNbDroitsIgnores() {
		return nbDroitsIgnores;
	}

	public List<DemandeDegrevementEnvoyee> getEnvois() {
		return envois;
	}

	public List<DemandeDegrevementNonEnvoyee> getIgnores() {
		return ignores;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}
}
