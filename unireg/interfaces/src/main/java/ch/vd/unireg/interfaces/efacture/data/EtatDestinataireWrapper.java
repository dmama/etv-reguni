package ch.vd.unireg.interfaces.efacture.data;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;

public class EtatDestinataireWrapper {
	private String champLibre;
	private RegDate dateObtention;
	private String descriptionRaison;
	private Integer codeRaison;
	private TypeStatusDestinataire statusDestinataire;

	public EtatDestinataireWrapper(PayerSituationHistoryEntry payerSituationHistoryEntry) {
		this.champLibre = payerSituationHistoryEntry.getCustomField();
		this.dateObtention = XmlUtils.xmlcal2regdate(payerSituationHistoryEntry.getDate());
		this.descriptionRaison = payerSituationHistoryEntry.getReasonDescription();
		this.statusDestinataire = determineStatusDestinataire(payerSituationHistoryEntry.getStatus());
		this.codeRaison = payerSituationHistoryEntry.getReasonCode();

	}

	public String getChampLibre() {
		return champLibre;
	}

	public RegDate getDateObtention() {
		return dateObtention;
	}

	public String getDescriptionRaison() {
		return descriptionRaison;
	}

	public TypeStatusDestinataire getStatusDestinataire() {
		return statusDestinataire;
	}

	public Integer getCodeRaison() {
		return codeRaison;
	}

	private TypeStatusDestinataire determineStatusDestinataire(PayerStatus status) {
		if(status == null){
			return null;

		}
		switch (status) {
		case DESINSCRIT:
			return TypeStatusDestinataire.DESINSCRIT;
		case DESINSCRIT_SUSPENDU:
			return TypeStatusDestinataire.DESINSCRIT_SUSPENDU;
		case INSCRIT:
			return TypeStatusDestinataire.INSCRIT;
		case INSCRIT_SUSPENDU:
			return TypeStatusDestinataire.INSCRIT_SUSPENDU;
		default:
			throw new IllegalArgumentException("Le statut du destinataire suivant n'est pas reconnu " + status);

		}
	}
}
