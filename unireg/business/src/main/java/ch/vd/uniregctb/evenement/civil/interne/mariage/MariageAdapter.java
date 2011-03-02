package ch.vd.uniregctb.evenement.civil.interne.mariage;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Modélise un événement conjugal (mariage, pacs)
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class MariageAdapter extends EvenementCivilInterneBase {

	protected static Logger LOGGER = Logger.getLogger(MariageAdapter.class);

	/**
	 * Le nouveau conjoint de l'individu concerné par le mariage.
	 */
	private Individu nouveauConjoint;

	public MariageAdapter(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);

		/*
		 * Calcul de l'année où a eu lieu l'événement
		 */
		int anneeEvenement = getDate().year();

		/*
		 * Récupération des informations sur le conjoint de l'individu depuis le host.
		 * getIndividu().getConjoint() peut être null si mariage le 01.01
		 */
		final long noIndividu = getNoIndividu();
		Individu individuPrincipal = context.getServiceCivil().getIndividu(noIndividu, anneeEvenement, AttributeIndividu.CONJOINT);
		this.nouveauConjoint = getConjointValide(individuPrincipal, context.getServiceCivil());
		//this.nouveauConjoint = individuPrincipal.getConjoint();
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected MariageAdapter(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.MARIAGE, date, numeroOfsCommuneAnnonce, context);
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

	/**UNIREG-2055
	 * Cette méthode permet de vérifier que le conjoint trouvé pour l'individu principal a bien
	 * un état civil cohérent avec l'événement de mariage traité
	 *
	 * @param individuPrincipal
	 * @return le conjoint correct ou null si le conjoint trouvé n'a pas le bon état civil
	 */
	private Individu getConjointValide(Individu individuPrincipal,ServiceCivilService serviceCivil) {
		Individu conjointTrouve = serviceCivil.getConjoint(individuPrincipal.getNoTechnique(),getDate().getOneDayAfter());
		if (conjointTrouve!=null && isBonConjoint(individuPrincipal, conjointTrouve, serviceCivil)) {
			final EtatCivil etatCivilConjoint = serviceCivil.getEtatCivilActif(conjointTrouve.getNoTechnique(), getDate());
			//Si le conjoint n'a pas d'état civil ou son état civil est différent de marié, on renvoie null
			if (etatCivilConjoint!=null ) {
				if (TypeEtatCivil.MARIE == etatCivilConjoint.getTypeEtatCivil() || TypeEtatCivil.PACS == etatCivilConjoint.getTypeEtatCivil()) {
					return conjointTrouve;
				}

			}

		}
		return null;
	}


	private boolean isBonConjoint(Individu principal, Individu conjoint, ServiceCivilService serviceCivil){
		Individu principalAttendu = serviceCivil.getConjoint(conjoint.getNoTechnique(),getDate().getOneDayAfter());
		if (principalAttendu!=null && principal.getNoTechnique()== principalAttendu.getNoTechnique()) {
			return true;
		}
		return false;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// Rien a vérifier, un seul événement est envoyé pour l'un des 2 individus
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
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

		// [UNIREG-1595] On ne teste l'état civil que si le tiers est habitant (pas ancien habitant...)
		if (habitant.isHabitantVD()) {

			final EtatCivil etatCivil = serviceCivil.getEtatCivilActif(individu.getNoTechnique(), getDate());
			if (etatCivil == null) {
				erreurs.add(new EvenementCivilExterneErreur("L'individu principal ne possède pas d'état civil à la date de l'événement"));
			}

			if (!EtatCivilHelper.estMarieOuPacse(etatCivil)) {
				erreurs.add(new EvenementCivilExterneErreur("L'individu principal n'est ni marié ni pacsé dans le civil"));
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

				final EtatCivil etatCivilConjoint = serviceCivil.getEtatCivilActif(conjoint.getNoTechnique(), getDate());
				if (etatCivilConjoint == null) {
					erreurs.add(new EvenementCivilExterneErreur("Le conjoint ne possède pas d'état civil à la date de l'événement"));
				}

				if (!EtatCivilHelper.estMarieOuPacse(etatCivilConjoint)) {
					erreurs.add(new EvenementCivilExterneErreur("Le conjoint n'est ni marié ni pacsé dans le civil"));
				}
			}
		}

		final ValidationResults resultat = context.getMetierService().validateMariage(getDate(), habitant, habitantConjoint);
		EvenementCivilHandlerBase.addValidationResults(erreurs, warnings, resultat);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		try {
			final PersonnePhysique contribuable = getPersonnePhysiqueOrThrowException(getNoIndividu());
			final PersonnePhysique conjointContribuable = (getNouveauConjoint() == null) ? null : getPersonnePhysiqueOrThrowException(getNouveauConjoint().getNoTechnique());

			// état civil pour traitement
			final EtatCivil etatCivil = getService().getServiceCivilService().getEtatCivilActif(contribuable.getNumeroIndividu(), getDate());
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
			
			if (remariage) {
				// [UNIREG-780]
				context.getMetierService().rattachToMenage(ancienMenage, contribuable, conjointContribuable, getDate(), null, etatCivilUnireg, false, getNumeroEvenement());
			}
			else {
				context.getMetierService().marie(getDate(), contribuable, conjointContribuable, null, etatCivilUnireg, false, getNumeroEvenement());
			}

			return null;
		}
		catch (Exception e) {
			LOGGER.error("Erreur lors du traitement de mariage", e);
			throw new EvenementCivilHandlerException(e.getMessage(), e);
		}
	}
}
