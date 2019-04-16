package ch.vd.unireg.declaration.ordinaire.pp;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;

public class ImpressionConfirmationDelaiHelperParams {

	private final DeclarationImpotOrdinaire di;
	private final RegDate dateAccord;
	private final String traitePar;
	private final String tel;
	private final String adrMsg;
	private final Long idDelai;
	private final Date logCreationDateDelai;
	private String serviceTraiterPar;
	private RegDate dateExpedition;


	public ImpressionConfirmationDelaiHelperParams(
			DeclarationImpotOrdinaire di, RegDate dateAccord, String traitePar, String tel, String adrMsg, Long idDelai, Date logCreationDateDelai, String serviceTraiterPar, RegDate dateExpedition) {
		super();
		this.di = di;
		this.dateAccord = dateAccord;
		this.traitePar = traitePar;
		this.tel = tel;
		this.adrMsg = adrMsg;
		this.idDelai = idDelai;
		this.logCreationDateDelai = logCreationDateDelai;
		this.serviceTraiterPar = serviceTraiterPar;
		this.dateExpedition = dateExpedition;
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
		this.serviceTraiterPar=null;
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

	public String getServiceTraiterPar() {
		return serviceTraiterPar;
	}

	public RegDate getDateExpedition() {
		return dateExpedition;
	}
}
