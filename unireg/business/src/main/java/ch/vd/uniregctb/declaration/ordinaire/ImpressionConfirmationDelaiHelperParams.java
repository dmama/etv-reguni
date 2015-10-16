package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;

public class ImpressionConfirmationDelaiHelperParams {

	private final DeclarationImpotOrdinaire di;
	private final RegDate dateAccord;
	private final String traitePar;
	private final String tel;
	private final String adrMsg;
	private final Long idDelai;
	private final Date logCreationDateDelai;


	public ImpressionConfirmationDelaiHelperParams(
			DeclarationImpotOrdinaire di, RegDate dateAccord, String traitePar, String tel, String adrMsg, Long idDelai, Date logCreationDateDelai) {
		super();
		this.di = di;
		this.dateAccord = dateAccord;
		this.traitePar = traitePar;
		this.tel = tel;
		this.adrMsg = adrMsg;
		this.idDelai = idDelai;
		this.logCreationDateDelai = logCreationDateDelai;
	}

	public ImpressionConfirmationDelaiHelperParams(RegDate dateAccord, Long idDelai, Date logCreationDateDelai) {
		super();
		this.di = null;
		this.dateAccord = dateAccord;
		this.traitePar = null;
		this.tel = null;
		this.adrMsg = null;
		this.idDelai = idDelai;
		this.logCreationDateDelai = logCreationDateDelai;
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

	public String getNoTelephone() {
		return tel;
	}

	public String getAdrMsg() {
		return adrMsg;
	}

	public Long getIdDelai() {
		return idDelai;
	}

	public Date getLogCreationDateDelai() {
		return logCreationDateDelai;
	}
}
