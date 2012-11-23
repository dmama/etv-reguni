package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.List;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.interne.mouvement.Mouvement;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
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
	protected static Logger LOGGER = Logger.getLogger(Depart.class);

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

	public Depart(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
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
	 *
	 * @param pp             personne physique désignée par l'événement civil
	 * @param ctb            contribuable concerné par le départ
	 * @param dateFermeture  date de fermeture des fors
	 * @param motifFermeture motif de fermeture des fors à fermer
	 * @throws EvenementCivilException en cas de problème
	 */
	protected abstract void doHandleFermetureFors(PersonnePhysique pp, Contribuable ctb, RegDate dateFermeture, MotifFor motifFermeture) throws EvenementCivilException;

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		if (isDepartVaudois()) { // on ignore les départs vaudois car dans ce cas c'est l'arrivée qui fait foi
			return HandleStatus.TRAITE;
		}

		final PersonnePhysique pp = getPrincipalPP();
		if (pp == null) {
			// si on ne connaissait pas le gaillard, c'est un problème
			throw new EvenementCivilException("Aucun habitant (ou ancien habitant) trouvé avec numéro d'individu " + getNoIndividu());
		}

		final MotifFor motifFermeture = findMotifFermetureFor();
		final RegDate dateFermeture = findDateFermeture(this, motifFermeture == MotifFor.DEMENAGEMENT_VD);
		final Contribuable contribuable = findContribuable(dateFermeture, pp);

		doHandleFermetureFors(pp, contribuable, dateFermeture, motifFermeture);

		// [SIFISC-6841] on met-à-jour le flag habitant en fonction de ses adresses de résidence civiles
		context.getTiersService().updateHabitantFlag(pp, getNoIndividu(), dateFermeture.getOneDayAfter(), getNumeroEvenement());

		if (isAncienTypeDepart) {
			context.getTiersService().updateHabitantFlag(getConjointPP(), getNoIndividuConjoint(), dateFermeture.getOneDayAfter(), getNumeroEvenement());
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
	private Contribuable findContribuable(RegDate date, PersonnePhysique pp) {
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
				erreurs.addErreur("Un départ ne peut être traité qu'à partir du lendemain de sa date d'effet");
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
	protected ForFiscalPrincipal openForFiscalPrincipalHC(Contribuable contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale, ModeImposition modeImposition,
	                                                      MotifFor motifOuverture) {
		return getService()
				.openForFiscalPrincipal(contribuable, dateOuverture, MotifRattachement.DOMICILE, numeroOfsAutoriteFiscale, TypeAutoriteFiscale.COMMUNE_HC, modeImposition, motifOuverture, false);
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
	protected ForFiscalPrincipal openForFiscalPrincipalHS(Contribuable contribuable, final RegDate dateOuverture, int numeroOfsAutoriteFiscale, ModeImposition modeImposition,
	                                                      MotifFor motifOuverture) {
		return getService()
				.openForFiscalPrincipal(contribuable, dateOuverture, MotifRattachement.DOMICILE, numeroOfsAutoriteFiscale, TypeAutoriteFiscale.PAYS_HS, modeImposition, motifOuverture, false);
	}

	protected static ModeImposition determineModeImpositionDepartHCHS(Contribuable contribuable, RegDate dateFermeture, ForFiscalPrincipal ffp) {

		Assert.notNull(ffp);

		final ModeImposition modeImpositionAncien = ffp.getModeImposition();
		final ModeImposition modeImposition;
		if (isSourcierPur(modeImpositionAncien)) {
			// Un sourcier pur reste à la source.
			modeImposition = ModeImposition.SOURCE;
		}
		else if (isSourcierMixte(modeImpositionAncien)) {
			// [UNIREG-1849] passe à l'ordinaire si for secondaire, sinon à la source
			final List<ForFiscal> ffs = contribuable.getForsFiscauxValidAt(dateFermeture);
			boolean hasForSecondaire = false;
			for (ForFiscal ff : ffs) {
				if (ff instanceof ForFiscalSecondaire) {
					hasForSecondaire = true;
					break;
				}
			}
			if (hasForSecondaire) {
				modeImposition = ModeImposition.ORDINAIRE;
			}
			else {
				modeImposition = ModeImposition.SOURCE;
			}
		}
		else {
			// tous les autres cas passent à l'ordinaire (ordinaire, dépense, indigent...)
			modeImposition = ModeImposition.ORDINAIRE;
		}
		return modeImposition;
	}

	private static boolean isSourcierPur(ModeImposition modeImposition) {
		return ModeImposition.SOURCE == modeImposition;
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
