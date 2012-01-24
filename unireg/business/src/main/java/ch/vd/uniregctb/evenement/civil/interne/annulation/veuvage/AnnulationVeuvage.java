package ch.vd.uniregctb.evenement.civil.interne.annulation.veuvage;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class AnnulationVeuvage extends EvenementCivilInterne {

	protected AnnulationVeuvage(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationVeuvage(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		long numeroIndividu = getNoIndividu();
		PersonnePhysique veuf = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(numeroIndividu);

		/*
				 * Récupération du ménage du veuf
				 */
		EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(veuf, getDate().getOneDayAfter());

		if (menageComplet != null || (menageComplet != null && (menageComplet.getMenage() != null || menageComplet.getConjoint(veuf) != null))) {
			/*
			 * Normalement l'événement de veuvage ne doit s'appliquer aux personnes encores mariées (seules).
			 */
			erreurs.addErreur("L'événement d'annulation veuvage ne peut pas s'appliquer à une personne mariée.");
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		/*
		 * Obtention du tiers
		 */
		PersonnePhysique veuf = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());

		/*
		 * Traitement de l'événement
		 */
		try {
			context.getMetierService().annuleVeuvage(veuf, getDate(), getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return null;
	}
}
