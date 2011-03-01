package ch.vd.uniregctb.evenement.changement.filiation;

import java.util.List;
import java.util.Set;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class CorrectionFiliationAdapter extends GenericEvenementAdapter {

	protected CorrectionFiliationAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PARENTS);
	}

	@Override
	public void checkCompleteness(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		//les événements de correction de filiation n'ont aucun impact sur le fiscal ==> rien à faire
	}

	@Override
	public void validate(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		//les événements de correction de filiation n'ont aucun impact sur le fiscal ==> rien à faire
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		Audit.info(getNumeroEvenement(), String.format("Correction de filiation de l'individu : %d", getNoIndividu()));
		//les événements de correction de filiation n'ont aucun impact sur le fiscal ==> rien à faire
		return null;
	}
}
