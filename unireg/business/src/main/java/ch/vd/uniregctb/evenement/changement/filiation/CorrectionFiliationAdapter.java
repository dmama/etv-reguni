package ch.vd.uniregctb.evenement.changement.filiation;

import java.util.Set;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;

public class CorrectionFiliationAdapter extends GenericEvenementAdapter {

	@Override
	protected void fillRequiredParts(Set<EnumAttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(EnumAttributeIndividu.PARENTS);
	}
}
