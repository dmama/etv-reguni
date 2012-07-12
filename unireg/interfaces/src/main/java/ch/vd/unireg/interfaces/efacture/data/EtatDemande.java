package ch.vd.unireg.interfaces.efacture.data;

import ch.vd.evd0025.v1.RegistrationRequestHistoryEntry;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.XmlUtils;

/**
 * Representation interne UNIREG de la classe {@link RegistrationRequestHistoryEntry} de l' eVD-25
 */
public class EtatDemande {

	private String champLibre;
	private RegDate date;
	private Integer codeRaison;
	private String descriptionRaison;
	private TypeEtatDemande type;

	public static EtatDemande newEtatDemandeFactice (TypeEtatDemande type) {
		return new EtatDemande(type);
	}

	private EtatDemande (TypeEtatDemande type) {
		this.type = type;
		this.date = RegDate.getEarlyDate();
		this.codeRaison = null;
		this.descriptionRaison ="";
		this.champLibre = "ATTENTION: le service E-facture ne renvoie aucun historique des états de cette demande, cette donnée est générée par UNIREG";
	}

	public EtatDemande(RegistrationRequestHistoryEntry target) {
		this.champLibre = target.getCustomField();
		this.date = XmlUtils.xmlcal2regdate(target.getDate());
		this.codeRaison = target.getReasonCode();
		this.descriptionRaison = target.getReasonDescription();
		this.type = TypeEtatDemande.valueOf(target.getStatus(), TypeAttenteDemande.valueOf(target.getReasonCode()));
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

	public TypeEtatDemande getType() {
		return type;
	}
}
