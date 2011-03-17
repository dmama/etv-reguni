package ch.vd.uniregctb.evenement.changement.permis;

import java.util.Set;

import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;

public class CorrectionDebutValiditePermisAdapter extends GenericEvenementAdapter {

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.PERMIS);
	}
}
