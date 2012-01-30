package ch.vd.uniregctb.evenement.civil.interne.mariage;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
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
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Modélise un événement conjugal (mariage, pacs)
 */
public class Mariage extends EvenementCivilInterne {

	protected static final Logger LOGGER = Logger.getLogger(Mariage.class);

	/**
	 * Le nouveau conjoint de l'individu concerné par le mariage.
	 */
	private Individu nouveauConjoint;
	private boolean isRedondant;

	public Mariage(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		this.nouveauConjoint = getConjointValide(getNoIndividu(), getDate(), context.getServiceCivil());
	}

	public Mariage(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
		this.nouveauConjoint = getConjointValide(getNoIndividu(), getDate(), context.getServiceCivil());
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Mariage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
		this.nouveauConjoint = conjoint;
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	/*l
	 * (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.mariage.Mariage#getNouveauConjoint()
	 */
	public Individu getNouveauConjoint() {
		return nouveauConjoint;
	}

	/**
	 * [UNIREG-2055] Cette méthode permet de vérifier que le conjoint trouvé pour l'individu principal a bien un état civil cohérent avec l'événement de mariage traité
	 *
	 * @param noPrincipal  le numéro d'individu principal considéré
	 * @param date         la date de valeur
	 * @param serviceCivil le service civil
	 * @return le conjoint correct ou null si le conjoint trouvé n'a pas le bon état civil
	 */
	private static Individu getConjointValide(long noPrincipal, @NotNull RegDate date, ServiceCivilService serviceCivil) {

		final Individu conjoint = serviceCivil.getConjoint(noPrincipal, date.getOneDayAfter());
		if (conjoint == null) {
			return null;
		}

		final Individu principal = serviceCivil.getIndividu(noPrincipal, date);
		if (!isBonConjoint(principal, conjoint, date, serviceCivil)) {
			return null;
		}

		// si le conjoint n'a pas d'état civil, on renvoie null
		final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(conjoint.getNoTechnique(), date);
		if (etatCivil == null) {
			return null;
		}

		// si le conjoint n'est pas marié/pacsé, on renvoie null
		if (TypeEtatCivil.MARIE != etatCivil.getTypeEtatCivil() && TypeEtatCivil.PACS != etatCivil.getTypeEtatCivil()) {
			return null;
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

		/*
		 * Le tiers correspondant doit exister
		 */
		PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(individu.getNoTechnique(), erreurs);
		if (habitant == null) {
			return;
		}

		final ServiceCivilService serviceCivil = context.getServiceCivil();
		final RegDate dateMariage = getDate();

		// [UNIREG-1595] On ne teste l'état civil que si le tiers est habitant (pas ancien habitant...)
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
		 * Dans le cas où le conjoint réside dans le canton, il faut que le tiers contribuable existe.
		 */
		PersonnePhysique habitantConjoint = null;
		final Individu conjoint = getNouveauConjoint();
		if (conjoint != null) {

			/*
			 * Le tiers correspondant doit exister
			 */
			habitantConjoint = getPersonnePhysiqueOrFillErrors(conjoint.getNoTechnique(), erreurs);
			if (habitantConjoint == null) {
				return;
			}

			// [UNIREG-1595] On ne teste l'état civil que si le tiers est habitant (pas ancien habitant...)
			if (habitantConjoint.isHabitantVD()) {

				final EtatCivil etatCivilConjoint = serviceCivil.getEtatCivilActif(conjoint.getNoTechnique(), dateMariage);
				if (etatCivilConjoint == null) {
					erreurs.addErreur("Le conjoint ne possède pas d'état civil à la date de l'événement");
				}

				if (!EtatCivilHelper.estMarieOuPacse(etatCivilConjoint)) {
					erreurs.addErreur("Le conjoint n'est ni marié ni pacsé dans le civil");
				}
			}
		}

		// détection d'un événement redondant
		isRedondant = context.getMetierService().isEnMenageDepuis(habitant, habitantConjoint, dateMariage);

		if (!isRedondant) {
			final ValidationResults resultat = context.getMetierService().validateMariage(dateMariage, habitant, habitantConjoint);
			addValidationResults(erreurs, warnings, resultat);
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		if (isRedondant) {
			return HandleStatus.REDONDANT;
		}

		final PersonnePhysique contribuable = getPersonnePhysiqueOrThrowException(getNoIndividu());
		final PersonnePhysique conjointContribuable = (getNouveauConjoint() == null) ? null : getPersonnePhysiqueOrThrowException(getNouveauConjoint().getNoTechnique());

		// état civil pour traitement
		final EtatCivil etatCivil = context.getServiceCivil().getEtatCivilActif(contribuable.getNumeroIndividu(), getDate());
		final ch.vd.uniregctb.type.EtatCivil etatCivilUnireg = etatCivil.getTypeEtatCivil().asCore();
		
		// [UNIREG-780] : détection et reprise d'un ancien ménage auquel ont appartenu les deux contribuables
		boolean remariage = false;
		MenageCommun ancienMenage = null;
		for (RapportEntreTiers rapport : contribuable.getRapportsSujet()) {
			if (!rapport.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE == rapport.getType()) {
				final MenageCommun menage = (MenageCommun) context.getTiersDAO().get(rapport.getObjetId());
				final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(menage, rapport.getDateDebut());
				if (couple != null && couple.estComposeDe(contribuable, conjointContribuable)) {
					// les contribuables se sont remariés
					remariage = true;
					ancienMenage = menage;
				}
			}
		}
		
		// [UNIREG-780]
		try {
			if (remariage) {
				context.getMetierService().rattachToMenage(ancienMenage, contribuable, conjointContribuable, getDate(), null, etatCivilUnireg, false, getNumeroEvenement());
			}
			else {
				context.getMetierService().marie(getDate(), contribuable, conjointContribuable, null, etatCivilUnireg, false, getNumeroEvenement());
			}
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}

		return HandleStatus.TRAITE;
	}
}
