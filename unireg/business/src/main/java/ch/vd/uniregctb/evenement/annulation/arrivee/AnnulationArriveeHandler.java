package ch.vd.uniregctb.evenement.annulation.arrivee;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
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

	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		getPersonnePhysiqueOrFillErrors(target.getNoIndividu(), erreurs);
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

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
	public GenericEvenementAdapter createAdapter(EvenementCivilData event, EvenementCivilContext context) throws EvenementAdapterException {
		return new AnnulationArriveeAdapter(event, context, this);
	}
}
