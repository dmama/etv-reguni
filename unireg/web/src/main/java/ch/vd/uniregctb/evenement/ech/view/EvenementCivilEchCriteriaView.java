package ch.vd.uniregctb.evenement.ech.view;

import ch.vd.uniregctb.evenement.common.view.EvenementCivilCriteriaView;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchCriteriaView extends EvenementCivilCriteriaView<TypeEvenementCivilEch> {

	private static final long serialVersionUID = 1L;

	private String actionEvenement;

	public String getActionEvenement() {
		return actionEvenement;
	}

	public void setActionEvenement(String actionEvenement) {
		this.actionEvenement = actionEvenement;
	}

	@Override
	public void setAction(ActionEvenementCivilEch action) {
		super.setAction(action);
		if (action != null) {
			setActionEvenement(action.name());
		}
		else {
			setActionEvenement(TOUS);
		}
	}

	@Override
	protected Class<TypeEvenementCivilEch> getTypeClass() {
		return TypeEvenementCivilEch.class;
	}
}
