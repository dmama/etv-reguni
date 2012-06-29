package ch.vd.unireg.interfaces.efacture.data;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.TypeEtatDestinataire;

public class EtatDestinataire {
	private String champLibre;
	private RegDate dateObtention;
	private String descriptionRaison;
	private Integer codeRaison;
	private TypeEtatDestinataire etatDestinataire;

	public EtatDestinataire(PayerSituationHistoryEntry payerSituationHistoryEntry) {
		this.champLibre = payerSituationHistoryEntry.getCustomField();
		this.dateObtention = XmlUtils.xmlcal2regdate(payerSituationHistoryEntry.getDate());
		this.descriptionRaison = payerSituationHistoryEntry.getReasonDescription();
		this.etatDestinataire = determineStatusDestinataire(payerSituationHistoryEntry.getStatus());
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

	public TypeEtatDestinataire getEtatDestinataire() {
		return etatDestinataire;
	}

	public Integer getCodeRaison() {
		return codeRaison;
	}

	private TypeEtatDestinataire determineStatusDestinataire(PayerStatus status) {
		if(status == null){
			return null;

		}
		switch (status) {
		case DESINSCRIT:
			return TypeEtatDestinataire.DESINSCRIT;
		case DESINSCRIT_SUSPENDU:
			return TypeEtatDestinataire.DESINSCRIT_SUSPENDU;
		case INSCRIT:
			return TypeEtatDestinataire.INSCRIT;
		case INSCRIT_SUSPENDU:
			return TypeEtatDestinataire.INSCRIT_SUSPENDU;
		default:
			throw new IllegalArgumentException("Le statut du destinataire suivant n'est pas reconnu " + status);

		}
	}
}
