package ch.vd.uniregctb.evenement.civil.interne.annulation.arrivee;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class AnnulationArrivee extends EvenementCivilInterne {

	protected AnnulationArrivee(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationArrivee(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		getPersonnePhysiqueOrFillErrors(getNoIndividu(), erreurs);
	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// [UNIREG-3017] si le CTB PP est mineur (ou le couple à la date de l'événement CTB MC a deux individus mineurs) et n'a aucun for (du tout) ou que tous sont annulés -> Traiter l'événement tout droit
		final Individu individu = getIndividu();
		final PersonnePhysique pp = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());
		final EnsembleTiersCouple couple = context.getTiersService().getEnsembleTiersCouple(pp, getDate());
		final boolean mineur;
		if (couple == null) {
			mineur = individu.isMineur(getDate());
		}
		else {
			final boolean mineurPpal = individu.isMineur(getDate());
			final PersonnePhysique conjoint = couple.getConjoint(pp);
			final boolean mineurConjoint = conjoint == null || context.getTiersService().isMineur(conjoint, getDate());
			mineur = mineurPpal && mineurConjoint;
		}

		boolean erreur = true;
		if (mineur) {
			final Contribuable ctb = couple != null ? couple.getMenage() : pp;
			final List<ForFiscal> fors = ctb.getForsFiscauxNonAnnules(false);
			if (fors == null || fors.isEmpty()) {
				Audit.info(getNumeroEvenement(), String.format("Aucun for non-annulé existant sur le contribuable %s (%s) : rien à faire",
						FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()),
						couple != null ? "ménage de personnes physiques mineures" : "mineur"));
				erreur = false;
			}
		}

		if (erreur) {
			throw new EvenementCivilException("Veuillez effectuer cette opération manuellement");
		}
		return null;
	}
}
