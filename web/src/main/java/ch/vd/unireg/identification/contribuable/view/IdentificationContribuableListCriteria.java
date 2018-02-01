package ch.vd.unireg.identification.contribuable.view;

import ch.vd.unireg.evenement.identification.contribuable.IdentificationContribuableCriteria;

public class IdentificationContribuableListCriteria extends IdentificationContribuableCriteria{


	private String userCourant;

	public String getUserCourant() {
		return userCourant;
	}

	public void setUserCourant(String userCourant) {
		this.userCourant = userCourant;
	}

	private Long tabIdsMessages[];

	public Long[] getTabIdsMessages() {
		return tabIdsMessages;
	}

	public void setTabIdsMessages(Long[] tabIdsMessages) {
		this.tabIdsMessages = tabIdsMessages;
	}


}
