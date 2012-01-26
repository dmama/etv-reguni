package ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementBase;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public abstract class ModificationAdresseBase extends ChangementBase {

	protected ModificationAdresseBase(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
	}

	@Override
	protected boolean autoriseIndividuInconnuFiscalement() {
		return false;
	}

	/**
	 * Travail spécifique à la modification d'adresse
	 *
	 * @param pp la personne physique concernée
	 * @param warnings la liste à compléter si nécessaire
	 * @throws EvenementCivilException en cas de problème
	 */
	protected abstract void doHandle(PersonnePhysique pp, EvenementCivilWarningCollector warnings) throws EvenementCivilException;

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final PersonnePhysique pp = getPrincipalPP();
		if (pp == null) {
			final long noIndividu = getNoIndividu();
			throw new EvenementCivilException("Impossible de retrouver le tiers correspondant à l'individu " + noIndividu);
		}

		doHandle(pp, warnings);

		return super.handle(warnings);
	}
}
