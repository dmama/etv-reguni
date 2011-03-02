package ch.vd.uniregctb.evenement.civil.interne.fin.nationalite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
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

/**
 * Traitement métier des événements fin d'une nationalité.
 *
 * @author Pavel BLANCO
 *
 */
public class FinNationaliteHandler extends EvenementCivilHandlerBase {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		// rien à faire
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		FinNationalite finNationalite = (FinNationalite) target;
		switch (finNationalite.getType()) {
		case FIN_NATIONALITE_SUISSE:
			Audit.info(finNationalite.getNumeroEvenement(), "Nationalité suisse : passage en traitement manuel");
			erreurs.add(new EvenementCivilExterneErreur("La fin de la nationalité suisse doit être traitée manuellement"));
			break;
		case FIN_NATIONALITE_NON_SUISSE:
			Audit.info(finNationalite.getNumeroEvenement(), "Nationalité non suisse : ignorée");
			break;
		default:
			Assert.fail();
		}
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		// rien à faire
		return null;
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.FIN_NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.FIN_NATIONALITE_NON_SUISSE);
		return types;
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new FinNationaliteAdapter(event, context, this);
	}
}
