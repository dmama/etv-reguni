package ch.vd.unireg.webservices.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static final Pattern FROM_STRING_PATTERN = Pattern.compile("([a-zA-Z0-9]{6,})/([0-9]{1,8})");

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

	/**
	 * Construit une instance de {@link UserLogin} à partir d'une chaîne de caractères qui suit le pattern {@link #FROM_STRING_PATTERN}
	 * @param str la chaîne de caractères
	 * @return une instance de {@link UserLogin}
	 * @throws IllegalArgumentException si la chaîne de caractères en entrée ne suit pas le pattern {@link #FROM_STRING_PATTERN}
	 */
	public static UserLogin fromString(String str) {
		final Matcher matcher = FROM_STRING_PATTERN.matcher(str);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid value for user login");
		}
		return new UserLogin(matcher.group(1), Integer.parseInt(matcher.group(2)));
	}

	@Override
	public String toString() {
		return "UserLogin{" +
				"userId='" + userId + '\'' +
				", oid=" + oid +
				'}';
	}
}
