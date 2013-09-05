package ch.vd.unireg.interfaces.efacture.data;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.uniregctb.common.XmlUtils;

public class EtatDestinataire {

	private final String champLibre;
	private final Date dateObtention;
	private final String descriptionRaison;
	private final Integer codeRaison;
	private final TypeEtatDestinataire type;
	private final String email;

	public static EtatDestinataire newEtatDestinataireFactice(TypeEtatDestinataire type) {
		return new EtatDestinataire(type);
	}

	private EtatDestinataire (TypeEtatDestinataire type) {
		this.type = type;
		this.dateObtention = null;
		this.codeRaison = null;
		this.descriptionRaison = "ATTENTION: le service E-facture ne renvoie aucun historique des états du destinataire, cette donnée est générée par UNIREG";
		this.champLibre = StringUtils.EMPTY;
		this.email = null;
	}

	public EtatDestinataire(PayerSituationHistoryEntry payerSituationHistoryEntry) {
		this.champLibre = payerSituationHistoryEntry.getCustomField();
		this.dateObtention = XmlUtils.xmlcal2date(payerSituationHistoryEntry.getDate());
		this.descriptionRaison = payerSituationHistoryEntry.getReasonDescription();
		this.type = TypeEtatDestinataire.valueOf(payerSituationHistoryEntry.getStatus());
		this.codeRaison = payerSituationHistoryEntry.getReasonCode();
		if (type.isInscrit()) {
			// TODO jde Récupérer l'adresse mail dans les données efacture
			this.email = "god@heaven.com";
		}
		else {
			this.email = null;
		}
	}

	public EtatDestinataire(String champLibre, Date dateObtention, String descriptionRaison, Integer codeRaison, TypeEtatDestinataire type, String email) {
		this.champLibre = champLibre;
		this.dateObtention = dateObtention;
		this.descriptionRaison = descriptionRaison;
		this.codeRaison = codeRaison;
		this.type = type;
		if (!type.isInscrit() && StringUtils.isNotBlank(email)) {
			throw new IllegalArgumentException("L'adresse mail n'a de sens que pour les états INSCRIT et INSCRIT_SUSPENDU");
		}
		this.email = StringUtils.trimToNull(email);
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

	public String getEmail() {
		return email;
	}
}
