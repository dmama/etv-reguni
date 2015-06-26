package ch.vd.uniregctb.migration.pm.extractor;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.regpm.CoordonneesFinancieresContainer;
import ch.vd.uniregctb.migration.pm.regpm.RegpmInstitutionFinanciere;

public abstract class IbanExtractor {

	private static final BigInteger MODULO_IBAN = BigInteger.valueOf(97);

	private static final Pattern CCP = Pattern.compile("(\\d{1,5})-(\\d{1,6})-(\\d)");
	private static final Pattern CLEARING = Pattern.compile("(\\d{1,5})");
	private static final int CLEARING_POSTE = 9000;

	public static class IbanExtratorException extends Exception {

		public IbanExtratorException(String message, Throwable cause) {
			super(message, cause);
		}

		public IbanExtratorException(String message) {
			super(message);
		}
	}

	/**
	 * @param coordonneesFinancieres des coordonnées financières extraites de la base du mainframe
	 * @param mr le collecteur de messages de suivi
	 * @return un IBAN (<code>null</code> si les coordonnées financières passées sont <code>null</code>)
	 * @throws IllegalArgumentException en cas d'incohérence, ou de problème en général
	 */
	@Nullable
	public static String extractIban(@Nullable CoordonneesFinancieresContainer coordonneesFinancieres, MigrationResultProduction mr) throws IbanExtratorException {
		if (coordonneesFinancieres == null) {
			return null;
		}

		final String iban;

		// si l'IBAN est déjà là, on le renvoie tout simplement
		if (StringUtils.isNotBlank(coordonneesFinancieres.getIban())) {
			iban = coordonneesFinancieres.getIban();
			mr.addMessage(LogCategory.COORDONNEES_FINANCIERES, LogLevel.INFO, String.format("IBAN déjà présent dans les données source : %s.", iban));
		}

		// si on a un CCP, on le convertit
		else if (StringUtils.isNotBlank(coordonneesFinancieres.getNoCCP())) {
			iban = extractIbanFromCCP(coordonneesFinancieres.getNoCCP());
			mr.addMessage(LogCategory.COORDONNEES_FINANCIERES, LogLevel.INFO, String.format("IBAN extrait du numéro CCP %s : %s.", coordonneesFinancieres.getNoCCP(), iban));
		}

		// si on a un compte bancaire, on le convertit
		else if (StringUtils.isNotBlank(coordonneesFinancieres.getNoCompteBancaire())) {
			iban = extractIbanFromCompteBancaire(coordonneesFinancieres.getNoCompteBancaire(), coordonneesFinancieres.getInstitutionFinanciere());
			mr.addMessage(LogCategory.COORDONNEES_FINANCIERES, LogLevel.INFO, String.format("IBAN extrait du numéro de compte '%s' et du clearing '%s' : %s.",
			                                                                                coordonneesFinancieres.getNoCompteBancaire(),
			                                                                                coordonneesFinancieres.getInstitutionFinanciere().getNoClearing(),
			                                                                                iban));
		}

		// je ne vois pas comment...
		else {
			iban = null;
		}

		return iban;
	}

	private static String buildIban(int clearing, String noCompte) throws IbanExtratorException {
		final String sansControle = String.format("%05d%12sCH00", clearing, noCompte).replace(' ', '0');     // ccccnnnnnnnnnnnnCH00

		// on convertit le code IBAN en valeur numérique
		final StringBuilder ibanNumeriqueStr = new StringBuilder();
		for (int i = 0; i < sansControle.length(); i++) {
			final char c = sansControle.charAt(i);
			if (Character.isDigit(c)) {
				ibanNumeriqueStr.append(c);
			}
			else {
				ibanNumeriqueStr.append(10 + Character.toUpperCase(c) - 'A');
			}
		}

		// calcul du modulo de contrôle
		final BigInteger ibanNumerique = new BigInteger(ibanNumeriqueStr.toString());
		final BigInteger ctrl = MODULO_IBAN.add(BigInteger.ONE).subtract(ibanNumerique.mod(MODULO_IBAN));
		return String.format("CH%2d%05d%12s", ctrl, clearing, noCompte).replace(' ', '0');

	}

	private static String extractIbanFromCompteBancaire(String noCompteBancaire, RegpmInstitutionFinanciere institutionFinanciere) throws IbanExtratorException {
		if (institutionFinanciere == null || StringUtils.isBlank(institutionFinanciere.getNoClearing())) {
			throw new IbanExtratorException("Clearing inconnu pour le numéro de compte '" + noCompteBancaire + "'");
		}

		final Matcher clearingMatcher = CLEARING.matcher(institutionFinanciere.getNoClearing());
		if (!clearingMatcher.matches()) {
			throw new IbanExtratorException("Numéro de clearing bancaire invalide : '" + institutionFinanciere.getNoClearing() + "'");
		}
		final int clearing = Integer.parseInt(institutionFinanciere.getNoClearing());
		final String noCompteCanonique = noCompteBancaire.replaceAll("[^a-zA-Z0-9]+", StringUtils.EMPTY);
		return buildIban(clearing, noCompteCanonique);
	}

	private static String extractIbanFromCCP(String ccp) throws IbanExtratorException {
		final Matcher matcher = CCP.matcher(ccp);
		if (!matcher.matches()) {
			throw new IbanExtratorException("CCP invalide : '" + ccp + "'");
		}

		final String formeCanoniqueCCP = String.format("%05d%06d%d", Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
		return buildIban(CLEARING_POSTE, formeCanoniqueCCP);
	}
}
