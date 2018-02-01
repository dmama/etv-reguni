package ch.vd.unireg.evenement.regpp.view;

import ch.vd.unireg.evenement.common.view.EvenementCivilCriteriaView;
import ch.vd.unireg.type.TypeEvenementCivil;

public class EvenementCivilRegPPCriteriaView extends EvenementCivilCriteriaView<TypeEvenementCivil> {

	private static final long serialVersionUID = -4778412566605280203L;

	@Override
	protected Class<TypeEvenementCivil> getTypeClass() {
		return TypeEvenementCivil.class;
	}
}
