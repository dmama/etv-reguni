package ch.vd.uniregctb.evenement.civil.interne.annulation.separation;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour l'annulation de séparation.
 *
 * @author Pavel BLANCO
 *
 */
public abstract class AnnulationSeparationOuDivorce extends EvenementCivilInterne {

	protected AnnulationSeparationOuDivorce(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	public AnnulationSeparationOuDivorce(Individu individu, Individu conjoint, TypeEvenementCivil typeEvenementCivil, RegDate dateEvenement, Integer numeroOfsCommuneAnnonce,
	                                     EvenementCivilContext context) {
		super(individu, conjoint, typeEvenementCivil, dateEvenement, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// Obtention du tiers correspondant au conjoint principal.
		PersonnePhysique principal = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());
		// Récupération de l'ensemble tiers couple
		EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(principal, getDate().getOneDayBefore());
		// Récupération du tiers MenageCommun
		MenageCommun menage = null;
		if (menageComplet != null) {
			menage = menageComplet.getMenage();
		}
		// Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
		if (menage == null) {
			throw new EvenementCivilHandlerException("Le tiers ménage commun n'a pu être trouvé");
		}
		PersonnePhysique conjoint = null;
		final Individu individuConjoint = context.getServiceCivil().getConjoint(getNoIndividu(), getDate());
		if (individuConjoint != null) {
			// Obtention du tiers correspondant au conjoint.
			conjoint = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(individuConjoint.getNoTechnique());
		}
		// Vérification de la cohérence
		if (!menageComplet.estComposeDe(principal, conjoint)) {
			throw new EvenementCivilHandlerException("Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		// Récupération du tiers principal.
		PersonnePhysique principal = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());
		// Récupération du menage du tiers
		MenageCommun menage = context.getTiersService().getEnsembleTiersCouple(principal, getDate().getOneDayBefore()).getMenage();
		// Traitement de l'annulation de séparation
		context.getMetierService().annuleSeparation(menage, getDate(), getNumeroEvenement());
		return null;
	}
}
