package ch.vd.uniregctb.evenement.fin.nationalite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Traitement métier des événements fin d'une nationalité.
 *
 * @author Pavel BLANCO
 *
 */
public class FinNationaliteHandler extends EvenementCivilHandlerBase {

	@Override
	public void checkCompleteness(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		// rien à faire
	}

	@Override
	protected void validateSpecific(EvenementCivil target, List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		FinNationalite finNationalite = (FinNationalite) target;
		switch (finNationalite.getType()) {
		case FIN_NATIONALITE_SUISSE:
			Audit.info(finNationalite.getNumeroEvenement(), "Nationalité suisse : passage en traitement manuel");
			erreurs.add(new EvenementCivilErreur("La fin de la nationalité suisse doit être traitée manuellement"));
			break;
		case FIN_NATIONALITE_NON_SUISSE:
			Audit.info(finNationalite.getNumeroEvenement(), "Nationalité non suisse : ignorée");
			break;
		default:
			Assert.fail();
		}
	}

	@Override
	public void handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		// rien à faire
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.FIN_NATIONALITE_SUISSE);
		types.add(TypeEvenementCivil.FIN_NATIONALITE_NON_SUISSE);
		return types;
	}

	@Override
	public GenericEvenementAdapter createAdapter() {
		return new FinNationaliteAdapter();
	}
}
