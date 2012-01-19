package ch.vd.uniregctb.evenement.civil.interne.annulationpermis;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour la suppresion de l'obtention d'une nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public abstract class SuppressionNationalite extends AnnulationPermisCOuNationaliteSuisse {

	protected SuppressionNationalite(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected SuppressionNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, boolean nationaliteSuisse, EvenementCivilContext context) {
		super(individu, conjoint, nationaliteSuisse ? TypeEvenementCivil.SUP_NATIONALITE_SUISSE : TypeEvenementCivil.SUP_NATIONALITE_NON_SUISSE, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		// rien à faire
	}

}
