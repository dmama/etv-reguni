package ch.vd.uniregctb.foncier;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AbstractJobResults;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;

public class EnvoiFormulairesDemandeDegrevementICIResults extends AbstractJobResults<EnvoiFormulairesDemandeDegrevementICIResults.InformationDroitsContribuable, EnvoiFormulairesDemandeDegrevementICIResults> {

	public final int nbThreads;
	public final Integer nbMaxEnvois;
	public final RegDate dateTraitement;
	public final RegDate dateSeuilMutationRF;

	private final RegistreFoncierService registreFoncierService;

	private final List<DemandeDegrevementEnvoyee> envois = new LinkedList<>();
	private final List<DemandeDegrevementNonEnvoyee> ignores = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();
	private int nbDroitsInspectes = 0;
	private int nbDroitsIgnores = 0;
	private boolean wasInterrupted = false;

	public EnvoiFormulairesDemandeDegrevementICIResults(int nbThreads, Integer nbMaxEnvois, RegDate dateTraitement, RegDate dateSeuilMutationRF, RegistreFoncierService registreFoncierService) {
		this.nbThreads = nbThreads;
		this.nbMaxEnvois = nbMaxEnvois;
		this.dateTraitement = dateTraitement;
		this.dateSeuilMutationRF = dateSeuilMutationRF;
		this.registreFoncierService = registreFoncierService;
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

	public final class ImmeubleInfo implements Comparable<ImmeubleInfo> {

		public final Long idImmeuble;
		public final String nomCommune;
		public final Integer noOfsCommune;
		public final Integer noParcelle;
		public final Integer index1;
		public final Integer index2;
		public final Integer index3;
		public final OutputInfoBaseAvecImmeubles parent;

		public ImmeubleInfo(@NotNull ImmeubleRF immeuble, RegDate dateTraitement, OutputInfoBaseAvecImmeubles parent) {
			this.idImmeuble = immeuble.getId();
			final SituationRF situation = registreFoncierService.getSituation(immeuble, dateTraitement);
			final Commune commune = registreFoncierService.getCommune(immeuble, dateTraitement);
			if (situation == null) {
				this.nomCommune = null;
				this.noOfsCommune = null;
				this.noParcelle = null;
				this.index1 = null;
				this.index2 = null;
				this.index3 = null;
			}
			else {
				if (commune != null) {
					this.nomCommune = commune.getNomOfficiel();
					this.noOfsCommune = commune.getNoOFS();
				}
				else {
					this.nomCommune = null;
					this.noOfsCommune = null;
				}
				this.noParcelle = situation.getNoParcelle();
				this.index1 = situation.getIndex1();
				this.index2 = situation.getIndex2();
				this.index3 = situation.getIndex3();
			}
			this.parent = parent;
		}

		public Long getIdImmeuble() {
			return idImmeuble;
		}

		public String getNomCommune() {
			return nomCommune;
		}

		public Integer getNoOfsCommune() {
			return noOfsCommune;
		}

		public Integer getNoParcelle() {
			return noParcelle;
		}

		public Integer getIndex1() {
			return index1;
		}

		public Integer getIndex2() {
			return index2;
		}

		public Integer getIndex3() {
			return index3;
		}

		public OutputInfoBaseAvecImmeubles getParent() {
			return parent;
		}

		@Override
		public int compareTo(@NotNull ImmeubleInfo o) {
			return Long.compare(idImmeuble, o.idImmeuble);
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

	public abstract class OutputInfoBaseAvecImmeuble<T extends OutputInfoBaseAvecImmeuble<T>> extends OutputInfoBase<T> {
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

			final Optional<SituationRF> situation = Optional.ofNullable(immeuble).map(i -> registreFoncierService.getSituation(i, dateTraitement));
			final Optional<Commune> commune = Optional.ofNullable(immeuble).map(i -> registreFoncierService.getCommune(i, dateTraitement));
			this.nomCommune = commune.map(Commune::getNomOfficiel).orElse(null);
			this.noOfsCommune = commune.map(Commune::getNoOFS).orElse(null);
			this.noParcelle = situation.map(SituationRF::getNoParcelle).orElse(null);
			this.index1 = situation.map(SituationRF::getIndex1).orElse(null);
			this.index2 = situation.map(SituationRF::getIndex2).orElse(null);
			this.index3 = situation.map(SituationRF::getIndex3).orElse(null);
		}
	}

	public abstract class OutputInfoBaseAvecImmeubles<T extends OutputInfoBaseAvecImmeubles<T>> extends OutputInfoBase<T> {

		private final List<ImmeubleInfo> immeubleInfos;

		public OutputInfoBaseAvecImmeubles(Tiers contribuable, @Nullable ImmeubleRF immeuble, RegDate dateTraitement) {
			super(contribuable.getNumero());
			this.immeubleInfos = (immeuble == null ? Collections.emptyList() : Collections.singletonList(new ImmeubleInfo(immeuble, dateTraitement, this)));
		}

		public OutputInfoBaseAvecImmeubles(Tiers contribuable, @NotNull List<ImmeubleRF> immeubles, RegDate dateTraitement) {
			super(contribuable.getNumero());
			this.immeubleInfos = immeubles.stream()
					.map((immeuble) -> new ImmeubleInfo(immeuble, dateTraitement, this))
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toList());
		}

		public List<ImmeubleInfo> getImmeubleInfos() {
			return immeubleInfos;
		}
	}

	public final class DemandeDegrevementEnvoyee extends OutputInfoBaseAvecImmeuble<DemandeDegrevementEnvoyee> {
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
		CONTRIBUABLE_TOTALEMENT_EXONERE("Contribuable totalement exonéré"),
		REGIME_FISCAL_INDETERMINE("Contribuable avec régime fiscal en attente de détermination"),
		DEGREVEMENT_DEJA_ACTIF_ANNEE_SUIVANT_DEBUT_DROIT("Dégrèvement déjà présent pour l'année suivant la date de début du droit"),
		DEGREVEMENT_ULTERIEUR_DEJA_PRESENT("Dégrèvement déjà présent pour une période postérieure à la période visée"),
		DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_SUIVANT_DEBUT_DROIT("Demande de dégrèvement déjà présente pour l'année suivant la date de début de droit"),
		DEMANDE_DEGREVEMENT_DEJA_PRESENTE_POUR_ANNEE_ESTIMATION_FISCALE("Demande de dégrèvement déjà présente pour l'année suivant l'année de la dernière estimation fiscale"),
		DEMANDE_DEGREVEMENT_DEJA_PRESENTE_DEPUIS_DERNIER_CHANGEMENT("Demande de dégrèvement déjà présente depuis le dernier changement"),
		DEMANDE_DEGREVEMENT_ULTERIEURE_DEJA_PRESENTE("Demande de dégrèvement déjà présente pour une période postérieure à la période visée"),
		ESTIMATION_FISCALE_ABSENTE_OU_ZERO("Estimation fiscale absente ou égale à zéro"),
		DATE_MUTATION_AVANT_SEUIL("Date de mutation antérieure au seuil paramétré"),
		DROIT_USUFRUIT_OU_HABITATION("Droit d'usufruit ou d'habitation"),
		DROIT_CLOTURE("Droit de propriété clôturé");

		public final String description;

		RaisonIgnorance(String description) {
			this.description = description;
		}
	}

	public final class DemandeDegrevementNonEnvoyee extends OutputInfoBaseAvecImmeubles<DemandeDegrevementNonEnvoyee> {

		public final RaisonIgnorance raison;
		public final String messageAdditionnel;

		public DemandeDegrevementNonEnvoyee(Tiers contribuable, @Nullable ImmeubleRF immeuble, RegDate dateTraitement, RaisonIgnorance raison, @Nullable String messageAdditionnel) {
			super(contribuable, immeuble, dateTraitement);
			this.raison = raison;
			this.messageAdditionnel = messageAdditionnel;
		}

		public DemandeDegrevementNonEnvoyee(Entreprise entreprise, @NotNull List<ImmeubleRF> immeubles, RegDate dateTraitement, RaisonIgnorance raison, String messageAdditionnel) {
			super(entreprise, immeubles, dateTraitement);
			this.raison = raison;
			this.messageAdditionnel = messageAdditionnel;
		}

		@Override
		public int compareTo(@NotNull DemandeDegrevementNonEnvoyee o) {
			int comparison = super.compareTo(o);
			if (comparison != 0) {
				return comparison;
			}
			// on compare les tailles des listes d'immeubles
			comparison = Integer.compare(this.getImmeubleInfos().size(), o.getImmeubleInfos().size());
			if (comparison != 0) {
				return comparison;
			}
			// on compare les ids des immeubles
			for (int i = 0; i < getImmeubleInfos().size(); i++) {
				final ImmeubleInfo left = getImmeubleInfos().get(i);
				final ImmeubleInfo right = o.getImmeubleInfos().get(i);
				comparison = Comparator.<ImmeubleInfo>nullsFirst(Comparator.naturalOrder()).compare(left, right);
				if (comparison != 0) {
					return comparison;
				}
			}

			// on compare les messages additionnels
			return Comparator.nullsLast(Comparator.<String>naturalOrder()).compare(this.messageAdditionnel, o.messageAdditionnel);
		}
	}

	public final class Erreur extends OutputInfoBaseAvecImmeuble<Erreur> {
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

	public void addContribuableTotalementExonere(Entreprise entreprise, ImmeubleRF immeuble, int periodeFiscale) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  immeuble,
		                                                  dateTraitement,
		                                                  RaisonIgnorance.CONTRIBUABLE_TOTALEMENT_EXONERE,
		                                                  String.format("Exonération totale ICI valable sur la période %d", periodeFiscale)));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addContribuableAvecRegimeFiscalIndetermine(Entreprise entreprise, ImmeubleRF immeuble, int periodeFiscale) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  immeuble,
		                                                  dateTraitement,
		                                                  RaisonIgnorance.REGIME_FISCAL_INDETERMINE,
		                                                  String.format("Nécessaire pour la période %d", periodeFiscale)));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
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

	public void addDroitClotureAvantDebutPeriodeVisee(Entreprise entreprise, ImmeubleRF immeuble, RegDate dateClotureDroit, int periodeVisee) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  immeuble,
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DROIT_CLOTURE,
		                                                  String.format("Droit clôturé au %s, avant le début de la PF %d",
		                                                                RegDateHelper.dateToDisplayString(dateClotureDroit),
		                                                                periodeVisee)));

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

	public void addDemandeDegrevementEnvoyeePourPeriodeUlterieureAPeriodeVisee(Entreprise entreprise, DemandeDegrevementICI demandeDegrevement, int periodeVisee) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  demandeDegrevement.getImmeuble(),
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DEMANDE_DEGREVEMENT_ULTERIEURE_DEJA_PRESENTE,
		                                                  String.format("Demande émise le %s pour la PF %d (période visée : %d)",
		                                                                RegDateHelper.dateToDisplayString(demandeDegrevement.getDateEnvoi()),
		                                                                demandeDegrevement.getPeriodeFiscale(),
		                                                                periodeVisee)));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addDegrevementUlterieurAPeriodeVisee(Entreprise entreprise, DegrevementICI degrevement, int periodeVisee) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  degrevement.getImmeuble(),
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DEGREVEMENT_ULTERIEUR_DEJA_PRESENT,
		                                                  String.format("Dégrèvement (%s - %s) (période visée : %d)",
		                                                                StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(degrevement.getDateDebut()), "?"),
		                                                                StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(degrevement.getDateFin()), "?"),
		                                                                periodeVisee)));

		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addDroitNonPropriete(Entreprise entreprise, @NotNull List<ImmeubleRF> immeubles, Class<? extends DroitRF> classeDroit) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  immeubles,
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

	public void addDateDebutNonDeterminable(Entreprise entreprise, ImmeubleRF immeuble) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  immeuble,
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DATE_MUTATION_AVANT_SEUIL,
		                                                  "Date de mutation non-déterminable"));
		++ this.nbDroitsInspectes;
		++ this.nbDroitsIgnores;
	}

	public void addDateDebutAvantSeuilMutationRF(Entreprise entreprise, ImmeubleRF immeuble, RegDate dateMutation, String descriptionMutation) {
		this.ignores.add(new DemandeDegrevementNonEnvoyee(entreprise,
		                                                  immeuble,
		                                                  dateTraitement,
		                                                  RaisonIgnorance.DATE_MUTATION_AVANT_SEUIL,
		                                                  String.format("%s (%s)", descriptionMutation, RegDateHelper.dateToDisplayString(dateMutation))));
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
