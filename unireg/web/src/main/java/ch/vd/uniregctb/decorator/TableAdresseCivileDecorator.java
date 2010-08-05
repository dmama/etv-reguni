package ch.vd.uniregctb.decorator;

import ch.vd.uniregctb.tiers.view.AdresseView;

public class TableAdresseCivileDecorator extends TableEntityDecorator{

	public String addRowClass(){
		String cssStyle=super.addRowClass();
		AdresseView adresseView = (AdresseView)getCurrentRowObject();
		if(adresseView !=null){		
			if(!adresseView.isSurVaud()){
				cssStyle += " horscanton";
			}

		}
		return cssStyle;
	}
}
