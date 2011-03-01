package ch.vd.uniregctb.evenement.changement.permis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

public class CorrectionDebutValiditePermisHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		final Individu individu = evenement.getIndividu();
		final Permis permis = individu.getPermisActif(evenement.getDate());
		if (permis == null || permis.getTypePermis() != TypePermis.ETABLISSEMENT) {
			Audit.info(evenement.getNumeroEvenement(), String.format("Permis autre que permis C à la date de l'événement : ignoré"));
		}
		else {
			// nous avons donc une modification de date de début d'un permis C
			// la règle dit : si l'individu est mineur sans for aujourd'hui, on peut passer tout droit, sinon, on ne sait pas encore faire et on met en erreur
			final RegDate aujourdhui = RegDate.get();
			boolean ignorable = false;
			if (individu.isMineur(aujourdhui)) {
				final PersonnePhysique pp = getService().getPersonnePhysiqueByNumeroIndividu(individu.getNoTechnique());
				if (pp != null) {
					boolean mineurs = true;
					final Contribuable ctb;
					final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(pp, aujourdhui);
					if (couple != null) {
						ctb = couple.getMenage();
						final PersonnePhysique conjoint = couple.getConjoint(pp);
						if (conjoint != null) {
							mineurs = getService().isMineur(conjoint, aujourdhui);
						}
					}
					else {
						ctb = pp;
					}

					if (mineurs) {
						final List<ForFiscal> fors = ctb.getForsFiscauxValidAt(aujourdhui);
						if (fors == null || fors.size() == 0) {
							ignorable = true;
							Audit.info(evenement.getNumeroEvenement(), String.format("Permis C sur mineur sans for (à la date de traitement) : ignoré"));
						}
					}
				}
			}

			if (!ignorable) {
				throw new EvenementCivilHandlerException("Permis C sur individu majeur ou ayant un for fiscal actif : veuillez traiter le cas manuellement.");
			}
		}

		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CORREC_DEBUT_VALIDITE_PERMIS);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter(EvenementCivilData event, EvenementCivilContext context) throws EvenementAdapterException {
		return new CorrectionDebutValiditePermisAdapter(event, context, this);
	}

}