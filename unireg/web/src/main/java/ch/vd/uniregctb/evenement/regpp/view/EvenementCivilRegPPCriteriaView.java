package ch.vd.uniregctb.evenement.regpp.view;

import ch.vd.uniregctb.evenement.common.view.EvenementCivilCriteriaView;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class EvenementCivilRegPPCriteriaView extends EvenementCivilCriteriaView<TypeEvenementCivil> {

	private static final long serialVersionUID = 1L;

	@Override
	protected Class<TypeEvenementCivil> getTypeClass() {
		return TypeEvenementCivil.class;
	}
}
