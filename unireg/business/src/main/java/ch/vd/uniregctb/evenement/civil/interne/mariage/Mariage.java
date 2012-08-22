package ch.vd.uniregctb.evenement.civil.interne.mariage;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;


/**
 * Modélise un événement conjugal (mariage, pacs)
 */
public class Mariage extends EvenementCivilInterne {

	protected static final Logger LOGGER = Logger.getLogger(Mariage.class);

	/**
	 * Le nouveau conjoint de l'individu concerné par le mariage.
	 */
	private final Individu nouveauConjoint;
	private final PersonnePhysique nouveauConjointPP;
	private final MenageCommun menageAReconstituer;
	private boolean isRedondant;
	private final boolean isFromRcpers; // Pas beau, mais utile pour eviter les regressions sur les traitements entre reg-pp et rcpers
	private ConjointBizarreException conjointBizarreException; // Utiliser dans le validSpecific pour differencier les cas ou le conjoint n'est pas là des cas ou il est invalide

	public Mariage(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		this.isFromRcpers = false;
		Individu nouveauConjoint;
		try {
			nouveauConjoint = getConjointValide(evenement.getId());
		}
		catch (ConjointBizarreException e) {
			nouveauConjoint = null;
			this.conjointBizarreException = null; // pour ne pas changer le traitement des evt RegPP on ne tient pas compte de l'exception qui a un effet de bord dans le validSpecific
		}
		this.nouveauConjoint = nouveauConjoint;
		this.nouveauConjointPP = nouveauConjoint == null ? null : getPersonnePhysiqueOrNull(nouveauConjoint.getNoTechnique(), true);
		this.menageAReconstituer = null;        // n'était pas géré à l'époque Reg-PP
	}

	public Mariage(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
		this.isFromRcpers = true;
		Individu nouveauConjoint;
		try {
			nouveauConjoint = getConjointValide(event.getId());
		}
		catch (ConjointBizarreException e) {
			nouveauConjoint = null;
			this.conjointBizarreException = e;
		}
		this.nouveauConjoint = nouveauConjoint;
       	this.nouveauConjointPP = nouveauConjoint == null ? null : getPersonnePhysiqueOrNull(nouveauConjoint.getNoTechnique(), true);
		this.menageAReconstituer = computeReconstitution(nouveauConjointPP, getDate(), context.getTiersService());
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Mariage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
		this.isFromRcpers = false;
		this.nouveauConjoint = conjoint;
		this.nouveauConjointPP = nouveauConjoint == null ? null : getPersonnePhysiqueOrNull(nouveauConjoint.getNoTechnique(), true);
		this.menageAReconstituer = null;
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	/**
	 * @param conjointConnu le contribuable correspondant au conjoint que le marié nous dit avoir au civil
	 * @param dateMariage date du mariage
	 * @param tiersService service d'accès aux informations des tiers
	 * @return si le conjoint connu du marié est déjà, à la date du mariage pris en compte aujourd'hui, le seul composant d'un ménage commun (= marié seul), on renvoie ce ménage commun
	 */
	private static MenageCommun computeReconstitution(PersonnePhysique conjointConnu, RegDate dateMariage, TiersService tiersService) {
		final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(conjointConnu, dateMariage);
		return couple != null && couple.getConjoint(conjointConnu) == null ? couple.getMenage() : null;
	}

	/**
	 * [UNIREG-2055] Cette méthode permet de vérifier que le conjoint trouvé pour l'individu principal a bien un état civil cohérent avec l'événement de mariage traité
	 *
	 * @param idEvent      identifiant de l'événement civil en cours de traitement
	 *
	 * @return le conjoint correct ou null si le conjoint trouvé n'a pas le bon état civil
	 */
	private Individu getConjointValide(long idEvent) throws ConjointBizarreException {

		final Long noIndividu = getNoIndividu();
		final RegDate dateDeValidite = getDate();
		final ServiceCivilService serviceCivil = context.getServiceCivil();

		final Individu conjoint = serviceCivil.getConjoint(noIndividu, dateDeValidite.getOneDayAfter());
		if (conjoint == null) {
			Audit.info(idEvent, String.format("Aucun conjoint trouvé pour l'individu %d dans le civil au %s", noIndividu, RegDateHelper.dateToDisplayString(dateDeValidite.getOneDayAfter())));
			return null;
		}

		// si le conjoint n'a pas d'état civil, on lève une ConjointBizarreException
		final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(conjoint.getNoTechnique(), dateDeValidite);
		if (etatCivil == null) {
			String msg = String.format("L'individu conjoint %d n'a pas d'état civil actif dans le registre civil au %s",
					conjoint.getNoTechnique(), RegDateHelper.dateToDisplayString(dateDeValidite));
			Audit.info(idEvent, msg);
			throw new ConjointBizarreException(ConjointBizarreException.TypeDeBizarrerie.ETAT_CIVIL, msg);
		}

		// si le conjoint n'est pas marié/pacsé, on lève une ConjointBizarreException
		if (!EtatCivilHelper.estMarieOuPacse(etatCivil)) {
			String msg = String.format("L'état civil de l'individu conjoint %d est '%s' dans le registre civil au %s",
					conjoint.getNoTechnique(), etatCivil.getTypeEtatCivil(), RegDateHelper.dateToDisplayString(dateDeValidite));
			Audit.info(idEvent, msg);
			throw new ConjointBizarreException(ConjointBizarreException.TypeDeBizarrerie.ETAT_CIVIL, msg);
		}

		final Individu principal = serviceCivil.getIndividu(noIndividu, dateDeValidite);
		if (!isBonConjoint(principal, conjoint, dateDeValidite, serviceCivil)) {
			String msg = String.format("Le lien de conjoint n'existe pas depuis l'individu %d vers l'individu %d dans le registre civil au %s",
					conjoint.getNoTechnique(), noIndividu, RegDateHelper.dateToDisplayString(dateDeValidite));
			Audit.info(idEvent, msg);
			throw new ConjointBizarreException(ConjointBizarreException.TypeDeBizarrerie.LIEN, msg);
		}

		// on a trouvé le conjoint !
		return conjoint;
	}

	private static boolean isBonConjoint(Individu principal, Individu conjoint, @NotNull RegDate date, ServiceCivilService serviceCivil){
		final Individu principalAttendu = serviceCivil.getConjoint(conjoint.getNoTechnique(), date.getOneDayAfter());
		return principalAttendu != null && principal.getNoTechnique() == principalAttendu.getNoTechnique();
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		/* L’événement est mis en erreur dans les cas suivants */
		final Individu individu = getIndividu();
		if (individu == null) {
			erreurs.addErreur("L'individu principal est introuvable ...");
			return;
		}
		PersonnePhysique habitant = getPrincipalPP();

		final ServiceCivilService serviceCivil = context.getServiceCivil();
		final RegDate dateMariage = getDate();

		/*
		 * S'il est habitant, Le tiers correspondant doit avoir un état civil cohérent dans le civil
		 */
		if (habitant.isHabitantVD()) {
			final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(individu.getNoTechnique(), dateMariage);
			if (etatCivil == null) {
				erreurs.addErreur("L'individu principal ne possède pas d'état civil à la date de l'événement");
			}
			if (!EtatCivilHelper.estMarieOuPacse(etatCivil)) {
				erreurs.addErreur("L'individu principal n'est ni marié ni pacsé dans le civil");
			}
		}

		/*
		 * Dans le cas où l'état civil du conjoint est invalide et l'evenement originaire de Rcpers
		 */
		if (isFromRcpers && conjointBizarreException != null && conjointBizarreException.bizarrerie == ConjointBizarreException.TypeDeBizarrerie.ETAT_CIVIL) {
			erreurs.addErreur(conjointBizarreException);
		}

		/*
		 * Dans le cas où le conjoint n'existe pas dans le fiscal
		 */
		if (nouveauConjoint != null && nouveauConjointPP == null) {
			erreurs.addErreur(String.format("Le contribuable conjoint correspondant à l'individu %d n'existe pas dans le registre fiscal", nouveauConjoint.getNoTechnique()));
		}

		// détection d'un événement redondant
		isRedondant = context.getMetierService().isEnMenageDepuis(habitant, nouveauConjointPP, dateMariage);

		if (!isRedondant) {
			final ValidationResults validationResults;
			if (menageAReconstituer != null) {
				validationResults = context.getMetierService().validateReconstitution(menageAReconstituer, habitant, getDate());
			}
			else {
				validationResults = context.getMetierService().validateMariage(dateMariage, habitant, nouveauConjointPP);
			}
			addValidationResults(erreurs, warnings, validationResults);
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		if (isRedondant) {
			return HandleStatus.REDONDANT;
		}

		final PersonnePhysique contribuable = getPrincipalPP();
		final PersonnePhysique conjointContribuable = nouveauConjointPP;

		// état civil pour traitement
		final EtatCivil etatCivil = context.getServiceCivil().getEtatCivilActif(contribuable.getNumeroIndividu(), getDate());
		final ch.vd.uniregctb.type.EtatCivil etatCivilUnireg = EtatCivilHelper.civil2core(etatCivil.getTypeEtatCivil());
		
		try {
			// [SIFISC-6021] Le comportement de reconstitution des ménages n'est pas encore bien défini, on shunte les modif en attendant d'en savoir plus ...
			final boolean SKIP_SIFISC_4672 = true;
			//noinspection PointlessBooleanExpression,ConstantConditions
			if (!SKIP_SIFISC_4672 && menageAReconstituer != null) {
				// [SIFISC-4672] Reconstitution du ménage commun complet à la réception de l'événement civil de mariage du deuxième individu
				context.getMetierService().reconstitueMenage(menageAReconstituer, contribuable, getDate(), null, etatCivilUnireg);
			}
			else  {
				// [UNIREG-780] : détection et reprise d'un ancien ménage auquel ont appartenu les deux contribuables
				MenageCommun ancienMenage = null;
				for (RapportEntreTiers rapport : contribuable.getRapportsSujet()) {
					if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType()) {
						final MenageCommun menage = (MenageCommun) context.getTiersDAO().get(rapport.getObjetId());
						final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(menage, rapport.getDateDebut());
						if (couple != null && couple.estComposeDe(contribuable, conjointContribuable)) {
							// les contribuables se sont remariés
							ancienMenage = menage;
							break;
						}
					}
				}

				if (ancienMenage != null) {
					context.getMetierService().rattachToMenage(ancienMenage, contribuable, conjointContribuable, getDate(), null, etatCivilUnireg, false, getNumeroEvenement());
				}
				else {
					context.getMetierService().marie(getDate(), contribuable, conjointContribuable, null, etatCivilUnireg, false, getNumeroEvenement());
				}
			}
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}

		return HandleStatus.TRAITE;
	}

	private static class ConjointBizarreException extends Exception {

		static enum TypeDeBizarrerie {
			ETAT_CIVIL,  // Lorsque la bizarrerie est relative à l'état civil
			LIEN         // Lorsque la bizarrerie est relative au lien entre les conjoints dans le civil
		}

		private static final long serialVersionUID = 8334382141567038871L;

		final TypeDeBizarrerie bizarrerie;

		private ConjointBizarreException(TypeDeBizarrerie bizarrerie, String message) {
			super(message);
			this.bizarrerie = bizarrerie;
		}

	}
}
