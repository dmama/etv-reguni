package ch.vd.uniregctb.di.view;

import java.util.List;

import ch.vd.uniregctb.general.view.TiersGeneralView;

/**
 * Vue pour les listes de DI
 *
 * @author xcifde
 *
 */
public class DeclarationImpotListView implements DeclarationImpotView {

	private TiersGeneralView contribuable;

	private List<DeclarationImpotDetailView> dis;

	private boolean isAllowedEmission;
	
	public boolean isAllowedEmission() {
		return isAllowedEmission;
	}

	public void setAllowedEmission(boolean isAllowedEmission) {
		this.isAllowedEmission = isAllowedEmission;
	}

	@Override
	public TiersGeneralView getContribuable() {
		return contribuable;
	}

	public void setContribuable(TiersGeneralView contribuable) {
		this.contribuable = contribuable;
	}

	public List<DeclarationImpotDetailView> getDis() {
		return dis;
	}

	public void setDis(List<DeclarationImpotDetailView> dis) {
		this.dis = dis;
	}



}
