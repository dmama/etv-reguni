package ch.vd.uniregctb.evenement.civil.interne.changement.nationalite;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class RemiseBlancDateFinNationalite extends EvenementCivilInterne {

	protected RemiseBlancDateFinNationalite(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected RemiseBlancDateFinNationalite(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, boolean suisse, EvenementCivilContext context) {
		super(individu, conjoint, (suisse ? TypeEvenementCivil.ANNUL_DATE_FIN_NATIONALITE_SUISSE : TypeEvenementCivil.ANNUL_DATE_FIN_NATIONALITE_NON_SUISSE), date, numeroOfsCommuneAnnonce, context);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// rien à faire
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		switch (getType()) {
		case ANNUL_DATE_FIN_NATIONALITE_SUISSE:
			Audit.info(getNumeroEvenement(), "Remise à blanc de la date de fin de la nationalité suisse : passage en traitement manuel");
			erreurs.add(new EvenementCivilExterneErreur("La remise à blanc de la date de fin de la nationalité suisse doit être traitée manuellement"));
			break;
		case ANNUL_DATE_FIN_NATIONALITE_NON_SUISSE:
			Audit.info(getNumeroEvenement(), "Remise à blanc de la date de fin d'une nationalité non suisse : ignorée");
			break;
		default:
			Assert.fail();
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		// rien à faire
		return null;
	}
}
