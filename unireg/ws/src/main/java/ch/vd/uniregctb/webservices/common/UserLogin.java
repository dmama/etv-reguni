package ch.vd.uniregctb.webservices.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Cette classe doit contenir toutes les informations nécessaires à l'identification d'un utilisateur.
 * <p>
 * Ces informations sont utilisées par Unireg pour gérer le contrôle d'accès au niveau de chaque ressources. Les informations spécifiées
 * doivent donc correspondre à l'utilisateur physique (= la personne derrière le clavier) et non à l'utilisateur technique (= l'application
 * demandeuse).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UserLogin")
public class UserLogin {

	/**
	 * L'identification de l'utilisateur telle que définie par IAM.
	 */
	@XmlElement(required = true)
	public String userId;

	/**
	 * L'id de l'office d'impôt sélectionné par l'utilisateur.
	 * <p>
	 * Dans la majorité des cas cette information peut être déduite à partir de l'id de l'utilisateur. Cependant certains utilisateurs
	 * possèdent plus d'un rattachement à un office d'impôt. Ces utilisateurs doivent sélectionner un office d'impôt bien précis au moment
	 * du login, et ce choix doit être propagé à tous les systèmes.
	 */
	@XmlElement(required = true)
	public Integer oid;

	public UserLogin() {
	}

	public UserLogin(String userId, Integer oid) {
		this.userId = userId;
		this.oid = oid;
	}

	@Override
	public String toString() {
		return "UserLogin{" +
				"userId='" + userId + '\'' +
				", oid=" + oid +
				'}';
	}
}
