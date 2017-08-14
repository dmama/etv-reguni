package ch.vd.unireg.interfaces.efacture.data;

import java.util.Date;

import ch.vd.evd0025.v1.RegistrationRequestHistoryEntry;
import ch.vd.uniregctb.common.XmlUtils;

/**
 * Representation interne UNIREG de la classe {@link RegistrationRequestHistoryEntry} de l' eVD-25
 */
public class EtatDemande {

	private final String champLibre;
	private final Date date;
	private final Integer codeRaison;
	private final String descriptionRaison;
	private final TypeEtatDemande type;

	public static EtatDemande newEtatDemandeFactice(TypeEtatDemande type) {
		return new EtatDemande(type);
	}

	private EtatDemande(TypeEtatDemande type) {
		this.type = type;
		this.date = null;
		this.codeRaison = null;
		this.descriptionRaison = "ATTENTION: le service E-facture ne renvoie aucun historique des états de cette demande, cette donnée est générée par UNIREG";
		this.champLibre = "";
	}

	public EtatDemande(RegistrationRequestHistoryEntry target) {
		this.champLibre = target.getCustomField();
		this.date = XmlUtils.xmlcal2date(target.getDate());
		this.codeRaison = target.getReasonCode();
		this.descriptionRaison = target.getReasonDescription();
		this.type = TypeEtatDemande.valueOf(target.getStatus(), TypeAttenteDemande.valueOf(target.getReasonCode()));
	}

	public EtatDemande(String champLibre, Date date, Integer codeRaison, String descriptionRaison, TypeEtatDemande type) {
		this.champLibre = champLibre;
		this.date = date;
		this.codeRaison = codeRaison;
		this.descriptionRaison = descriptionRaison;
		this.type = type;
	}

	public String getChampLibre() {
		return champLibre;
	}

	public Date getDate() {
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
