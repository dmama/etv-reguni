package ch.vd.uniregctb.evenement.fin.permis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Gère la fin d'obtention d'un permis.
 * 
 * @author Pavel BLANCO
 *
 */
public class FinPermisHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil evenement, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		
	}

	@Override
	protected void validateSpecific(EvenementCivil evenement, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		FinPermis finPermis = (FinPermis) evenement;
		/* Seulement le permis C est traité */
		if (finPermis.getTypePermis().equals(EnumTypePermis.ETABLLISSEMENT)) {
			
			PersonnePhysique habitant = getHabitantOrFillErrors(finPermis.getIndividu().getNoTechnique(), erreurs);
			if (habitant == null) {
				return;
			}
			
			boolean isSuisse = false;
			// vérification si la nationalité suisse a été obtenue
			try {
				isSuisse = getService().isSuisse(habitant, finPermis.getDate().getOneDayAfter());
			}
			catch (Exception e) {
				erreurs.add(new EvenementCivilErreur(e.getMessage()));
				return;
			}
			
			if (isSuisse) {
				Audit.info(finPermis.getNumeroEvenement(), "Permis C : l'habitant a obtenu la nationalité suisse, rien à faire");
			}
			else {
				Audit.info(finPermis.getNumeroEvenement(), "Permis C : l'habitant n'a pas obtenu la nationalité suisse, passage en traitement manuel");
				erreurs.add(new EvenementCivilErreur("La fin du permis C doit être traitée manuellement"));
			}
		}
		else {
			Audit.info(finPermis.getNumeroEvenement(), "Permis non C : ignoré");
		}
	}

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
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
	public GenericEvenementAdapter createAdapter() {
		return new FinPermisAdapter();
	}

}