package ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementBase;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public abstract class ModificationAdresseBase extends ChangementBase {

	protected ModificationAdresseBase(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
	}

	@Override
	protected boolean autoriseIndividuInconnuFiscalement() {
		return false;
	}

	/**
	 * Travail spécifique à la modification d'adresse
	 * @param pp la personne physique concernée
	 * @param warnings la liste à compléter si nécessaire
	 * @throws EvenementCivilException en cas de problème
	 */
	protected abstract void doHandle(PersonnePhysique pp, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException;

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		final long noIndividu = getNoIndividu();
		final PersonnePhysique pp = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(noIndividu);
		if (pp == null) {
			throw new EvenementCivilException("Impossible de retrouver le tiers correspondant à l'individu " + noIndividu);
		}

		doHandle(pp, warnings);

		return super.handle(warnings);
	}
}
