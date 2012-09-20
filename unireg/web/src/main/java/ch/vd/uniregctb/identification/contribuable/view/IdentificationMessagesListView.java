package ch.vd.uniregctb.identification.contribuable.view;

import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuableCriteria;

public class IdentificationMessagesListView extends IdentificationContribuableCriteria{


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
