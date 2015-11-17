package ch.vd.uniregctb.evenement.organisation.view;

import java.io.Serializable;
import java.util.List;

import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 */
public class EvenementOrganisationDetailView extends EvenementOrganisationDetailBaseView implements Serializable {

	private static final long serialVersionUID = 4622877003735146179L;

	private TypeEvenementOrganisation evtType;
	private Long refEvtId;
	private boolean recyclable;
	private EvenementOrganisationGrappeView grappeComplete;
	private List<EvenementOrganisationBasicInfo> nonTraitesSurMemeOrganisation;

	@SuppressWarnings("unused")
	public TypeEvenementOrganisation getEvtType() {
		return evtType;
	}

	public void setEvtType(TypeEvenementOrganisation evtType) {
		this.evtType = evtType;
	}

	@SuppressWarnings("unused")
	public boolean isRecyclable() {
		return recyclable;
	}

	public void setRecyclable(boolean recyclable) {
		this.recyclable = recyclable;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Long getRefEvtId() {
		return refEvtId;
	}

	public void setRefEvtId(Long refEvtId) {
		this.refEvtId = refEvtId;
	}

	@SuppressWarnings("UnusedDeclaration")
	public EvenementOrganisationGrappeView getGrappeComplete() {
		return grappeComplete;
	}

	public void setGrappeComplete(EvenementOrganisationGrappeView grappeComplete) {
		this.grappeComplete = grappeComplete;
	}

	@SuppressWarnings("UnusedDeclaration")
	public List<EvenementOrganisationBasicInfo> getNonTraitesSurMemeOrganisation() {
		return nonTraitesSurMemeOrganisation;
	}

	public void setNonTraitesSurMemeOrganisation(List<EvenementOrganisationBasicInfo> nonTraitesSurMemeOrganisation) {
		this.nonTraitesSurMemeOrganisation = nonTraitesSurMemeOrganisation;
	}
}
