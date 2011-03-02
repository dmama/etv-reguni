package ch.vd.uniregctb.evenement.civil.interne.changement.origine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.changement.AbstractChangementHandler;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class CorrectionOrigineHandler extends AbstractChangementHandler {

	public void checkCompleteness(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	protected void validateSpecific(EvenementCivilInterne target, List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Throwable.class)
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivilInterne evenement, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		Audit.info(evenement.getNumeroEvenement(), String.format("Traitement correction de origine de l'individu : %d", evenement.getNoIndividu()));
		return super.handle(evenement, warnings);
	}

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new CorrectionOrigineAdapter(event, context, this);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		final Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.CORREC_ORIGINE);
		return types;
	}

}
