package ch.vd.uniregctb.evenement.civil.interne.changement.sexe;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementBase;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ChangementSexe extends ChangementBase {

	protected ChangementSexe(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ChangementSexe(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.CHGT_SEXE, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		final long noIndividu = getNoIndividu();

		Audit.info(getNumeroEvenement(), String.format("Traitement du changement de sexe de l'individu : %d", noIndividu));

		final PersonnePhysique pp = context.getTiersDAO().getPPByNumeroIndividu(noIndividu, true);
		if (pp != null && !pp.isHabitantVD()) {
			// pour les non-habitants, il faut recharger les données, non?
			// quelles sont les données à recharger ? sexe !
			final Individu individu = context.getTiersService().getIndividu(pp);
			pp.setSexe(individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ);
		}

		return super.handle(warnings);
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		/* Voir ce qu'il y a à vérifier pour le changement de sexe: peut etre le num AVS */

		/* l'existance de l'individu est vérifié dans validateCommon */
	}
}
