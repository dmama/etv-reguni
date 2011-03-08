package ch.vd.uniregctb.evenement.civil.interne.fin.nationalite;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Adapter pour la fin obtention d'une nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public class FinNationalite extends EvenementCivilInterne {

	protected FinNationalite(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected FinNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, boolean nationaliteSuisse, EvenementCivilContext context) {
		super(individu, conjoint, nationaliteSuisse ? TypeEvenementCivil.FIN_NATIONALITE_SUISSE : TypeEvenementCivil.FIN_NATIONALITE_NON_SUISSE, date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// rien à faire
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		switch (getType()) {
		case FIN_NATIONALITE_SUISSE:
			Audit.info(getNumeroEvenement(), "Nationalité suisse : passage en traitement manuel");
			erreurs.add(new EvenementCivilExterneErreur("La fin de la nationalité suisse doit être traitée manuellement"));
			break;
		case FIN_NATIONALITE_NON_SUISSE:
			Audit.info(getNumeroEvenement(), "Nationalité non suisse : ignorée");
			break;
		default:
			Assert.fail();
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		// rien à faire
		return null;
	}
}
