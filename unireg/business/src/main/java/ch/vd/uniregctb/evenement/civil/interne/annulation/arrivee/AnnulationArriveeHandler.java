package ch.vd.uniregctb.evenement.civil.interne.annulation.arrivee;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Gère les événements suivants:
 * <ul>
 * <li>suppression arrivée dans la commune</li>
 * <li>annulation arrivée secondaire</li>
 * </ul>
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationArriveeHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		getPersonnePhysiqueOrFillErrors(target.getNoIndividu(), erreurs);
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		// [UNIREG-3017] si le CTB PP est mineur (ou le couple à la date de l'événement CTB MC a deux individus mineurs) et n'a aucun for (du tout) ou que tous sont annulés -> Traiter l'événement tout droit
		final Individu individu = evenement.getIndividu();
		final PersonnePhysique pp = getService().getPersonnePhysiqueByNumeroIndividu(evenement.getNoIndividu());
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(pp, evenement.getDate());
		final boolean mineur;
		if (couple == null) {
			mineur = individu.isMineur(evenement.getDate());
		}
		else {
			final boolean mineurPpal = individu.isMineur(evenement.getDate());
			final PersonnePhysique conjoint = couple.getConjoint(pp);
			final boolean mineurConjoint = conjoint == null || getService().isMineur(conjoint, evenement.getDate());
			mineur = mineurPpal && mineurConjoint;
		}

		boolean erreur = true;
		if (mineur) {
			final Contribuable ctb = couple != null ? couple.getMenage() : pp;
			final List<ForFiscal> fors = ctb.getForsFiscauxNonAnnules(false);
			if (fors == null || fors.size() == 0) {
				Audit.info(evenement.getNumeroEvenement(), String.format("Aucun for non-annulé existant sur le contribuable %s (%s) : rien à faire",
																		FormatNumeroHelper.numeroCTBToDisplay(ctb.getNumero()),
																		couple != null ? "ménage de personnes physiques mineures" : "mineur"));
				erreur = false;
			}
		}

		if (erreur) {
			throw new EvenementCivilHandlerException("Veuillez effectuer cette opération manuellement");
		}
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.SUP_ARRIVEE_DANS_COMMUNE);
		types.add(TypeEvenementCivil.ANNUL_ARRIVEE_SECONDAIRE);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new AnnulationArriveeAdapter(event, context, this);
	}
}
