package ch.vd.unireg.evenement.civil.interne.annulation.arrivee;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.PersonnePhysique;

public class AnnulationArrivee extends EvenementCivilInterne {

	protected AnnulationArrivee(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected AnnulationArrivee(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationArrivee(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		final PersonnePhysique pp = getPersonnePhysiqueOrFillErrors(getNoIndividu(), erreurs);
		verifierPresenceDecisionEnCours(pp,getDate());
		verifierPresenceDecisionsEnCoursSurCouple(pp);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// [UNIREG-3017] si le CTB PP est mineur (ou le couple à la date de l'événement CTB MC a deux individus mineurs) et n'a aucun for (du tout) ou que tous sont annulés -> Traiter l'événement tout droit
		final Individu individu = getIndividuOrThrowException();
		final PersonnePhysique pp = getPrincipalPP();
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
				context.audit.info(getNumeroEvenement(), String.format("Aucun for non-annulé existant sur le contribuable %s (%s) : rien à faire",
						FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()),
						couple != null ? "ménage de personnes physiques mineures" : "mineur"));
				erreur = false;
			}
		}

		if (erreur) {
			throw new EvenementCivilException("Veuillez effectuer cette opération manuellement");
		}

		// [SIFISC-6841] on met-à-jour le flag habitant en fonction de ses adresses de résidence civiles
		updateHabitantStatus(pp, getDate());

		return HandleStatus.TRAITE;
	}
}
