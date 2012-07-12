package ch.vd.unireg.interfaces.efacture.data;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;

public class EtatDestinataire {
	private String champLibre;
	private RegDate dateObtention;
	private String descriptionRaison;
	private Integer codeRaison;
	private TypeEtatDestinataire type;

	public static EtatDestinataire newEtatDestinataireFactice (TypeEtatDestinataire type) {
		return new EtatDestinataire (type);
	}

	private EtatDestinataire (TypeEtatDestinataire type) {
		this.type = type;
		this.dateObtention = RegDate.getEarlyDate();
		this.codeRaison = null;
		this.descriptionRaison ="";
		this.champLibre = "ATTENTION: le service E-facture ne renvoie aucun historique des états du destinataire, cette donnée est générée par UNIREG";
	}

	public EtatDestinataire(PayerSituationHistoryEntry payerSituationHistoryEntry) {
		this.champLibre = payerSituationHistoryEntry.getCustomField();
		this.dateObtention = XmlUtils.xmlcal2regdate(payerSituationHistoryEntry.getDate());
		this.descriptionRaison = payerSituationHistoryEntry.getReasonDescription();
		this.type = TypeEtatDestinataire.valueOf(payerSituationHistoryEntry.getStatus());
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

	public TypeEtatDestinataire getType() {
		return type;
	}

	public Integer getCodeRaison() {
		return codeRaison;
	}

}
