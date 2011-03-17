package ch.vd.uniregctb.evenement.changement.filiation;

import java.util.Set;

import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;

public class CorrectionFiliationAdapter extends GenericEvenementAdapter {

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PARENTS);
	}
}
