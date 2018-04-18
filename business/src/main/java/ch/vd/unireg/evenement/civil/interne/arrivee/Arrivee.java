package ch.vd.unireg.evenement.civil.interne.arrivee;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.interne.mouvement.Mouvement;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.indexer.TooManyResultsIndexerException;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.EtatCivilList;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Modélise un événement d'arrivée.
 */
public abstract class Arrivee extends Mouvement {

	protected static Logger LOGGER = LoggerFactory.getLogger(Arrivee.class);

	protected Arrivee(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		Assert.isTrue(isEvenementArrivee(evenement.getType()));
	}

	protected Arrivee(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
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
    @SuppressWarnings("null")
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		/*
		 * Le retour du mort-vivant
		 */
        final Individu indiv = getIndividu();
        if (indiv == null) {
            erreurs.addErreur("individu requis pour traiter une arrivée");
            return;
        }
        if (indiv.getDateDeces() != null) {
			erreurs.addErreur("L'individu est décédé");
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
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
	@NotNull
	protected final HandleStatus handleIndividuSeul(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		if (isArriveeRedondantePourIndividuSeul()) {
			return HandleStatus.REDONDANT;
		}

		try {
			final Individu individu = getIndividu();
			final RegDate dateArrivee = getDateArriveeEffective(getDate());
			final Long numeroEvenement = getNumeroEvenement();

			/*
			 * Création à la demande de l'habitant
			 */
			// [UNIREG-770] rechercher si un Non-Habitant assujetti existe (avec Nom - Prénom)
			final PersonnePhysique habitant = getOrCreatePersonnePhysique(individu, numeroEvenement);

			verifierPresenceDecisionEnCours(habitant,dateArrivee);

			// [SIFISC-6841] on met-à-jour le flag habitant en fonction de ses adresses de résidence civiles
			updateHabitantStatus(habitant, dateArrivee);

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

			return HandleStatus.TRAITE;
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
	 * @return <code>true</code> si l'arrivée de cet individu seul a déjà été traitée
	 */
	protected abstract boolean isArriveeRedondantePourIndividuSeul();

	/**
	 * @return <code>true</code> si l'arrivée de cet individu en ménage a déjà été traitée
	 */
	protected abstract boolean isArriveeRedondantePourIndividuEnMenage();

	/**
	 * @return <code>true</code> si l'arrivée de cet individu en ménage est antérieur à une arrivée déjà traitée pour l'autre conjoint
	 * Se detecte par la presence de for ouvert après l'arrivée à traiter  sur le ménage avec comme motif d'ouverture arrivée HS ou HC
	 */
	protected abstract boolean isArriveeRedondanteAnterieurPourIndividuEnMenage();


	/**
	 * @return <code>true</code> si le conjoint de l'individu qui arrive correspond à un contribuable Marié seul dans unireg
	 */
	protected abstract boolean isConjointMarieSeul();

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

	private Map.Entry<ModeDetection,List<PersonnePhysique>> findNonHabitants(Individu individu) throws EvenementCivilException {
		return findNonHabitants(getService(), individu, getNumeroEvenement());
	}

	/**
	 * [UNIREG-3073] Recherche un ou plusieurs non-habitants à partir du prénom, du nom, de la date de naissance et du sexe d'un individu.
	 * [UNIREG-770] le non habitant doit être assujetti
	 * [SIFISC-9279] on abandonne cette histoire d'assujettissement mais il faut filtrer les annulés, désactivés et i107...
	 * [SIFISC-11358] on introduit la recherche par le navs13
	 * @param individu                    un individu
	 * @return une liste de non-habitants qui correspondent aux critères.
	 */
	protected static Map.Entry<ModeDetection,List<PersonnePhysique>> findNonHabitants(TiersService tiersService, Individu individu, @Nullable Long numeroEvenement) throws EvenementCivilException {

		// les critères de recherche
		final List<PersonnePhysique> nonHabitants = new ArrayList<>();
		final ModeDetection modeDetection;
		final String nomPrenom = tiersService.getNomPrenom(individu);
		final RegDate dateNaissance = individu.getDateNaissance();
		final Sexe sexe = individu.getSexe();
		final String navs13 = individu.getNouveauNoAVS();
		final TiersCriteria criteria = getTiersCriteriaComplet(nomPrenom, dateNaissance, sexe);
		if (navs13 != null) {
			//Recherche par navs13
			final TiersCriteria criteriaNavs13 = getTiersCriteriaNavs13(navs13);
			final List<PersonnePhysique> resultNavs13 = searchPersonne(tiersService, numeroEvenement, nomPrenom, criteriaNavs13);
			if (resultNavs13.isEmpty()) {
				//aucun resultat trouvé on lance la recherche par les autres critères
				final List<PersonnePhysique> resultComplet = searchPersonne(tiersService, numeroEvenement, nomPrenom, criteria);
				if (resultComplet.isEmpty() || resultComplet.size() != 1 || candidatAvecNavs13Conforme(navs13, resultComplet)) {
					//Aucun, plusieurs conadidats  ou 1 contribuable avec navs 13 conforme: on retourne le resultat trouvé
					nonHabitants.addAll(resultComplet);
					modeDetection = ModeDetection.COMPLET;
				}
				else {
					//le seul candidat trouvé n'a pas le même numéro AVS que celui del'individu de l'évènement
					PersonnePhysique candidat = resultComplet.get(0);
					throw new EvenementCivilException(String.format("Le non-habitant trouvé (%s) a un numero d'assure social qui ne correspond pas à celui de l'individu de l'évènement",
							FormatNumeroHelper.numeroCTBToDisplay(candidat.getNumero())));
				}


			}
			else {
				//La recherche par NAVS13 est poistive, on retourne le resultat
				nonHabitants.addAll(resultNavs13);
				modeDetection = ModeDetection.NAVS13;
			}
		}
		else {

			nonHabitants.addAll(searchPersonne(tiersService, numeroEvenement, nomPrenom, criteria));
			modeDetection = ModeDetection.COMPLET;
		}

		return new AbstractMap.SimpleEntry<>(modeDetection, nonHabitants);
	}

	/**Determine si la liste des personnes physiques passée en paramètre contient
	 * un membre avec un navs13 null ou equivalent à celui passé en paramètre
	 *
	 * @param navs13 de l'individu concerné par l'évenement
	 * @param candidats la liste des nonhabitants trouvé
	 * @return <b>True</b> si on a trouve une personne avec le bon navs13, <b>False</b> sinon
	 */
	private static boolean candidatAvecNavs13Conforme(String navs13, List<PersonnePhysique> candidats) {
		if (!candidats.isEmpty() && candidats.size() == 1) {
			PersonnePhysique candidat = candidats.get(0);
			return candidat.getNumeroAssureSocial()==null || navs13.equals(candidat.getNumeroAssureSocial());
		}
		return false;

	}

	/**Créé un objet critère pour l'indexeur a partir des informations concerant l'individu renseigné dans l'evenement
	 *
	 * @param nomPrenom nom et prenom de l'individu
	 * @param dateNaissance la date de naissance d el'individuu
	 * @param sexe le sexe de l'individu
	 * @return un objet critere
	 */
	private static TiersCriteria getTiersCriteriaComplet(String nomPrenom, RegDate dateNaissance, Sexe sexe) {
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.NON_HABITANT);
		criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.EST_EXACTEMENT);
		criteria.setNomRaison(nomPrenom);
		criteria.setSexeOrNull(sexe);
		criteria.setDateNaissanceOrNull(dateNaissance);
		criteria.setInclureI107(false);
		criteria.setInclureTiersAnnules(false);
		return criteria;
	}

	/**Créé un objet critère pour l'indexeur a partir du navs13 de l'individu renseigné dans l'evenement
	 *
	 * @param navs13 de l'individu
	 * @return un objet critère
	 */
	private static TiersCriteria getTiersCriteriaNavs13(String navs13) {
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.NON_HABITANT);
		criteria.setNavs13(navs13);
		criteria.setInclureI107(false);
		criteria.setInclureTiersAnnules(false);
		return criteria;
	}

	/**
	 * Recherche des personnes physiques dans l'indexeur
	 * @param tiersService service des tiers
	 * @param numeroEvenement numero de l'évènement
	 * @param nomPrenom le nom et premon de l'individu
	 * @param criteria les critères de recherche
	 * @return une liste de personne correspondantes aux critères
	 * @throws EvenementCivilException
	 */
	private static List<PersonnePhysique> searchPersonne(TiersService tiersService, Long numeroEvenement, String nomPrenom, TiersCriteria criteria) throws EvenementCivilException {
		final List<TiersIndexedData> results;
		try {
		    results = tiersService.search(criteria);
		}
		catch (TooManyResultsIndexerException e) {
		    // [SIFISC-4876] On catch cette runtime exception de l'indexeur pour fournir à l'utilisateur un message plus parlant
		    throw new EvenementCivilException (String.format("Trop de non-habitants (%d au total) correspondent à: %s", e.getNbResults(), nomPrenom), e);
		}

		final List<PersonnePhysique> nonHabitants = new ArrayList<>();
		for (final TiersIndexedData tiersIndexedData : results) {
			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(tiersIndexedData.getNumero());
			// [UNIREG-1603] on filtre les non habitants qui possèdent un numéro d'individu
			if (pp.getNumeroIndividu() != null) {
				Audit.warn(numeroEvenement, "Candidat " + pp.getNumero() + " écarté en raison de la présence d'un autre numéro d'individu");
				continue;
			}
			nonHabitants.add(pp);
		}

		return nonHabitants;
	}

	@NotNull
	private PersonnePhysique getOrCreatePersonnePhysique(Individu individu, long evenementId) throws EvenementCivilException {

		final PersonnePhysique pp = context.getTiersDAO().getPPByNumeroIndividu(individu.getNoTechnique());
		final PersonnePhysique personnePhysiqueResultante;
		if (pp != null) {
			personnePhysiqueResultante = pp;
		}
		else {
			final Map.Entry<ModeDetection,List<PersonnePhysique>> resultat = findNonHabitants(individu);
			final List<PersonnePhysique> nonHabitants = resultat.getValue();
			final ModeDetection modeDetection = resultat.getKey();
			if (nonHabitants.size() == 1) {
				final PersonnePhysique candidat = nonHabitants.get(0);
				if (modeDetection == ModeDetection.NAVS13 || candidatAvecDateNaissanceEtSexe(candidat)) {
					//SIFISC-12951 Si un seul tiers trouvé avec le navs13, on prend directement ce tiers même si d'autres valeurs comme la date de naissance ou le sexe sont manquantes.
					// OU [UNIREG-1603] le candidat correspond parfaitement aux critères
					personnePhysiqueResultante = candidat;
					// Meme si on ne passe pas en Habitant le conjoint, on fait le lien avec le civil (Decision prise suite au traitement de SIFISC-6109)
					personnePhysiqueResultante.setNumeroIndividu(individu.getNoTechnique());
				}
				else {
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
			}
			else if (nonHabitants.isEmpty()) {
				// Ici on créé un non-habitant et on l'initialise avec les données connues de son individu. C'est un peu spécial mais le problème c'est qu'on
				// est entrain de traiter l'arrivée du principal, et que l'on ne veut pas aussi traiter l'arrivée du conjoint. La solution, c'est de créer le
				// conjoint non-habitant.
				personnePhysiqueResultante = context.getTiersService().createNonHabitantFromIndividu(individu.getNoTechnique());
				Audit.info(evenementId, "Un tiers non-habitant a été créé pour le nouvel arrivant");
			}
			else {
				// [UNIREG-2650] Message d'erreur un peu plus explicite...
				final StringBuilder b = new StringBuilder();
				boolean first = true;
				final List<Long> numerosCtb = new ArrayList<>();
				for (PersonnePhysique nonHabitant : nonHabitants) {
					numerosCtb.add(nonHabitant.getNumero());
				}
				Collections.sort(numerosCtb);
				for (long numero : numerosCtb) {
					if (!first) {
						b.append(", ");
					}
					b.append(FormatNumeroHelper.numeroCTBToDisplay(numero));
					first = false;
				}

				final String message = String.format("Plusieurs tiers non-habitants assujettis potentiels trouvés (%s)", b.toString());
				throw new EvenementCivilException(message);
			}
		}

		return personnePhysiqueResultante;
	}

	private boolean candidatAvecDateNaissanceEtSexe(PersonnePhysique candidat){
		return candidat.getDateNaissance() != null && candidat.getSexe() != null;
	}

	private static RegDate findDateDebutMenageAvant(Individu individu, RegDate limiteSuperieureEtDefaut) {
		final EtatCivilList etatsCivils = individu.getEtatsCivils();
		final List<EtatCivil> ecList = etatsCivils != null ? etatsCivils.asList() : null;
		final RegDate dateDebutMenage;
		if (ecList != null && !ecList.isEmpty()) {
            RegDate candidate = limiteSuperieureEtDefaut;
			for (EtatCivil etatCivil : CollectionsUtils.revertedOrder(ecList)) {
				if (EtatCivilHelper.estMarieOuPacse(etatCivil)) {
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
	@NotNull
	protected final HandleStatus handleIndividuEnMenage(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		if (isConjointMarieSeul()) {
			long numeroIndividu = getNoIndividu();
			String message = String.format("Le conjoint de l'individu (n° %s) correspond à un(e) marié(e) seul",numeroIndividu);
			throw new EvenementCivilException(message);
		}

		if (isArriveeRedondanteAnterieurPourIndividuEnMenage()) {
			String stringDateArrivee = RegDateHelper.dateToDashString(getDate());
			long numeroIndividu = getNoIndividu();
			String message = String.format("la date d'arrivée (%s) de l'individu (n° %s) est antérieure à l'arrivée de son menage commun",stringDateArrivee,numeroIndividu);

			throw new EvenementCivilException(message);
		}

		final Individu individu = getIndividu();
		Assert.notNull(individu); // prérequis

		// [UNIREG-2212] Il faut décaler la date du for en cas d'arrivée vaudoise après le 20 décembre
		final RegDate dateEvenement = getDateArriveeEffective(getDate());
		final Long numeroEvenement = getNumeroEvenement();

		final Individu conjoint = context.getServiceCivil().getConjoint(getNoIndividu(), getDate());

		if (isArriveeRedondantePourIndividuEnMenage()) {
			Audit.info(getNumeroEvenement(), "Arrivée considérée comme redondante fiscalement, ré-évaluation du flag habitant");
			updateHabitantStatus(getPrincipalPP(), dateEvenement);
			return HandleStatus.REDONDANT;
		}

		/*
		 * Récupération/création des habitants
		 */
		final PersonnePhysique arrivant = getOrCreatePersonnePhysique(individu, numeroEvenement);
		//[SIFISC-12624]
		//Si une décision aci en cours est présente, on met l'évenement en erreur
		verifierPresenceDecisionEnCours(arrivant,dateEvenement);
		verifierPresenceDecisionsEnCoursSurCouple(arrivant);

		// [SIFISC-6841] on met-à-jour le flag habitant en fonction de ses adresses de résidence civiles
		updateHabitantStatus(arrivant, dateEvenement);

		final PersonnePhysique conjointDeLArrivant;
		if (conjoint != null) {
			conjointDeLArrivant = getOrCreatePersonnePhysique(conjoint, numeroEvenement);
			verifierPresenceDecisionEnCours(conjointDeLArrivant,arrivant,dateEvenement);
		}
		else {
			conjointDeLArrivant = null;
		}

		/*
		 * Récupération/création du ménage commun et du rapports entre tiers
		 */

		// trouve la date du mariage (qui peut avoir eu lieu bien avant l'arrivée !)
		final RegDate dateDebutMenage = findDateDebutMenageAvant(individu, getDate());
		 // Fermeture de l'eventuel for personnel ouvert en dehors du canton.
		if (contribuableAUnForFiscalPrincipalPersonnelOuvertEnDehorsDuCanton(arrivant)) {
			context.getTiersService().closeForFiscalPrincipal(arrivant, dateDebutMenage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}
		if (conjointDeLArrivant != null && contribuableAUnForFiscalPrincipalPersonnelOuvertEnDehorsDuCanton(conjointDeLArrivant)) {
			context.getTiersService().closeForFiscalPrincipal(conjointDeLArrivant, dateDebutMenage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		}

		final MenageCommun menageCommun = getOrCreateMenageCommun(arrivant, conjointDeLArrivant, dateEvenement, dateDebutMenage, numeroEvenement);
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

		return HandleStatus.TRAITE;
	}

	/**
	 *
	 * @return true si le contribuable en paramètre possède un for fiscal personnel ouvert en dehors du canton
	 */
	private boolean contribuableAUnForFiscalPrincipalPersonnelOuvertEnDehorsDuCanton(PersonnePhysique ctb) {
		if (ctb == null) {
			return false;
		}
		ForFiscalPrincipal ffp = ctb.getForFiscalPrincipalAt(null);
		return ffp != null && ffp.getDateFin() == null && ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
	}

	/**
	 * @param habitantPrincipal .
	 * @param habitantConjoint .
	 * @param dateEvenement date de l'événement d'arrivée
	 * @param dateDebutMenage date de début des nouveaux rapports d'appartenance ménage en cas de création de ménage
	 * @param evenementId ID technique de l'événement d'arrivée
	 * @return le ménage commun avec le contribuable principal et le conjoint comme parties.
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
				 *
                 * Vérification que l'on ajoute pas un deuxième conjoint
                 */
				final PersonnePhysique autrePersonne = getAutrePersonneDuMenage(menageCommunHabitantPrincipal, habitantPrincipal);
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
					final RapportEntreTiers rapport = getService().addTiersToCouple(menageCommunHabitantPrincipal, habitantConjoint, dateDebutMenage, null);

					menageCommun = (MenageCommun) context.getTiersDAO().get(rapport.getObjetId());
					Audit.info(evenementId, String.format("L'arrivant [%d] a été attaché au ménage commun [%d] déjà existant", habitantConjoint.getNumero(), menageCommun.getNumero()));
				}
				else {
					menageCommun = menageCommunHabitantPrincipal;
				}

			}
			else {
				Assert.notNull(habitantConjoint);
				Assert.notNull(menageCommunHabitantConjoint);

				/*
				 * Le conjoint appartient déjà à un ménage => on vérifie que le ménage en question ne possède bien qu'un seul membre actif
				 * et on rattache l'individu principal au ménage
                 *
                 * Vérification que l'on ajoute pas un deuxième individu principal
                 */
				final PersonnePhysique autrePersonne = getAutrePersonneDuMenage(menageCommunHabitantConjoint, habitantConjoint);
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

				final RapportEntreTiers rapport = getService().addTiersToCouple(menageCommunHabitantConjoint, habitantPrincipal, rapportExistant.getDateDebut(), null);
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
	@Nullable
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
	@Nullable
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
	@Nullable
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
	 * @param principal le membre principal du ménage
	 * @param conjoint son conjoint
	 * @return le ménage commun trouvé, ou null si aucun trouvé.
	 */
	@Nullable
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

	private enum ModeDetection{
		NAVS13,
		COMPLET
	}
}
