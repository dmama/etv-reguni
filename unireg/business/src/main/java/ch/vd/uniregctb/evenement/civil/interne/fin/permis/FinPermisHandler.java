package ch.vd.uniregctb.evenement.civil.interne.fin.permis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Gère la fin d'obtention d'un permis.
 * 
 * @author Pavel BLANCO
 *
 */
public class FinPermisHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		FinPermis finPermis = (FinPermis) evenement;
		/* Seulement le permis C est traité */
		if (finPermis.getTypePermis() == TypePermis.ETABLISSEMENT) {
			
			PersonnePhysique habitant = getPersonnePhysiqueOrFillErrors(finPermis.getNoIndividu(), erreurs);
			if (habitant == null) {
				return;
			}
			
			boolean isSuisse = false;
			// vérification si la nationalité suisse a été obtenue
			try {
				isSuisse = getService().isSuisse(habitant, finPermis.getDate().getOneDayAfter());
			}
			catch (Exception e) {
				erreurs.add(new EvenementCivilExterneErreur(e.getMessage()));
				return;
			}
			
			if (isSuisse) {
				Audit.info(finPermis.getNumeroEvenement(), "Permis C : l'habitant a obtenu la nationalité suisse, rien à faire");
			}
			else {
				Audit.info(finPermis.getNumeroEvenement(), "Permis C : l'habitant n'a pas obtenu la nationalité suisse, passage en traitement manuel");
				erreurs.add(new EvenementCivilExterneErreur("La fin du permis C doit être traitée manuellement"));
			}
		}
		else {
			Audit.info(finPermis.getNumeroEvenement(), "Permis non C : ignoré");
		}
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		// rien à faire tout ce passe dans le validateSpecific
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.FIN_CHANGEMENT_CATEGORIE_ETRANGER);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new FinPermisAdapter(event, context, this);
	}

}
