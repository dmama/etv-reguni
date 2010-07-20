package ch.vd.uniregctb.iban;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Properties;

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

	/**
	 * Liste des longueurs IBAN par pays.
	 */
	private static Properties longueursValides = null;

	/**
	 * Dao pour la vérification du numéro de clearing bancaire
	 */
	private ClearingDao clearingDao = null;

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
		validateUpperCase(iban);
		validateLength(iban);
		validateFormat(iban);
		validatePlausability(iban);
		validateClearing(iban);
	}

	private void validateUpperCase(String iban) throws IbanUpperCaseException {
		if(!iban.toUpperCase().equals(iban)){
			throw new IbanUpperCaseException();
		}
	}

	public boolean isValidIban(String iban){
		try{
			validate(iban);
		}
		catch(IbanValidationException e){
			return false;
		}
		return true;
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
	private void validateLength(String iban) throws IbanBadLengthException, IbanUnknownCountryException {
		// longueur < 2
		if ( iban.length() < 2)
			throw new IbanBadLengthException();

		// recherche de la longueur attendue
		String codePays = extractCodePays(iban);
		int longueurAttendue = Integer.parseInt( longueursValides.getProperty(codePays, "-1") );

		// code pays non trouvé
		if (longueurAttendue == -1)
			throw new IbanUnknownCountryException();

		// longueur incorrecte
		if ( iban.length() != longueurAttendue )
			throw new IbanBadLengthException();
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
	private void validateFormat(String iban) throws IbanBadFormatException {

		// Tous les caractères doivent être alphanumériques
		String ibanMinuscule = iban.toLowerCase();
		for (int i=0; i<ibanMinuscule.length(); i++) {
			if ( !Character.isLetterOrDigit( ibanMinuscule.charAt(i) ) )
				throw new IbanBadFormatException("les caractères doivent être alpha numériques");
		}

		// cas spécial de la Suisse : les positions 3 à 9 doivent être numériques
		for (int i=2; i<9; i++) {
			if ( !Character.isDigit( ibanMinuscule.charAt(i) ) )
				throw new IbanBadFormatException("pour la Suisse, les caractères de la 3ème à la 9ème position doivent être numériques");
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
	private void validatePlausability(String iban) throws IbanNonPlausibleException {
		// on déplace les 4 premiers caractères à la fin de la chaine
        String ibanPermute = iban.substring(4) + iban.substring(0, 4);

        // on convertit le code IBAN en valeur numérique
        StringBuffer ibanNumeriqueStr = new StringBuffer();
        for (int i = 0; i < ibanPermute.length(); i++) {
            if (Character.isDigit(ibanPermute.charAt(i))) {
            	ibanNumeriqueStr.append(ibanPermute.charAt(i));
            } else {
            	ibanNumeriqueStr.append(10 + getAlphabetPosition(ibanPermute.charAt(i)));
            }
        }

        // on teste si le modulo 97 est égal à 1
        BigInteger ibanNumerique = new BigInteger(ibanNumeriqueStr.toString());
        if ( ibanNumerique.mod(new BigInteger("97")).intValue() != 1)
			throw new IbanNonPlausibleException();
	}

	/**
	 * Valide le numéro de clearing bancaire de l'IBAN.
	 *
	 * @param iban
	 *            le code IBAN à tester
	 * @throws IbanBadClearingNumberException
	 *             si le numéro de clearing bancaire ne correspond à aucun établissement.
	 */
	private void validateClearing(String iban) throws IbanBadClearingNumberException {
		String codePays = extractCodePays(iban);

		// on ne fait le contrôle que pour la suisse
		if ("CH".equals(codePays)) {
			if ( ! clearingDao.isNumeroClearingValid( extractClearing(iban) ) )
				throw new IbanBadClearingNumberException();
		}
	}

	private String extractCodePays(String iban) {
		return iban != null && iban.length() > 1 ? iban.substring(0, 2) : null;
	}

	private String extractClearing(String iban) {
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
	private int getAlphabetPosition(char letter) {
        return Character.valueOf(Character.toUpperCase(letter)).compareTo(Character.valueOf('A'));
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

		final String codePays = extractCodePays(iban);
		if ("CH".equals(codePays)) {
			String c = extractClearing(iban);
			return c;
		}
		else {
			return null;
		}
	}


	/**
	 * @param clearingDao the clearingDao to set
	 */
	public void setClearingDao(ClearingDao clearingDao) {
		this.clearingDao = clearingDao;
	}

}
