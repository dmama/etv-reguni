package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.mouvement.Mouvement;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersException;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Evénement de départ d'un individu dans les cas suivants:
 * <ul>
 * <li>DEPART_SECONDAIRE : déménagement d'une commune vaudoise à l'autre (intra-cantonal) pour l'adresse secondaire</li>
 * <li>DEPART_COMMUNE : déménagement d'un canton à l'autre (inter-cantonal) ou Départ de Suisse</li>
 * </ul>
 */
public abstract class Depart extends Mouvement {

	/**
	 * LOGGER log4J
	 */
	protected static Logger LOGGER = LoggerFactory.getLogger(Depart.class);

	protected Commune nouvelleCommune;
	protected Localisation nouvelleLocalisation;

	private final Pays paysInconnu;

	/**
	 * Indique si l'evenement est un ancien type de départ
	 */
	private boolean isAncienTypeDepart = false;

	protected Depart(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

		if (evenement.getNumeroIndividuConjoint() != null) {
			isAncienTypeDepart = true;
		}

		try {
			this.paysInconnu = context.getServiceInfra().getPaysInconnu();
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}

	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Depart(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Adresse ancienneAdresse, Adresse nouvelleAdresse, Commune nouvelleCommune,
	                 EvenementCivilContext context, boolean isRegPP) throws EvenementCivilException {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, nouvelleAdresse, null, null, context);
		this.paysInconnu = context.getServiceInfra().getPaysInconnu();
		this.nouvelleCommune = nouvelleCommune;
		this.nouvelleLocalisation = computeNouvelleLocalisation(ancienneAdresse, nouvelleAdresse, nouvelleCommune);

		//SIFISC-4230 Pour les evenements regPP, les départs vaudois doivent partir en erreur
		if (isDepartVaudois() && isRegPP) {
			throw new EvenementCivilException("La nouvelle commune est toujours dans le canton de Vaud");
		}
	}

	public Depart(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);

		try {
			this.paysInconnu = context.getServiceInfra().getPaysInconnu();
		}
		catch (ServiceInfrastructureException e) {
			throw new EvenementCivilException(e);
		}
	}


	/**
	 * Traitement spécifique du départ dans les fors
	 * @param warnings       collecteur des messages de warning
	 * @param pp             personne physique désignée par l'événement civil
	 * @param ctb            contribuable concerné par le départ
	 * @param dateFermeture  date de fermeture des fors
	 * @param motifFermeture motif de fermeture des fors à fermer
	 * @return la date de fermeture effectivement prise en compte pour le for
	 * @throws EvenementCivilException en cas de problème
	 */
	protected abstract RegDate doHandleFermetureFors(EvenementCivilWarningCollector warnings, PersonnePhysique pp, ContribuableImpositionPersonnesPhysiques ctb, RegDate dateFermeture, MotifFor motifFermeture) throws EvenementCivilException;

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		if (isDepartVaudois()) { // on ignore les départs vaudois car dans ce cas c'est l'arrivée qui fait foi
			Audit.info(getNumeroEvenement(), "Départ vaudois -> ignoré.");
			return HandleStatus.TRAITE;
		}

		final PersonnePhysique pp = getPrincipalPP();
		if (pp == null) {
			// si on ne connaissait pas le gaillard, c'est un problème
			throw new EvenementCivilException("Aucun habitant (ou ancien habitant) trouvé avec numéro d'individu " + getNoIndividu());
		}

		final MotifFor motifFermeture = findMotifFermetureFor();
		final RegDate dateFermeture = findDateFermeture(this, motifFermeture == MotifFor.DEMENAGEMENT_VD);
		final ContribuableImpositionPersonnesPhysiques contribuable = findContribuable(dateFermeture, pp);

		final RegDate dateFermetureEffective = doHandleFermetureFors(warnings, pp, contribuable, dateFermeture, motifFermeture);

		// [SIFISC-6841] on met-à-jour le flag habitant en fonction de ses adresses de résidence civiles
		updateHabitantStatus(pp, dateFermetureEffective.getOneDayAfter());
		if (isAncienTypeDepart) {
			updateHabitantStatus(getConjointPP(), getNoIndividuConjoint(), dateFermetureEffective.getOneDayAfter());
		}

		return HandleStatus.TRAITE;
	}

	/**
	 * Détermine si l'habitant est seul ou en ménage et renvoi dans ce cas son ménage.
	 *
	 * @param date           la date de référence
	 * @param pp             une personne physique
	 * @return le contribuable concerné par le déménagement
	 */
	private ContribuableImpositionPersonnesPhysiques findContribuable(RegDate date, PersonnePhysique pp) {
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(pp, date);
		if (couple != null) {
			final MenageCommun menage = couple.getMenage();
			if (menage != null) {
				return menage;
			}
		}
		return pp;
	}

	/**
	 * [UNIREG-2212] Calcule la date de fermeture du for fiscal principal en fonction de la date de départ
	 *
	 * @param depart         un événement de départ
	 * @param demenagementVD <b>vrai</b> s'il s'agit d'un déménagement entre commune vaudoise; <b>false</b> autrement.
	 * @return la date de fermeture du for fiscal
	 */
	private RegDate findDateFermeture(Depart depart, boolean demenagementVD) {

		final RegDate dateFermeture;
		if (demenagementVD) {
			dateFermeture = FiscalDateHelper.getDateFermetureForFiscal(depart.getDate());
		}
		else {
			dateFermeture = depart.getDate();
		}

		return dateFermeture;
	}

	/**
	 * calcule le motif de fermeture du for fiscal
	 *
	 * @return le motif de fermeture
	 */
	protected MotifFor findMotifFermetureFor() {
		MotifFor motifFermeture;

		// Départ vers l'etranger
		if (isPaysEstConnu()) {
			if (!estEnSuisse()) {
				motifFermeture = MotifFor.DEPART_HS;
			}
			else if (!nouvelleCommune.isVaudoise()) {
				motifFermeture = MotifFor.DEPART_HC;
			}
			else {
				// on ne devrait jamais avoir ce cas pour un départ: c'est un déménagement
				// Audit.warn("La commune de destination est dans le canton de Vaud une erreur a du se produire");
				/*
				 * msi (16.09.2008) : en fait, il s'agit d'un cas valide où un contribuable bénéficiant d'un arrangement fiscal (= for
				 * principal ouvert sur une résidence secondaire dans le canton) quitte sa résidence secondaire pour sa résidence principale
				 * elle-même située dans le canton : dans ce cas il s'agit bien d'un départ de la résidence secondaire, mais il se traduit
				 * par un déménagement vaudois.
				 */
				motifFermeture = MotifFor.DEMENAGEMENT_VD;
			}
		}
		else {
			Audit.warn("Le pays de destination est inconnu");
			motifFermeture = MotifFor.DEPART_HS;
		}

		return motifFermeture;
	}

	/**
	 * Determine si la nouvelle adresse est en suisse
	 *
	 * @return true si la nouvelle adresse se situe en suisse, false sinon
	 */

	protected boolean estEnSuisse() {
		return (getNouvelleLocalisation() != null && getNouvelleLocalisation().getType() != LocalisationType.HORS_SUISSE);
	}

	/**
	 * determine si le le numéro ofs du pays est renseigné
	 *
	 * @return true si on a numéro ofs, false sinon
	 */
	protected boolean isPaysEstConnu() {
		return (getNouvelleLocalisation() != null && getNouvelleLocalisation().getNoOfs() != null);
	}

	/** Determine si une destination n'a pas de numéro ofs indiquée.
	 *
	 * @return true si on pas de numéro ofs false sinon
	 */
	protected boolean isDestinationNontIdentifiable(){
		return (getNouvelleLocalisation() != null && getNouvelleLocalisation().getNoOfs() == null);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		if (!isDepartVaudois()) {

			// [SIFISC-1918] Pour un départ, si la date est à la date du jour, on doit partir en erreur (l'événement sera re-traité plus tard)
			// car la nouvelle adresse ne commence que demain, et les adresses qui commencent dans le futur sont ignorées (voir SIFISC-35)

			if (getDate().equals(RegDate.get())) {
				erreurs.addErreur("Un départ HC/HS ne peut être traité qu'à partir du lendemain de sa date d'effet");
			}

			/*
			 * Le Depart du mort-vivant
			 */
			final Individu individu = getIndividu();
			if (individu == null) {
				erreurs.addErreur("L'individu est null");
			} else if (individu.getDateDeces() != null) {
				erreurs.addErreur("L'individu est décédé");
			}

			/**
			 * La commune d'annonce est nécessaire
			 */
			if (getNumeroOfsEntiteForAnnonce() == null) {
				erreurs.addErreur("La commune d'annonce n'est pas renseignée");
			}

			/**
			 * La destination du départ est non identifiable
			 */
			if (isDestinationNontIdentifiable()) {
				erreurs.addErreur("La destination de départ n'est pas identifiable car le numéro OFS de destination n'est pas renseigné");
			}
		}

		final PersonnePhysique ppPrincipale = getPrincipalPP();
		EnsembleTiersCouple etc = context.getTiersService().getEnsembleTiersCouple(ppPrincipale, getDate().getOneDayBefore());
		PersonnePhysique conjoint =null;
		if (etc != null) {
			conjoint = etc.getConjoint(ppPrincipale);
		}

		verifierPresenceDecisionEnCours(ppPrincipale,getDate());
		verifierPresenceDecisionsEnCoursSurCouple(ppPrincipale);
		if (conjoint != null) {
			verifierPresenceDecisionEnCours(conjoint,ppPrincipale,getDate());

		}



	}

	/**
	 * Permet de valider la cohérence d'une adresse fournie par l'événement départ
	 *
	 * @param adresse l'adresse à valider
	 * @param commune commune à valider
	 * @param erreurs buffer d'erreur
	 */
	protected void validateCoherenceAdresse(Adresse adresse, Commune commune, EvenementCivilErreurCollector erreurs) {
		if (!isDepartVaudois()) {

			if (adresse == null) {
				erreurs.addErreur("Adresse de résidence avant départ inconnue");
			}
			else if (adresse.getDateFin() == null) {
				erreurs.addErreur("La date de fin de validité de la résidence est inconnue");
			}
			// la date de départ est differente de la date de fin de validité de l'adresse
			else if (!getDate().equals(adresse.getDateFin())) {
				erreurs.addErreur("La date de départ est différente de la date de fin de validité de l'adresse dans le canton");
			}

			// La commune d'annonce est differente de la commune de résidence avant l'évenement
			// de départ
			if (commune != null) {
				if ((!commune.isFraction() && commune.getNoOFS() != getNumeroOfsEntiteForAnnonce()) ||
						(commune.isFraction() && commune.getOfsCommuneMere() != getNumeroOfsEntiteForAnnonce())) {
					erreurs.addErreur("La commune d'annonce est differente de la dernière commune de résidence");
				}
			}
		}
	}

	/**
	 * Permet d'ouvrir un for principal sur une commune hors canton
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param modeImposition           le mode d'imposition du for fiscal principal
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipal openForFiscalPrincipalHC(ContribuableImpositionPersonnesPhysiques contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale, ModeImposition modeImposition, MotifFor motifOuverture) {
		return getService().openForFiscalPrincipal(contribuable, dateOuverture, MotifRattachement.DOMICILE, numeroOfsAutoriteFiscale, TypeAutoriteFiscale.COMMUNE_HC, modeImposition, motifOuverture);
	}

	/**
	 * Permet d'ouvrir un for principal sur un pays
	 *
	 * @param contribuable             le contribuable sur lequel le nouveau for est ouvert
	 * @param dateOuverture            la date à laquelle le nouveau for est ouvert
	 * @param numeroOfsAutoriteFiscale le numéro OFS de l'autorité fiscale sur laquelle est ouverte le nouveau fort.
	 * @param modeImposition           le mode d'imposition du for fiscal principal
	 * @param motifOuverture           le motif d'ouverture du for fiscal principal
	 * @return le nouveau for fiscal principal
	 */
	protected ForFiscalPrincipal openForFiscalPrincipalHS(ContribuableImpositionPersonnesPhysiques contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale, ModeImposition modeImposition, MotifFor motifOuverture) {
		return getService().openForFiscalPrincipal(contribuable, dateOuverture, MotifRattachement.DOMICILE, numeroOfsAutoriteFiscale, TypeAutoriteFiscale.PAYS_HS, modeImposition, motifOuverture);
	}

	protected static ModeImposition determineModeImpositionDepartHCHS(ContribuableImpositionPersonnesPhysiques contribuable, RegDate dateFermeture, ForFiscalPrincipalPP ffp, TiersService tiersService) throws EvenementCivilException {

		Assert.notNull(ffp);

		final ModeImposition modeImpositionAncien = ffp.getModeImposition();
		final ModeImposition modeImposition;
		if (isSourcierPur(modeImpositionAncien)) {
			// Un sourcier pur reste à la source.
			modeImposition = ModeImposition.SOURCE;
		}
		else if (isSourcierMixte(modeImpositionAncien)) {
			// [UNIREG-1849] passe à l'ordinaire si for secondaire, sinon à la source
			modeImposition = determineOrdinaireOuSourceSelonPresenceForSecondaire(contribuable, dateFermeture.getOneDayAfter());
		}
		else if (modeImpositionAncien == ModeImposition.DEPENSE && isEtrangerSansPermisC(contribuable, dateFermeture.getOneDayAfter(), tiersService)) {
			// [SIFISC-7965] passe à l'ordinaire si for secondaire, sinon à la source
			modeImposition = determineOrdinaireOuSourceSelonPresenceForSecondaire(contribuable, dateFermeture.getOneDayAfter());
		}
		else {
			// tous les autres cas passent à l'ordinaire (ordinaire, dépense avec permis C, indigent...)
			modeImposition = ModeImposition.ORDINAIRE;
		}
		return modeImposition;
	}

	private static ModeImposition determineOrdinaireOuSourceSelonPresenceForSecondaire(Contribuable ctb, RegDate dateReference) {
		// [UNIREG-1849] passe à l'ordinaire si for secondaire, sinon à la source
		final List<ForFiscal> ffs = ctb.getForsFiscauxValidAt(dateReference);
		boolean hasForSecondaire = false;
		for (ForFiscal ff : ffs) {
			if (ff instanceof ForFiscalSecondaire) {
				hasForSecondaire = true;
				break;
			}
		}
		return hasForSecondaire ? ModeImposition.ORDINAIRE : ModeImposition.SOURCE;
	}

	private static boolean isSourcierPur(ModeImposition modeImposition) {
		return ModeImposition.SOURCE == modeImposition;
	}

	private static boolean isEtrangerSansPermisC(Contribuable ctb, RegDate dateReference, TiersService tiersService) throws EvenementCivilException {
		try {
			if (ctb instanceof PersonnePhysique) {
				return tiersService.isEtrangerSansPermisC((PersonnePhysique) ctb, dateReference);
			}
			else if (ctb instanceof MenageCommun) {
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, dateReference);
				final PersonnePhysique prn = couple.getPrincipal();
				final PersonnePhysique sec = couple.getConjoint();
				if (prn != null && sec != null) {
					return tiersService.isEtrangerSansPermisC(prn, dateReference) && tiersService.isEtrangerSansPermisC(sec, dateReference);
				}
				else if (prn != null) {
					return tiersService.isEtrangerSansPermisC(prn, dateReference);
				}
				else if (sec != null) {
					// est-ce seulement possible ?
					return tiersService.isEtrangerSansPermisC(sec, dateReference);
				}
				else {
					// ménage sans individu -> que faire ?
					throw new EvenementCivilException(String.format("Ménage commun %d sans lien vers des personnes physique au %s : impossible de déterminer le mode d'imposition du for après le départ",
					                                                ctb.getNumero(), RegDateHelper.dateToDisplayString(dateReference)));
				}
			}
			else {
				// ni une personne physique ni un ménage commun -> que faire ?
				throw new EvenementCivilException(String.format("Contribuable %d n'est ni une personne physique ni un ménage commun", ctb.getNumero()));
			}
		}
		catch (TiersException e) {
			throw new EvenementCivilException(e);
		}
	}

	private static boolean isSourcierMixte(ModeImposition modeImposition) {
		return ModeImposition.MIXTE_137_1 == modeImposition || ModeImposition.MIXTE_137_2 == modeImposition;
	}

	protected static Commune findNouvelleCommuneByLocalisation(Localisation localisation, EvenementCivilContext context, RegDate dateDepart) throws EvenementCivilException {
		final Commune nouvelleCommune;
		final RegDate lendemain = dateDepart.getOneDayAfter();

		if (localisation != null && localisation.getType() != LocalisationType.HORS_SUISSE && localisation.getNoOfs() != null) {
			try {
				nouvelleCommune = context.getServiceInfra().getCommuneByNumeroOfs(localisation.getNoOfs(), lendemain);
			}
			catch (ServiceInfrastructureException e) {
				throw new EvenementCivilException(e);
			}
		}
		else {
			nouvelleCommune = null;
		}

		return nouvelleCommune;
	}

	/**
	 * Dans le cas du départ d'une personne seule, c'est la date du départ. Dans le cas du départ d'une personne en couple, l'idée est de renvoyer ici la prochaine date
	 * (égale ou postérieure à la date du départ en cours de traitement) à laquelle le conjoint est également parti (soit du canton, soit de sa résidence vaudoise
	 * au moment du départ traité)... Si un tel moment n'existe pas (ou pas encore), on retournera <i>null</i>.
	 */
	@Nullable
	protected RegDate getDateDepartComplet(final boolean includeSecondaire) throws EvenementCivilException {
		final PersonnePhysique habitant = getPrincipalPP();
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(habitant, getDate());
		if (couple != null) {
			final PersonnePhysique conjoint = couple.getConjoint(habitant);
			if (conjoint != null) {
				// il faut donc trouver un historique (postérieur ou égal à la date du départ traité) des lieux de résidence du conjoint
				final List<LieuResidence> lieuxResidencePrincipaleConjoint = getLieuxResidencePrincipale(conjoint);
				RegDate dateDepartComplet = getDateFromLieuxResidence(lieuxResidencePrincipaleConjoint);
				if (dateDepartComplet == null && includeSecondaire) {
					final List<LieuResidence> lieuxResidenceSecondaireConjoint = getLieuxResidenceSecondaire(conjoint);
					//Si le conjoint n'a pas de résidence secondaire, on ne fait rien
					if (!lieuxResidenceSecondaireConjoint.isEmpty()) {
						dateDepartComplet = getDateFromLieuxResidence(lieuxResidenceSecondaireConjoint);
					}

				}
				return dateDepartComplet;
			}
		}
		return getDate();
	}

	/**
	 * @param dateDepartEffectif date de départ effective (= date de fermeture du for d'avant)
	 * @return <code>true</code> si l'individu sur le départ est seul ou, s'il est en couple, si les types d'autorité fiscale des deux membres du couple au lendemain
	 * du départ sont les mêmes (si marié seul ou si un des deux types d'autorité fiscale est absent, on dira que les deux sont identiques)
	 */
	protected boolean isToutLeMondeAvecLeMemeTypeAutoriteFiscaleApresDepartPrincipal(RegDate dateDepartEffectif) throws EvenementCivilException {
		final PersonnePhysique partant = getPrincipalPP();
		final RegDate lendemainDepart = dateDepartEffectif.getOneDayAfter();
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(partant, lendemainDepart);
		if (couple != null) {
			final PersonnePhysique conjoint = couple.getConjoint(partant);
			if (conjoint != null) {
				final List<LieuResidence> lieuxResidencePrincipalePartant = getLieuxResidencePrincipale(partant);
				final List<LieuResidence> lieuxResidencePrincipaleConjoint = getLieuxResidencePrincipale(conjoint);
				final LieuResidence lieuResidencePartant = DateRangeHelper.rangeAt(lieuxResidencePrincipalePartant, lendemainDepart);
				final LieuResidence lieuResidenceConjoint = DateRangeHelper.rangeAt(lieuxResidencePrincipaleConjoint, lendemainDepart);
				if (lieuResidenceConjoint != null && lieuResidencePartant != null) {
					return lieuResidenceConjoint.getTypeAutoriteFiscale() == lieuResidencePartant.getTypeAutoriteFiscale();
				}
			}
		}
		return true;
	}

	private RegDate getDateFromLieuxResidence(List<LieuResidence> lieuxResidenceConjoint) {
		final LieuResidence residenceAuMomentDuDepart = DateRangeHelper.rangeAt(lieuxResidenceConjoint, getDate());
		final RegDate dateDepartComplet;
		if (residenceAuMomentDuDepart != null && residenceAuMomentDuDepart.typeAutoriteFiscale == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			dateDepartComplet = residenceAuMomentDuDepart.getDateFin();
		}
		else {
			dateDepartComplet = getDate();
		}
		return dateDepartComplet;
	}

	@NotNull
	private List<LieuResidence> getLieuxResidencePrincipale(PersonnePhysique pp) throws EvenementCivilException {
		if (pp.isConnuAuCivil()) {
			try {
				final AdressesCivilesHistoriques adresses = context.getServiceCivil().getAdressesHisto(pp.getNumeroIndividu(), false);
				return getLieuResidences(adresses.principales);
			}
			catch (DonneesCivilesException e) {
				throw new EvenementCivilException(e);
			}
		}
		else {
			return Collections.emptyList();
		}
	}

	@NotNull
	private List<LieuResidence> getLieuxResidenceSecondaire(PersonnePhysique pp) throws EvenementCivilException {
		if (pp.isConnuAuCivil()) {
			try {
				final AdressesCivilesHistoriques adresses = context.getServiceCivil().getAdressesHisto(pp.getNumeroIndividu(), false);
				return getLieuResidences(adresses.secondaires);
			}
			catch (DonneesCivilesException e) {
				throw new EvenementCivilException(e);
			}
		}
		else {
			return Collections.emptyList();
		}
	}

	@NotNull
	private List<LieuResidence> getLieuResidences(List<Adresse> residences) {
		if (residences== null  ||  residences.isEmpty()) {
			return Collections.emptyList();
		}
		else {
			final List<LieuResidence> lieux = new ArrayList<>(residences.size() * 2);
			final MovingWindow<Adresse> adresseWindow = new MovingWindow<>(residences);
			while (adresseWindow.hasNext()) {
				final MovingWindow.Snapshot<Adresse> snapshot = adresseWindow.next();
				final Adresse adresse = snapshot.getCurrent();
				final LieuResidence lieu = new LieuResidence(adresse.getDateDebut(), adresse.getDateFin(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, adresse.getNoOfsCommuneAdresse());
				lieux.add(lieu);

				// une adresse de destination sur le canton devrait être reprise dans les résidences suivantes -> pas la peine de la prendre en compte maintenant
				if (adresse.getLocalisationSuivante() != null && adresse.getLocalisationSuivante().getType() != LocalisationType.CANTON_VD) {
					final Adresse adresseSuivante = snapshot.getNext();
					if (adresseSuivante == null || RegDateHelper.isAfter(adresseSuivante.getDateDebut(), adresse.getDateFin().getOneDayAfter(), NullDateBehavior.EARLIEST)) {
						final RegDate dateFin = adresseSuivante == null ? null : adresseSuivante.getDateDebut().getOneDayBefore();
						final TypeAutoriteFiscale typeAutoriteFiscale =
								adresse.getLocalisationSuivante().getType() == LocalisationType.HORS_CANTON ? TypeAutoriteFiscale.COMMUNE_HC : TypeAutoriteFiscale.PAYS_HS;
						final LieuResidence suivant = new LieuResidence(adresse.getDateFin().getOneDayAfter(), dateFin, typeAutoriteFiscale, adresse.getLocalisationSuivante().getNoOfs());
						lieux.add(suivant);
					}
				}
			}
			return lieux;
		}
	}

	private static class LieuResidence implements CollatableDateRange<LieuResidence> {
		private final TypeAutoriteFiscale typeAutoriteFiscale;
		private final int noOfs;
		private final RegDate dateDebut;
		private final RegDate dateFin;

		private LieuResidence(RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noOfs) {
			this.dateDebut = dateDebut;
			this.typeAutoriteFiscale = typeAutoriteFiscale;
			this.noOfs = noOfs == null ? -1 : noOfs;
			this.dateFin = dateFin;
		}

		@Override
		public boolean isCollatable(LieuResidence next) {
			return DateRangeHelper.isCollatable(this, next) && next.noOfs == noOfs && next.typeAutoriteFiscale == typeAutoriteFiscale;
		}

		@Override
		public LieuResidence collate(LieuResidence next) {
			return new LieuResidence(dateDebut, next.getDateFin(), typeAutoriteFiscale, noOfs);
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}

		public TypeAutoriteFiscale getTypeAutoriteFiscale() {
			return typeAutoriteFiscale;
		}
	}

	public Pays getPaysInconnu() {
		return paysInconnu;
	}

	public Commune getNouvelleCommune() {
		return nouvelleCommune;
	}

	public boolean isAncienTypeDepart() {
		return isAncienTypeDepart;
	}

	protected Localisation computeNouvelleLocalisation(@Nullable Adresse ancienneAdresse, @Nullable Adresse nouvelleAdresse, @Nullable Commune nouvelleCommune) throws EvenementCivilException {

		final Localisation nextLocalisation;

		if (nouvelleAdresse != null) {
			final Integer noOfsPays = nouvelleAdresse.getNoOfsPays();
			if (noOfsPays != null && !noOfsPays.equals(ServiceInfrastructureService.noOfsSuisse)) {
				// adresse hors-suisse
				nextLocalisation = new Localisation(LocalisationType.HORS_SUISSE, noOfsPays, null);
			}
			else {
				// adresse suisse
				if (nouvelleCommune == null) {
					nouvelleCommune = context.getServiceInfra().getCommuneByAdresse(nouvelleAdresse, nouvelleAdresse.getDateDebut());
					if (nouvelleCommune == null) {
						throw new EvenementCivilException("Impossible de déterminer la commune de la nouvelle adresse");
					}
				}
				if (nouvelleCommune.isVaudoise()) {
					nextLocalisation = new Localisation(LocalisationType.CANTON_VD, nouvelleCommune.getNoOFS(), null);
				}
				else {
					nextLocalisation = new Localisation(LocalisationType.HORS_CANTON, nouvelleCommune.getNoOFS(), null);
				}
			}
		}
		else if (ancienneAdresse != null) {
			nextLocalisation = ancienneAdresse.getLocalisationSuivante();
		}
		else {
			nextLocalisation = null;
		}

		return nextLocalisation;
	}

	protected Localisation getNouvelleLocalisation() {
		return nouvelleLocalisation;
	}

	protected boolean isDepartVaudois() {
		return getNouvelleCommune() != null && getNouvelleCommune().isVaudoise();
	}

	protected abstract Integer getNumeroOfsEntiteForAnnonce();
}
