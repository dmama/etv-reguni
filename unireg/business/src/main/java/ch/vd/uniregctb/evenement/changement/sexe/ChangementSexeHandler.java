package ch.vd.uniregctb.evenement.changement.sexe;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.changement.AbstractChangementHandler;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ChangementSexeHandler extends AbstractChangementHandler {
	private static final Logger LOGGER = Logger.getLogger(ChangementSexeHandler.class);

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {

	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Throwable.class)
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		final long noIndividu = evenement.getIndividu().getNoTechnique();

		LOGGER.debug("Traitement du changement de sexe de l'individu : " + noIndividu);
		Audit.info(evenement.getNumeroEvenement(), "Traitement du changement de sexe de l'individu : " + noIndividu);

		final PersonnePhysique pp = getTiersDAO().getPPByNumeroIndividu(noIndividu, true);
		if (pp != null && !pp.isHabitant()) {
			// pour les non-habitants, il faut recharger les données, non?
			// quelles sont les données à recharger ? sexe !
			final Individu individu = getService().getIndividu(pp);
			pp.setSexe(individu.isSexeMasculin() ? Sexe.MASCULIN : Sexe.FEMININ);
		}

		return super.handle(evenement, warnings);
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		/* Voir ce qu'il y a à vérifier pour le changement de sexe: peut etre le num AVS */

		/* l'existance de l'individu est vérifié dans validateCommon */
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CHGT_SEXE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new ChangementSexeAdapter();
	}
}
