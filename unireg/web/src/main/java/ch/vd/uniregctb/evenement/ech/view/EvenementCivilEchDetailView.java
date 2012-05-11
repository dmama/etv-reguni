package ch.vd.uniregctb.evenement.ech.view;

import java.io.Serializable;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.common.view.EvenementCivilDetailView;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 */
public class EvenementCivilEchDetailView extends EvenementCivilDetailView implements Serializable {

	private static final long serialVersionUID = 7000273271978471047L;

	private TypeEvenementCivilEch evtType;
	private ActionEvenementCivilEch evtAction;
	private boolean recyclable;
	private EvenementCivilEchBasicInfo evtPrioritaire;
	private int totalAutresEvenementsAssocies;

	@SuppressWarnings("unused")
	public TypeEvenementCivilEch getEvtType() {
		return evtType;
	}

	public void setEvtType(TypeEvenementCivilEch evtType) {
		this.evtType = evtType;
	}

	@SuppressWarnings("unused")
	public boolean isRecyclable() {
		return recyclable;
	}

	public void setRecyclable(boolean recyclable) {
		this.recyclable = recyclable;
	}

	@SuppressWarnings("unused")
	public EvenementCivilEchBasicInfo getEvtPrioritaire() {
		return evtPrioritaire;
	}

	public void setEvtPrioritaire(EvenementCivilEchBasicInfo evt) {
		if (getIndividu() == null || getIndividu().getNumeroIndividu() == null) {
			throw new NullPointerException("le champs individu de l'objet doit être renseigné avant de pouvoir utiliser cette méthode");
		}
		if (evt.getNoIndividu() != getIndividu().getNumeroIndividu()) {
			throw new RuntimeException(
					String.format(
							"impossible de définir un événement prioritaire qui porte sur un autre numero d'individu(attendu: %d reçu: %d)",
							getIndividu().getNumeroIndividu(), evt.getId()));
		}
		this.evtPrioritaire = evt;
	}

	public void setTotalAutresEvenementsAssocies(int totalAutresEvenementsAssocies) {
		this.totalAutresEvenementsAssocies = totalAutresEvenementsAssocies;
	}

	@SuppressWarnings("unused")
	public int getTotalAutresEvenementsAssocies() {
		return totalAutresEvenementsAssocies;
	}

	@SuppressWarnings("unused")
	public ActionEvenementCivilEch getEvtAction() {
		return evtAction;
	}

	public void setEvtAction(ActionEvenementCivilEch evtAction) {
		this.evtAction = evtAction;
	}
}
