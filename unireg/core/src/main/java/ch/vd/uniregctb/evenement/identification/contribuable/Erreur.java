package ch.vd.uniregctb.evenement.identification.contribuable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;

/**
 * Contient le détail de l'erreur dans le traitement d'une requête d'identification d'un contribuable.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Embeddable
public class Erreur {

	public enum TypeErreur {
		TECHNIQUE, METIER
	}

	/**
	 * Le type de l'erreur : technique ou métier.
	 */
	private TypeErreur type;

	/**
	 * Code identificateur de l’erreur.
	 */
	private String code;

	/**
	 * Désignation de l’erreur (Message clair à afficher ou à communiquer à un utilisateur).
	 */
	private String message;

	public Erreur() {
	}

	public Erreur(TypeErreur type, String code, String message) {
		this.type = type;
		this.code = code;
		this.message = message;
	}

	@Column(name = "ERREUR_TYPE", length = 9)
	@Type(type = "ch.vd.uniregctb.hibernate.identification.contribuable.TypeErreurIdentCtbUserType")
	public TypeErreur getType() {
		return type;
	}

	public void setType(TypeErreur type) {
		this.type = type;
	}

	@Column(name = "ERREUR_CODE", length = 20)
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Column(name = "ERREUR_MESSAGE", length = 1000)
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
