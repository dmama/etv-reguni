package ch.vd.uniregctb.evenement.common;

import java.util.List;

import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class EvenementCivilCoupleHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		
		/* Vérification de la présence de l'individu principal */
		Individu individuPrincipal = target.getIndividu();
		if (individuPrincipal == null) {
			erreurs.add(new EvenementCivilErreur("Impossible de récupérer l'individu concerné par cet événement [#" + target.getNumeroEvenement() + ", type: '" + target.getType().getDescription() + "']"));
		}
		else {
			/*
			 * si l'individu est marié, ou pacsé, on regarde si le conjoint a
			 * lui aussi fait l'objet d'un événement
			 */
			if (individuPrincipal.getConjoint() != null && !EtatCivilHelper.estSepare(individuPrincipal.getEtatCivil(target.getDate()))) {

				// erreur si l'événement conjoint n'a pas été reçu
				if (target.getConjoint() == null) {
					erreurs.add(new EvenementCivilErreur("L'évenement '" + target.getType().getDescription() + "' du conjoint n'a pas été reçu"));
				}

				// erreur si l'id du conjoint reçu ne correspond pas à celui de
				// l'état civil
				else if (target.getConjoint().getNoTechnique() != individuPrincipal.getConjoint().getNoTechnique()) {
					erreurs.add(new EvenementCivilErreur("Mauvais regroupement : le conjoint déclaré dans l'événement et celui dans le registre civil diffèrent"));
				}
			}
		}
		
	}

}
