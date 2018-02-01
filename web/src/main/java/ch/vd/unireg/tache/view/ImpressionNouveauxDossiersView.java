package ch.vd.unireg.tache.view;

import java.io.Serializable;

public class ImpressionNouveauxDossiersView implements Serializable {

	private static final long serialVersionUID = -2605087936476398394L;

	private Long tabIdsDossiers[];

	public Long[] getTabIdsDossiers() {
		return tabIdsDossiers;
	}

	public void setTabIdsDossiers(Long[] tabIdsDossiers) {
		this.tabIdsDossiers = tabIdsDossiers;
	}

}
