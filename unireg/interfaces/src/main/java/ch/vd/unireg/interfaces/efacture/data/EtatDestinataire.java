package ch.vd.unireg.interfaces.efacture.data;

import java.util.Date;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.uniregctb.common.XmlUtils;

public class EtatDestinataire {
	private String champLibre;
	private Date dateObtention;
	private String descriptionRaison;
	private Integer codeRaison;
	private TypeEtatDestinataire type;

	public static EtatDestinataire newEtatDestinataireFactice(TypeEtatDestinataire type) {
		return new EtatDestinataire(type);
	}

	private EtatDestinataire (TypeEtatDestinataire type) {
		this.type = type;
		this.dateObtention = null;
		this.codeRaison = null;
		this.descriptionRaison = "ATTENTION: le service E-facture ne renvoie aucun historique des états du destinataire, cette donnée est générée par UNIREG";
		this.champLibre ="";
	}

	public EtatDestinataire(PayerSituationHistoryEntry payerSituationHistoryEntry) {
		this.champLibre = payerSituationHistoryEntry.getCustomField();
		this.dateObtention = XmlUtils.xmlcal2date(payerSituationHistoryEntry.getDate());
		this.descriptionRaison = payerSituationHistoryEntry.getReasonDescription();
		this.type = TypeEtatDestinataire.valueOf(payerSituationHistoryEntry.getStatus());
		this.codeRaison = payerSituationHistoryEntry.getReasonCode();
	}

	public EtatDestinataire(String champLibre, Date dateObtention, String descriptionRaison, Integer codeRaison, TypeEtatDestinataire type) {
		this.champLibre = champLibre;
		this.dateObtention = dateObtention;
		this.descriptionRaison = descriptionRaison;
		this.codeRaison = codeRaison;
		this.type = type;
	}

	public String getChampLibre() {
		return champLibre;
	}

	public Date getDateObtention() {
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
