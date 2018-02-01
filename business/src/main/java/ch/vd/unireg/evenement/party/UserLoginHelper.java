package ch.vd.unireg.evenement.party;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ch.vd.unireg.xml.common.v2.UserLogin;

/**
 * Classe de méthodes utilitaires autour de la classe UserLogin
 */
public abstract class UserLoginHelper {

	private static final Pattern USER_LOGIN_PATTERN = Pattern.compile("([a-zA-Z0-9]+)/([0-9]+)");

	/**
	 * Décomposition en partie "user" et "oid" d'une donnée {@link UserLogin}
	 * @param userLogin données en entrée
	 * @return Données décomposées
	 * @throws IllegalArgumentException en cas de valeur invalide
	 */
	public static Pair<String, Integer> parse(UserLogin userLogin) {
		final Matcher matcher = USER_LOGIN_PATTERN.matcher(userLogin.getValue());
		if (!matcher.matches()) {
			throw new IllegalArgumentException("La donnée de login n'est pas correctement formattée.");
		}
		return Pair.of(matcher.group(1), Integer.parseInt(matcher.group(2)));
	}

	/**
	 * Recompose un {@link UserLogin} à partir de ses composants de base
	 * @param user le visa de l'utilisateur
	 * @param oid l'OID concerné
	 * @return une nouvelle instance de {@link UserLogin} qui va bien
	 * @throws IllegalArgumentException en cas de valeur invalide
	 */
	public static UserLogin of(String user, int oid) {
		final String packed = String.format("%s/%d", StringUtils.trimToEmpty(user), oid);
		final Matcher matcher = USER_LOGIN_PATTERN.matcher(packed);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Données invalides : user '" + user + "' et oid " + oid);
		}
		return new UserLogin(packed);
	}
}
