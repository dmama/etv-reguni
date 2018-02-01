package ch.vd.unireg.evenement.civil.interne.changement.adresseNotification;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.interne.changement.ChangementBase;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.tiers.PersonnePhysique;

public abstract class ModificationAdresseBase extends ChangementBase {

	protected ModificationAdresseBase(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public ModificationAdresseBase(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
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

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		final PersonnePhysique pp = getPrincipalPP();
		if (pp == null) {
			final long noIndividu = getNoIndividu();
			throw new EvenementCivilException("Impossible de retrouver le tiers correspondant à l'individu " + noIndividu);
		}

		doHandle(pp, warnings);

		return super.handle(warnings);
	}
}
