package ch.vd.unireg.evenement.ech.view;

import java.io.Serializable;
import java.util.List;

import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.unireg.evenement.common.view.EvenementCivilDetailView;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 */
public class EvenementCivilEchDetailView extends EvenementCivilDetailView implements Serializable {

	private static final long serialVersionUID = -3670349543387092473L;

	private TypeEvenementCivilEch evtType;
	private ActionEvenementCivilEch evtAction;
	private Long refEvtId;
	private boolean recyclable;
	private EvenementCivilEchGrappeView grappeComplete;
	private List<EvenementCivilEchBasicInfo> nonTraitesSurMemeIndividu;

	private boolean embedded;

	private Long nextId;

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
	public ActionEvenementCivilEch getEvtAction() {
		return evtAction;
	}

	public void setEvtAction(ActionEvenementCivilEch evtAction) {
		this.evtAction = evtAction;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Long getRefEvtId() {
		return refEvtId;
	}

	public void setRefEvtId(Long refEvtId) {
		this.refEvtId = refEvtId;
	}

	@SuppressWarnings("UnusedDeclaration")
	public EvenementCivilEchGrappeView getGrappeComplete() {
		return grappeComplete;
	}

	public void setGrappeComplete(EvenementCivilEchGrappeView grappeComplete) {
		this.grappeComplete = grappeComplete;
	}

	@SuppressWarnings("UnusedDeclaration")
	public List<EvenementCivilEchBasicInfo> getNonTraitesSurMemeIndividu() {
		return nonTraitesSurMemeIndividu;
	}

	public void setNonTraitesSurMemeIndividu(List<EvenementCivilEchBasicInfo> nonTraitesSurMemeIndividu) {
		this.nonTraitesSurMemeIndividu = nonTraitesSurMemeIndividu;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public Long getNextId() {
		return nextId;
	}

	public void setNextId(Long nextId) {
		this.nextId = nextId;
	}
}
