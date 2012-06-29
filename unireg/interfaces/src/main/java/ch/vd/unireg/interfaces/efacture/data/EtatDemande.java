package ch.vd.unireg.interfaces.efacture.data;

import ch.vd.evd0025.v1.RegistrationRequestHistoryEntry;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.TypeEtatDemande;

public class EtatDemande {
	private String champLibre;
	private RegDate date;
	private Integer codeRaison;
	private String descriptionRaison;
	private TypeEtatDemande typeEtatDemande;



	public EtatDemande(RegistrationRequestHistoryEntry target) {
		this.champLibre = target.getCustomField();
		this.date = XmlUtils.xmlcal2regdate(target.getDate());
		this.codeRaison = target.getReasonCode();
		this.descriptionRaison = target.getReasonDescription();
		this.typeEtatDemande = determineStatusDemande(target.getStatus());

	}

	private TypeEtatDemande determineStatusDemande(RegistrationRequestStatus status) {
		switch (status){
		 case IGNOREE:
			 return TypeEtatDemande.IGNOREE;
		 case A_TRAITER:
			 return TypeEtatDemande.A_TRAITER;
		 case REFUSEE:
			 return TypeEtatDemande.REFUSEE;
		 case VALIDATION_EN_COURS:
			 return TypeEtatDemande.VALIDATION_EN_COURS;
		 case VALIDEE:
			 return TypeEtatDemande.VALIDEE;
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

	public TypeEtatDemande getTypeEtatDemande() {
		return typeEtatDemande;
	}
}
