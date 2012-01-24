package ch.vd.uniregctb.evenement.civil.interne.arrivee;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.mouvement.Mouvement;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Modélise un événement d'arrivée.
 */
public abstract class Arrivee extends Mouvement {

	protected static Logger LOGGER = Logger.getLogger(Arrivee.class);

	protected Arrivee(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		Assert.isTrue(isEvenementArrivee(evenement.getType()));
	}

	protected Arrivee(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Arrivee(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, null, null, null, context);
	}

	@Override
	public boolean isContribuablePresentBefore() {
		/*
		 * par définition, dans le case d'une arrivée le contribuable peut ne pas encore exister dans la base de données fiscale
		 */
		return false;
	}

	private boolean isEvenementArrivee(TypeEvenementCivil type) {
		boolean isPresent = false;
		if (type == TypeEvenementCivil.ARRIVEE_DANS_COMMUNE
				|| type == TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC
				|| type == TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS
				|| type == TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE
				|| type == TypeEvenementCivil.ARRIVEE_SECONDAIRE
				// [UNIREG-3379] dans le cas d'un déménagement entre deux communes non-encore fusionnées fiscalement, le déménagement se traduit par une arrivée.
				|| type == TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE) {
			isPresent = true;

		}
		return isPresent;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		/*
		 * Le retour du mort-vivant
		 */
		if (getIndividu().getDateDeces() != null) {
			erreurs.addErreur("L'individu est décédé");
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		if (isIndividuEnMenage(getIndividu(), getDate())) {
			return handleIndividuEnMenage(warnings);
		}
		else {
			return handleIndividuSeul(warnings);
		}
	}

	private boolean isIndividuEnMenage(Individu individu, RegDate date ) throws EvenementCivilException {
		// Récupération de l'état civil de l'individu
		final EtatCivil etatCivil = individu.getEtatCivil(date);
		if (etatCivil == null) {
			throw new EvenementCivilException("Impossible de déterminer l'état civil de l'individu " + individu.getNoTechnique() +
					" à la date " + RegDateHelper.dateToDisplayString(date));
		}
		return EtatCivilHelper.estMarieOuPacse(etatCivil);
	}

	/**
	 * Gère l'arrivée d'un contribuable seul.
	 */
	protected final Pair<PersonnePhysique, PersonnePhysique> handleIndividuSeul(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		try {
			final Individu individu = getIndividu();
			final RegDate dateArrivee = getDateArriveeEffective(getDate());
			final Long numeroEvenement = getNumeroEvenement();

			/*
			 * Création à la demande de l'habitant
			 */
			// [UNIREG-770] rechercher si un Non-Habitant assujetti existe (avec Nom - Prénom)
			final MutableBoolean nouvelHabitant = new MutableBoolean(false);
			final PersonnePhysique habitant = getOrCreateHabitant(individu, dateArrivee, numeroEvenement, FindBehavior.ASSUJETTISSEMENT_OBLIGATOIRE_ERROR_IF_SEVERAL, nouvelHabitant);

			/*
			 * Mise-à-jour des adresses
			 *
			 * Les éventuelles adresses fiscales doivent rester valides en l'absence de demande explicite de
			 * changement de la part du contribuable, sauf pour le cas des adresses flagées "non-permanentes"
			 * qui doivent être férmées.Si aucune adresse fiscale n'existe, on prend les adresses civiles
			 * qui sont forcément valides.
			 */
			fermeAdresseTiersTemporaire(habitant, dateArrivee.getOneDayBefore());

			/*
			 * Mise-à-jour des fors fiscaux principaux du contribuable
			 */
			doHandleCreationForIndividuSeul(habitant, warnings);

			return nouvelHabitant.booleanValue() ? new Pair<PersonnePhysique, PersonnePhysique>(habitant, null) : null;
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
	}

	/**
	 * @param date date de référence de l'événement
	 * @return date à laquelle l'arrivée doit en fait être considérée comme ayant eu lieu (en particulier pour l'ouverture des fors)
	 */
	protected RegDate getDateArriveeEffective(RegDate date) {
		return date;
	}

	/**
	 * Création des fors lors de l'arrivée d'un invididu seul
	 *
	 * @param habitant personne physique habitante qui vient d'arriver
	 * @param warnings liste des warnings à compléter au besoin
	 * @throws EvenementCivilException en cas de souci
	 */
	protected abstract void doHandleCreationForIndividuSeul(PersonnePhysique habitant, EvenementCivilWarningCollector warnings) throws EvenementCivilException;

	/**
	 * Création des fors lors de l'arrivée d'un invididu seul
	 *
	 * @param arrivant personne physique habitante qui vient d'arriver
	 * @param menageCommun ménage commun de l'arrivant
	 * @param warnings liste des warnings à compléter au besoin
	 * @throws EvenementCivilException en cas de souci
	 */
	protected abstract void doHandleCreationForMenage(PersonnePhysique arrivant, MenageCommun menageCommun, EvenementCivilWarningCollector warnings) throws EvenementCivilException;

	private List<PersonnePhysique> findNonHabitants(Individu individu, boolean assujettissementObligatoire) {
		return findNonHabitants(getService(), individu, assujettissementObligatoire);
	}

	/**
	 * [UNIREG-3073] Recherche un ou plusieurs non-habitants à partir du prénom, du nom, de la date de naissance et du sexe d'un individu.
	 *
	 * @param individu                    un individu
	 * @param assujettissementObligatoire <b>vrai</b> s'il les non-habitants recherchés doivent posséder un for principal actif.
	 * @return une liste de non-habitants qui correspondent aux critères.
	 */
	protected static List<PersonnePhysique> findNonHabitants(TiersService tiersService, Individu individu, boolean assujettissementObligatoire) {

		// les critères de recherche
		final String nomPrenom = tiersService.getNomPrenom(individu);
		final RegDate dateNaissance = individu.getDateNaissance();
		final Sexe sexe = (individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ);

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypeTiers(TiersCriteria.TypeTiers.NON_HABITANT);
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		// criteria.setDateNaissance(individu.getDateNaissance()); [UNIREG-1603] on ne filtre pas sur la date de naissance ici, pour prendre en compte les dates nulles
		criteria.setNomRaison(nomPrenom);

		final List<PersonnePhysique> nonHabitants = new ArrayList<PersonnePhysique>();

		final List<TiersIndexedData> results = tiersService.search(criteria);
		for (final TiersIndexedData tiersIndexedData : results) {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(tiersIndexedData.getNumero());
			// [UNIREG-770] le non habitant doit être assujetti
			if (assujettissementObligatoire && pp.getForFiscalPrincipalAt(null) == null) {
				continue;
			}
			// [UNIREG-1603] on filtre les dates naissance en laissant passer les dates nulles
			if (pp.getDateNaissance() != null && pp.getDateNaissance() != dateNaissance) {
				continue;
			}
			// [UNIREG-1603] on filtre le sexe en laissant passer les sexes non renseignés
			if (pp.getSexe() != null && pp.getSexe() != sexe) {
				continue;
			}
			// [UNIREG-1603] on filtre les non habitants qui possèdent un numéro d'individu
			if (pp.getNumeroIndividu() != null) {
				continue;
			}
			nonHabitants.add(pp);
		}

		return nonHabitants;
	}

	private enum FindBehavior {
		ASSUJETTISSEMENT_OBLIGATOIRE_ERROR_IF_SEVERAL(true, true),
		ASSUJETTISSEMENT_OBLIGATOIRE_NO_ERROR_IF_SEVERAL(true, false),
		ASSUJETTISSEMENT_NON_OBLIGATOIRE_ERROR_IF_SEVERAL(false, true),
		ASSUJETTISSEMENT_NON_OBLIGATOIRE_NO_ERROR_IF_SEVERAL(false, false)
		;

		private final boolean assujettissementObligatoire;

		private final boolean errorOnMultiples;

		private FindBehavior(boolean assujettissementObligatoire, boolean errorOnMultiples) {
			this.assujettissementObligatoire = assujettissementObligatoire;
			this.errorOnMultiples = errorOnMultiples;
		}

		public boolean isAssujettissementObligatoire() {
			return assujettissementObligatoire;
		}

		public boolean isErrorOnMultiples() {
			return errorOnMultiples;
		}
	}

	/**
	 * @param nouveau rempli en sortie, à <code>true</code> si un nouvel habitant a été créé, et <code>false</code> sinon
	 */
	private PersonnePhysique getOrCreateHabitant(Individu individu, RegDate dateEvenement, long evenementId, FindBehavior behavior, MutableBoolean nouveau) throws EvenementCivilException {

		final PersonnePhysique pp = context.getTiersDAO().getPPByNumeroIndividu(individu.getNoTechnique());
		final PersonnePhysique habitant;
		if (pp != null) {
			if (pp.isHabitantVD()) {
				nouveau.setValue(false);
				habitant = pp;
			}
			else {
				habitant = getService().changeNHenHabitant(pp, pp.getNumeroIndividu(), dateEvenement);
				Audit.info(evenementId, "Le non habitant " + habitant.getNumero() + " devient habitant");
				nouveau.setValue(true);
			}
		}
		else {
			final List<PersonnePhysique> nonHabitants = findNonHabitants(individu, behavior.isAssujettissementObligatoire());
			if (nonHabitants.size() == 1) {
				final PersonnePhysique candidat = nonHabitants.get(0);
				if (candidat.getDateNaissance() == null || candidat.getSexe() == null) {
					// [UNIREG-3073] si le prénom/nom correspondent mais que la date de naissance ou le sexe manquent, on lève une erreur pour que l'utilisateur puisse gérer le cas manuellement.
					final StringBuilder message = new StringBuilder();
					message.append("Un non-habitant (n°").append(candidat.getNumero()).append(") qui possède le même prénom/nom que l'individu a été trouvé, mais ");
					if (candidat.getDateNaissance() == null && candidat.getSexe() == null) {
						message.append("la date de naissance et le sexe ne sont pas renseignés.");
					}
					else if (candidat.getDateNaissance() == null) {
						message.append("la date de naissance n'est pas renseignée.");
					}
					else {
						message.append("le sexe n'est pas renseigné.");
					}
					message.append(" Veuillez vérifier manuellement.");
					throw new EvenementCivilException(message.toString());
				}
				else {
					// [UNIREG-1603] le candidat correspond parfaitement aux critères
					habitant = getService().changeNHenHabitant(candidat, individu.getNoTechnique(), dateEvenement);
					Audit.info(evenementId, "Le non habitant " + habitant.getNumero() + " devient habitant");
					nouveau.setValue(true);
				}
			}
			else if (nonHabitants.isEmpty() || !behavior.isErrorOnMultiples()) {
				final PersonnePhysique nouvelHabitant = new PersonnePhysique(true);
				nouvelHabitant.setNumeroIndividu(individu.getNoTechnique());
				Audit.info(evenementId, "Un tiers a été créé pour le nouvel arrivant");
				habitant = (PersonnePhysique) context.getTiersDAO().save(nouvelHabitant);
				nouveau.setValue(true);
			}
			else {
				// [UNIREG-2650] Message d'erreur un peu plus explicite...
				final StringBuilder b = new StringBuilder();
				boolean first = true;
				for (PersonnePhysique candidat : nonHabitants) {
					if (!first) {
						b.append(", ");
					}
					b.append(FormatNumeroHelper.numeroCTBToDisplay(candidat.getNumero()));
					first = false;
				}

				final String message = String.format("Plusieurs tiers non-habitants assujettis potentiels trouvés (%s)", b.toString());
				throw new EvenementCivilException(message);
			}
		}

		return habitant;
	}

	private static boolean isSourcier(ModeImposition modeImposition) {
		return ModeImposition.SOURCE == modeImposition || ModeImposition.MIXTE_137_1 == modeImposition || ModeImposition.MIXTE_137_2 == modeImposition;
	}

	private static RegDate findDateDebutMenageAvant(Individu individu, RegDate limiteSuperieureEtDefaut) {
		final EtatCivilList etatsCivils = individu.getEtatsCivils();
		final RegDate dateDebutMenage;
		if (etatsCivils != null && !etatsCivils.isEmpty()) {
			final ListIterator<EtatCivil> iterator = etatsCivils.listIterator(etatsCivils.size());
			RegDate candidate = limiteSuperieureEtDefaut;
			while (iterator.hasPrevious()) {
				final EtatCivil etatCivil = iterator.previous();
				if (TypeEtatCivil.MARIE == etatCivil.getTypeEtatCivil() || TypeEtatCivil.PACS == etatCivil.getTypeEtatCivil()) {
					if (etatCivil.getDateDebut() == null) {
						// si si, ça arrive...
						break;
					}
					else if (etatCivil.getDateDebut().isBeforeOrEqual(limiteSuperieureEtDefaut)) {
						candidate = etatCivil.getDateDebut();
						break;
					}
				}
			}
			dateDebutMenage = candidate;
		}
		else {
			dateDebutMenage = limiteSuperieureEtDefaut;
		}
		return dateDebutMenage;
	}

	/**
	 * Gère l'arrive d'un contribuable en ménage commun.
	 */
	protected final Pair<PersonnePhysique, PersonnePhysique> handleIndividuEnMenage(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final Individu individu = getIndividu();
		Assert.notNull(individu); // prérequis
		final Individu conjoint = context.getServiceCivil().getConjoint(getNoIndividu(), getDate());

		// [UNIREG-2212] Il faut décaler la date du for en cas d'arrivée vaudoise après le 20 décembre
		final RegDate dateEvenement = getDateArriveeEffective(getDate());
		final Long numeroEvenement = getNumeroEvenement();

		/*
		 * Récupération/création des habitants
		 */
		final MutableBoolean nouvelArrivant = new MutableBoolean(false);
		final PersonnePhysique arrivant = getOrCreateHabitant(individu, dateEvenement, numeroEvenement, FindBehavior.ASSUJETTISSEMENT_NON_OBLIGATOIRE_NO_ERROR_IF_SEVERAL, nouvelArrivant);
		final MutableBoolean nouveauConjoint = new MutableBoolean(false);
		final PersonnePhysique conjointArrivant;
		if (conjoint != null) {
			conjointArrivant = getOrCreateHabitant(conjoint, dateEvenement, numeroEvenement, FindBehavior.ASSUJETTISSEMENT_NON_OBLIGATOIRE_NO_ERROR_IF_SEVERAL, nouveauConjoint);
		}
		else {
			conjointArrivant = null;
		}

		/*
		 * Récupération/création du ménage commun et du rapports entre tiers
		 */

		// trouve la date du mariage (qui peut avoir eu lieu bien avant l'arrivée !)
		final RegDate dateDebutMenage = findDateDebutMenageAvant(individu, getDate());
		final MenageCommun menageCommun = getOrCreateMenageCommun(arrivant, conjointArrivant, dateEvenement, dateDebutMenage, numeroEvenement);
		Assert.notNull(menageCommun);

		/*
		 * Mise-à-jour des adresses
		 *
		 * Les éventuelles adresses fiscales doivent rester valides en l'absence de demande explicite de
		 * changement de la part du contribuable, sauf pour le cas des adresses flagées "non-permanentes"
		 * qui doivent être férmées.Si aucune adresse fiscale n'existe, on prend les adresses civiles
		 * qui sont forcément valides.
		 */
		fermeAdresseTiersTemporaire(menageCommun, getDate().getOneDayBefore());

		// création du for principal si nécessaire
		doHandleCreationForMenage(arrivant, menageCommun, warnings);

		if (nouvelArrivant.booleanValue() || nouveauConjoint.booleanValue()) {
			final PersonnePhysique arrivantCree = nouvelArrivant.booleanValue() ? arrivant : null;
			final PersonnePhysique conjointCree = nouveauConjoint.booleanValue() ? conjointArrivant : null;
			return new Pair<PersonnePhysique, PersonnePhysique>(arrivantCree, conjointCree);
		}
		else {
			return null;
		}
	}

	/**
	 * Récupère le ménage commun avec le contribuable principal et le conjoint comme parties.
	 *
	 * @param habitantPrincipal
	 * @param habitantConjoint
	 * @param dateEvenement date de l'événement d'arrivée
	 * @param dateDebutMenage date de début des nouveaux rapports d'appartenance ménage en cas de création de ménage
	 * @param evenementId ID technique de l'événement d'arrivée
	 * @return
	 * @throws EvenementCivilException
	 *             si les deux habitants appartiennent à des ménages différents
	 */
	private MenageCommun getOrCreateMenageCommun(final PersonnePhysique habitantPrincipal, PersonnePhysique habitantConjoint, RegDate dateEvenement, RegDate dateDebutMenage, long evenementId) throws EvenementCivilException {

		final MenageCommun menageCommun;
		final MenageCommun menageCommunHabitantPrincipal = getMenageCommunActif(habitantPrincipal);
		final MenageCommun menageCommunHabitantConjoint = getMenageCommunActif(habitantConjoint);

		if (menageCommunHabitantPrincipal != null && menageCommunHabitantConjoint != null
				&& !menageCommunHabitantPrincipal.getId().equals(menageCommunHabitantConjoint.getId())) {
			/*
			 * Les deux contribuables appartiennent chacun à un ménage différent de l'autre
			 */
			throw new EvenementCivilException("L'individu et le conjoint ne partagent pas le même ménage commun");
		}
		else if (menageCommunHabitantPrincipal == null && menageCommunHabitantConjoint == null) {

			// [UNIREG-780] recherche d'un ancien ménage auquel auraient appartenu les deux contribuables
			final MenageCommun ancienMenage = getAncienMenageCommun(habitantPrincipal, habitantConjoint);
			if (ancienMenage != null) {
				/*
				 * Les arrivant ont appartenu à un même ménage
				 */
				menageCommun = ancienMenage;
				// ajout des tiers manquant au ménage commun
				boolean rapportPrincipalTrouve = false;
				boolean rapportConjointTrouve = false;
				final Set<RapportEntreTiers> rapportsObjet = menageCommun.getRapportsObjet();
				if (rapportsObjet != null) {
					for (RapportEntreTiers rapport : rapportsObjet) {
						if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType() && rapport.isValidAt(dateEvenement)) {
							final Long tiersId = rapport.getSujetId();
							if (habitantPrincipal.getNumero().equals(tiersId)) {
								rapportPrincipalTrouve = true;
							}
							else if (habitantConjoint != null && habitantConjoint.getNumero().equals(tiersId)) {
								rapportConjointTrouve = true;
							}
						}
					}
				}
				if (!rapportPrincipalTrouve) {
					getService().addTiersToCouple(menageCommun, habitantPrincipal, dateEvenement, null);
				}
				if (!rapportConjointTrouve && habitantConjoint != null) {
					getService().addTiersToCouple(menageCommun, habitantConjoint, dateEvenement, null);
				}

				Audit.info(evenementId, String.format("Les arrivants ont appartenu au ménage commun [%d].", menageCommun.getNumero()));
			}
			else {
				/*
				 * Le ménage commun n'existe pas du tout et aucun des deux individus n'appartient à un quelconque ménage => on crée un ménage
				 * commun de toutes pièces
				 */
				final EnsembleTiersCouple ensemble = getService().createEnsembleTiersCouple(habitantPrincipal, habitantConjoint, dateDebutMenage, null);
				menageCommun = ensemble.getMenage();
				Audit.info(evenementId, String.format("Un nouveau ménage commun [%d] a été créé pour l'arrivée des nouveaux arrivants", menageCommun.getNumero()));
			}
		}
		else if (menageCommunHabitantPrincipal != null && menageCommunHabitantConjoint != null) {
			/*
			 * Les deux individus appartiennt déjà au même ménage => rien de spécial à faire
			 */
			Assert.isTrue(menageCommunHabitantPrincipal == menageCommunHabitantConjoint);
			Assert.isTrue(habitantConjoint == null
					|| habitantConjoint == getAutrePersonneDuMenage(menageCommunHabitantPrincipal, habitantPrincipal));
			Assert.isTrue(habitantConjoint == null
					|| habitantPrincipal == getAutrePersonneDuMenage(menageCommunHabitantConjoint, habitantConjoint));
			menageCommun = menageCommunHabitantPrincipal;
			Audit.info(evenementId, String.format("Le ménage commun [%d] des nouveaux arrivants existe déjà.", menageCommun.getNumero()));
		}
		else {
			if (menageCommunHabitantPrincipal != null) {
				/*
				 * L'individu principal appartient déjà à un ménage => on vérifie que le ménage en question ne possède bien qu'un seul
				 * membre actif et on rattache le conjoint au ménage
				 */
				final MenageCommun menage = menageCommunHabitantPrincipal;

				/*
				 * Vérification que l'on ajoute pas un deuxième conjoint
				 */
				final PersonnePhysique autrePersonne = getAutrePersonneDuMenage(menage, habitantPrincipal);
				if (autrePersonne != null) {
					if (habitantConjoint == null) {
						// [UNIREG-1184] marié seul dans le civil
						throw new EvenementCivilException(
								String.format("L'individu principal [%d] est en ménage commun avec une personne [%d] dans le fiscal alors qu'il est marié seul dans le civil",
										habitantPrincipal.getNumero(), autrePersonne.getNumero()));
					}
					else {
						throw new EvenementCivilException(
								String.format("L'individu principal [%d] est en ménage commun avec une personne [%d] autre que son conjoint [%d]",
										habitantPrincipal.getNumero(), autrePersonne.getNumero(), habitantConjoint.getNumero()));
					}
				}

				/*
				 * On ajoute le rapport entre l'habitant conjoint et le ménage existant
				 */
				if (habitantConjoint != null) {
					final RapportEntreTiers rapport = getService().addTiersToCouple(menage, habitantConjoint, dateDebutMenage, null);

					menageCommun = (MenageCommun) context.getTiersDAO().get(rapport.getObjetId());
					Audit.info(evenementId, String.format("L'arrivant [%d] a été attaché au ménage commun [%d] déjà existant", habitantConjoint.getNumero(), menageCommun.getNumero()));
				}
				else {
					menageCommun = menage;
				}

			}
			else {
				Assert.notNull(habitantConjoint);
				Assert.notNull(menageCommunHabitantConjoint);

				/*
				 * Le conjoint appartient déjà à un ménage => on vérifie que le ménage en question ne possède bien qu'un seul membre actif
				 * et on rattache l'individu principal au ménage
				 */
				final MenageCommun menage = menageCommunHabitantConjoint;

				/*
				 * Vérification que l'on ajoute pas un deuxième individu principal
				 */
				final PersonnePhysique autrePersonne = getAutrePersonneDuMenage(menage, habitantConjoint);
				if (autrePersonne != null) {
					final String message = String.format("L'individu conjoint [%s] est en ménage commun avec une personne [%s] autre que son individu principal[%s]",
														habitantConjoint, autrePersonne, habitantPrincipal);
					throw new EvenementCivilException(message);
				}

				/*
				 * On ajoute le rapport entre l'habitant principal et le ménage existant
				 * [UNIREG-1677] La date de début du nouveau rapport entre tiers doit être reprise sur le rapport existant
				 */
				final RapportEntreTiers rapportExistant = habitantConjoint.getRapportSujetValidAt(null, TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				Assert.notNull(rapportExistant);

				final RapportEntreTiers rapport = getService().addTiersToCouple(menage, habitantPrincipal, rapportExistant.getDateDebut(), null);
				menageCommun = (MenageCommun) context.getTiersDAO().get(rapport.getObjetId());

				final String auditString = String.format("L'arrivant [%d] a été rattaché au ménage commun [%d] dèjà existant", habitantPrincipal.getNumero(), menageCommun.getNumero());
				Audit.info(evenementId, auditString);
			}
		}
		return menageCommun;
	}

	/**
	 * Recherche l'autre personne faisant partie du ménage commun.
	 *
	 * @param menageCommun
	 *            le ménage commun à analyser
	 * @param personne
	 *            la personne de référence du ménage commun
	 * @return l'autre personne du ménage, ou null si le ménage ne possède qu'une personne
	 * @throws EvenementCivilException
	 *             si plus d'une autre personne est trouvée (ménage à trois, ah là là...)
	 */
	private PersonnePhysique getAutrePersonneDuMenage(MenageCommun menageCommun, PersonnePhysique personne) throws EvenementCivilException {

		final RapportEntreTiers rapportAutrePersonne = getAppartenanceAuMenageAutrePersonne(menageCommun, personne);
		if (rapportAutrePersonne == null) {
			return null;
		}

		return (PersonnePhysique) context.getTiersDAO().get(rapportAutrePersonne.getSujetId());
	}

	/**
	 * Recherche l'autre personne faisant partie du ménage commun et retourne son rapport au ménage actif.
	 *
	 * @param menageCommun
	 *            le ménage commun à analyser
	 * @param personne
	 *            la personne de référence du ménage commun
	 * @return le rapport au ménage actif de l'autre personne du ménage, ou null si le ménage ne possède qu'une personne
	 * @throws EvenementCivilException
	 *             si plus d'une autre personne est trouvée (ménage à trois, ah là là...)
	 */
	private RapportEntreTiers getAppartenanceAuMenageAutrePersonne(MenageCommun menageCommun, PersonnePhysique personne)
			throws EvenementCivilException {

		RapportEntreTiers appartenanceAutrePersonne = null;

		final Set<RapportEntreTiers> rapportsObjet = menageCommun.getRapportsObjet();
		for (RapportEntreTiers rapportObjet : rapportsObjet) {
			if (!rapportObjet.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapportObjet.getType()
					&& rapportObjet.getDateFin() == null) {

				if (!rapportObjet.getSujetId().equals(personne.getId())) {
					if (appartenanceAutrePersonne != null) {
						final String message = String.format("Plus d'un conjoint trouvé pour le ménage commun [%s]", menageCommun);
						throw new EvenementCivilException(message);
					}
					appartenanceAutrePersonne = rapportObjet;
				}
			}
		}
		return appartenanceAutrePersonne;
	}

	/**
	 * Recherche le menage commun actif auquel est rattaché une personne
	 *
	 * @param personne
	 *            la personne potentiellement rattachée à un ménage commun
	 * @return le ménage commun trouvé, ou null si cette personne n'est pas rattaché au ménage.
	 * @throws EvenementCivilException
	 *             si plus d'un ménage commun est trouvé.
	 */
	private MenageCommun getMenageCommunActif(PersonnePhysique personne) throws EvenementCivilException {

		if (personne == null) {
			return null;
		}

		MenageCommun menageCommun = null;

		final Set<RapportEntreTiers> rapportsSujet = personne.getRapportsSujet();
		if (rapportsSujet != null) {
			for (RapportEntreTiers rapportSujet : rapportsSujet) {
				if (!rapportSujet.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapportSujet.getType()
						&& rapportSujet.getDateFin() == null) {
					/*
					 * le rapport de l'apartenance a été trouvé, on en déduit donc le tiers ménage
					 */
					if (menageCommun != null) {
						throw new EvenementCivilException("Plus d'un ménage commun trouvé pour la personne = ["
								+ personne.toString() + ']');
					}
					menageCommun = (MenageCommun) context.getTiersDAO().get(rapportSujet.getObjetId());
				}
			}
		}

		return menageCommun;
	}

	/**
	 * Cherche dans les rapport appartenance ménage, l'existence d'un ménage commun auquel auraient appartenu les deux contribuables.
	 *
	 * @param habitantPrincipal
	 *            le membre principal du ménage
	 * @param habitantConjoint
	 *            son conjoint
	 * @return le ménage commun trouvé, ou null si aucun trouvé.
	 */
	private MenageCommun getAncienMenageCommun(PersonnePhysique principal, PersonnePhysique conjoint) throws EvenementCivilException {

		MenageCommun ancienMenage = null;

		final Set<RapportEntreTiers> rapportsSujet = principal.getRapportsSujet();
		if (rapportsSujet != null) {
			for (RapportEntreTiers rapport : rapportsSujet) {
				if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType()
						&& rapport.getDateFin() != null) {

					final MenageCommun menage = (MenageCommun) context.getTiersDAO().get(rapport.getObjetId());

					final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(menage, rapport.getDateDebut());
					if (couple != null && couple.estComposeDe(principal, conjoint)) {

						if (ancienMenage != null) {
							final String message;
							if (conjoint != null) {
								message = "Plus d'un ménage commun trouvé pour les contribuables [" + principal.getNumero() + ", " + conjoint.getNumero() + ']';
							}
							else {
								message = "Plus d'un ménage commun trouvé pour le contribuable [" + principal.getNumero() + ']';
							}

							throw new EvenementCivilException(message);
						}
						ancienMenage = menage;
					}
				}
			}
		}
		return ancienMenage;
	}

	protected static boolean isDansLeCanton(Commune commune) {
		return commune != null && commune.isVaudoise();
	}
}
