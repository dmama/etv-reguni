package ch.vd.unireg.interfaces.efacture.data;

import ch.vd.evd0025.v1.RegistrationRequestHistoryEntry;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;

public class EtatDemandeWrapper {
	private String champLibre;
	private RegDate date;
	private Integer codeRaison;
	private String descriptionRaison;
	private TypeStatusDemande typeStatusDemande;


	public EtatDemandeWrapper(RegistrationRequestHistoryEntry target) {
		this.champLibre = target.getCustomField();
		this.date = XmlUtils.xmlcal2regdate(target.getDate());
		this.codeRaison = target.getReasonCode();
		this.descriptionRaison = target.getReasonDescription();
		this.typeStatusDemande = determineStatusDemande(target.getStatus());

	}

	private TypeStatusDemande determineStatusDemande(RegistrationRequestStatus status) {
		switch (status){
		 case IGNOREE:
			 return TypeStatusDemande.IGNOREE;
		 case A_TRAITER:
			 return TypeStatusDemande.A_TRAITE;
		 case REFUSEE:
			 return TypeStatusDemande.REFUSEE;
		 case VALIDATION_EN_COURS:
			 return TypeStatusDemande.VALIDATION_EN_COURS;
		 case VALIDEE:
			 return TypeStatusDemande.VALIDEE;
		 default:
			 throw new IllegalArgumentException("Le statut de demande suivant n'est pas reconnu "+ status);

		}
	}

	public String getChampLibre() {
		return champLibre;
	}

	public RegDate getDate() {
		return date;
	}

	public Integer getCodeRaison() {
		return codeRaison;
	}

	public String getDescriptionRaison() {
		return descriptionRaison;
	}

	public TypeStatusDemande getStatusDemande() {
		return typeStatusDemande;
	}
}
