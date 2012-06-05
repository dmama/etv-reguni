package ch.vd.uniregctb.iban;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.uniregctb.common.FormatNumeroHelper;


/**
 * Validateur de code IBAN.
 *
 * @author Ludovic Bertin (OOSphere)
 *
 */
public class IbanValidator {

	/**
	 * Fichier contenant les longueurs valides d'IBAN pour chaque pays.
	 */
	private static final String ISO_3166_1_PROPERTIES_FILENAME = "iban/ISO-3166-1.properties";

	private static final Logger LOGGER = Logger.getLogger(IbanValidator.class);

	private static final BigInteger MODULO = BigInteger.valueOf(97L);

	/**
	 * Liste des longueurs IBAN par pays.
	 */
	private static Properties longueursValides = null;


	/*
	 * Initialisation :
	 * ----------------
	 * 	 	- chargement de la liste des longueurs IBAN par pays
	 */
	static {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Initialisation du service IbanValidator");

		try {
			InputStream isoFile = IbanValidator.class.getClassLoader().getResourceAsStream(ISO_3166_1_PROPERTIES_FILENAME);
			longueursValides = new Properties();
			longueursValides.load( isoFile );

			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Le service IbanValidator a été initialisé avec succès");

		} catch (IOException e) {
			LOGGER.fatal("Problème lors du chargement du fichier ISO-3166-1.properties", e);
		}
	}

	/**
	 * Valide le code iban
	 * @param iban
	 */
	public final void validate(String iban) throws IbanValidationException {
		if (iban == null) {
			throw new IbanValidationException("Le code IBAN n'est pas renseigné");
		}

		iban = FormatNumeroHelper.removeSpaceAndDash(iban);
		iban = iban.toUpperCase();		
		validateLength(iban);
		validateFormat(iban);
		validatePlausability(iban);
	}

	public boolean isValidIban(String iban){
		final String msgErreur = getIbanValidationError(iban);
		return msgErreur == null;
	}

	/**
	 * @param iban numéro IBAN à valider
	 * @return <code>null</code> si l'IBAN est valide, explication textuelle de l'erreur sinon
	 */
	public String getIbanValidationError(String iban) {
		String msg = null;
		try {
			validate(iban);
		}
		catch (IbanValidationException e) {
			msg = e.getMessage();
			if (StringUtils.isBlank(msg)) {
				msg = "erreur générique";
			}
		}
		return msg;
	}

	/**
	 * Valide la longueur de l'iban. Utilise la table ISO 3166-1 pour connaitre
	 * la longueur requise selon le pays.
	 *
	 * @param iban
	 *            le code IBAN à tester
	 * @throws IbanBadLengthException
	 *             si la longueur est incorrecte.
	 * @throws IbanUnknownCountryException
	 *             si le code pays est inconnu
	 */
	private static void validateLength(String iban) throws IbanBadLengthException, IbanUnknownCountryException {
		// longueur < 2
		if (iban.length() < 2) {
			throw new IbanBadLengthException();
		}

		// recherche de la longueur attendue
		final String codePays = extractCodePays(iban);
		final int longueurAttendue = Integer.parseInt(longueursValides.getProperty(codePays, "-1"));

		// code pays non trouvé
		if (longueurAttendue == -1) {
			throw new IbanUnknownCountryException();
		}

		// longueur incorrecte
		if (iban.length() != longueurAttendue) {
			throw new IbanBadLengthException();
		}
	}

	/**
	 * Valide le format de l'iban.
	 * Caractères alphanumériques sans espaces et pour le cas de la suisse,
	 * les caractères de la 3ème à la 9ème position doivent être numériques.
	 *
	 * @param iban
	 *            le code IBAN à tester
	 * @throws IbanBadFormatException
	 *             si le format est incorrect.
	 */
	private static void validateFormat(String iban) throws IbanBadFormatException {

		// Tous les caractères doivent être alphanumériques
		final String ibanMajuscules = iban.toUpperCase();
		for (int i=0; i<ibanMajuscules.length(); i++) {
			if (!Character.isLetterOrDigit(ibanMajuscules.charAt(i))) {
				throw new IbanBadFormatException("les caractères doivent être alpha-numériques");
			}
		}

		// cas spécial de la Suisse : les positions 3 à 9 doivent être numériques
		if (isSuisse(iban)) {
			for (int i=2; i<9; i++) {
				if (!Character.isDigit(ibanMajuscules.charAt(i))) {
					throw new IbanBadFormatException("pour la Suisse, les caractères de la 3ème à la 9ème position doivent être numériques");
				}
			}
		}
	}

	/**
	 * Valide la plausibilité de l'iban.
	 * Utilise pour cela la méthode modulo 97-10
	 *
	 * @param iban
	 *            le code IBAN à tester
	 * @throws IbanNonPlausibleException
	 *             si l'iban n'est pas plausible.
	 */
	private static void validatePlausability(String iban) throws IbanNonPlausibleException {
		// on déplace les 4 premiers caractères à la fin de la chaine
        final String ibanPermute = String.format("%s%s", iban.substring(4), iban.substring(0, 4));

        // on convertit le code IBAN en valeur numérique
        final StringBuilder ibanNumeriqueStr = new StringBuilder();
        for (int i = 0; i < ibanPermute.length(); i++) {
	        final char c = ibanPermute.charAt(i);
	        if (Character.isDigit(c)) {
            	ibanNumeriqueStr.append(c);
            }
	        else {
            	ibanNumeriqueStr.append(10 + getAlphabetPosition(c));
            }
        }

        // on teste si le modulo 97 est égal à 1
        final BigInteger ibanNumerique = new BigInteger(ibanNumeriqueStr.toString());
        if (ibanNumerique.mod(MODULO).intValue() != 1) {
			throw new IbanNonPlausibleException();
        }
	}

	private static boolean isSuisse(String iban) {
		final String codePays = extractCodePays(iban);
		return "CH".equals(codePays);
	}


	private static String extractCodePays(String iban) {
		return iban != null && iban.length() > 1 ? iban.substring(0, 2) : null;
	}

	private static String extractClearing(String iban) {
		if (iban != null && iban.length() > 8) {
			return iban.substring(4,9);
		}
		else return null;
	}

	/**
	 * Renvoie la position de la lettre dans l'alphabet.
	 * @param letter la lettre
	 * @return la position
	 */
	private static int getAlphabetPosition(char letter) {
		return Character.toUpperCase(letter) - 'A';
    }

	/**
	 * @param iban
	 *            le numéro de compte IBAN (assumé valide).
	 * @return le clearing de la banque s'il s'agit d'un compte IBAN suisse, ou <b>null</b> s'il s'agit d'un compte étranger.
	 * @throws IbanValidationException
	 *             si l'IBAN n'est pas valide.
	 */
	public String getClearing(String iban) {

		iban = FormatNumeroHelper.removeSpaceAndDash(iban);
        if (isSuisse(iban)) {
			return extractClearing(iban);
		}
		else {
			return null;
		}
	}

}
