package ch.vd.uniregctb.declaration.ordinaire;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

public class ImpressionConfirmationDelaiHelperParams {
	
	private final DeclarationImpotOrdinaire di;
	private final RegDate dateAccord;
	private final String traitePar;
	private final String tel;
	private final String adrMsg;
	
	

	public ImpressionConfirmationDelaiHelperParams(
			DeclarationImpotOrdinaire di, RegDate dateAccord, String traitePar, String tel, String adrMsg) {
		super();
		this.di = di;
		this.dateAccord = dateAccord;
		this.traitePar = traitePar;
		this.tel = tel;
		this.adrMsg = adrMsg;
	}

	public DeclarationImpotOrdinaire getDi() {
		return di;
	}

	public String getTraitePar() {
		return traitePar;
	}

	public RegDate getDateAccord() {
		return dateAccord;
	}
	
	public String getNoTelephone(){
		return tel;
	}
	
	public String getAdrMsg(){
		return adrMsg;
	}
	

}
