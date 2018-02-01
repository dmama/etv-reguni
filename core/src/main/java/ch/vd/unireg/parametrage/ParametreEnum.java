package ch.vd.unireg.parametrage;

import java.text.ParseException;
import java.util.Arrays;

import ch.vd.registre.base.date.RegDateHelper;

/**
 * Enumeration des differents paramètres de l'application
 *
 */
public enum ParametreEnum {

	premierePeriodeFiscalePersonnesPhysiques("2003", Type.annee, false),
	premierePeriodeFiscalePersonnesMorales("2009", Type.annee, false),
	premierePeriodeFiscaleDeclarationPersonnesMorales("2016", Type.annee, false),

	noel("25.12", Type.jourDansAnnee, false),
	nouvelAn("01.01", Type.jourDansAnnee, false),
	lendemainNouvelAn("02.01", Type.jourDansAnnee, false),
	feteNationale("01.08", Type.jourDansAnnee, false),

	nbMaxParListe("100", Type.entierPositif, true),
	nbMaxParPage("10", Type.entierPositif, true),

	delaiAttenteDeclarationImpotPersonneDecedee("30", Type.delaisEnJour, true),
	delaiRetourDeclarationImpotPPEmiseManuellement("60", Type.delaisEnJour, true),
	delaiCadevImpressionDeclarationImpot("3", Type.delaisEnJour, true),
	delaiEnvoiSommationDeclarationImpotPP("15", Type.delaisEnJour, true),
	delaiEcheanceSommationDeclarationImpotPP("30", Type.delaisEnJour, true),

	delaiRetourDeclarationImpotPMEmiseManuellement("30", Type.delaisEnJour, true),
	delaiMinimalRetourDeclarationImpotPM("3", Type.delaisEnMois, true),
	delaiEnvoiSommationDeclarationImpotPM("0", Type.delaisEnJour, true),
	delaiEcheanceSommationDeclarationImpotPM("30", Type.delaisEnJour, true),
	dateLimiteEnvoiMasseDeclarationsUtilitePublique("31.01", Type.jourDansAnnee, true),

	delaiRetourQuestionnaireSNCEmisManuellement("90", Type.delaisEnJour, true),
	delaiEnvoiRappelQuestionnaireSNC("15", Type.delaisEnJour, true),
	delaiCadevImpressionQuestionnaireSNC("3", Type.delaisEnJour, true),

	dateDebutEnvoiLettresBienvenue("11.06.2016", Type.date, true),
	delaiRetourLettreBienvenue("30", Type.delaisEnJour, true),
	delaiCadevImpressionLettreBienvenue("3", Type.delaisEnJour, true),
	tailleTrouAssujettissementPourNouvelleLettreBienvenue("720", Type.delaisEnJour, true),
	delaiEnvoiRappelLettreBienvenue("15", Type.delaisEnJour, true),

	dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI("09.12.2016", Type.date, true),
	delaiRetourDemandeDegrevementICI("30", Type.delaisEnJour, true),
	delaiEnvoiRappelDemandeDegrevementICI("15", Type.delaisEnJour, true),
	delaiCadevImpressionDemandeDegrevementICI("3", Type.delaisEnJour, true),

	jourDuMoisEnvoiListesRecapitulatives("20", Type.jourDansMois, true),
 	delaiCadevImpressionListesRecapitulatives("3", Type.delaisEnJour, true),
	delaiRetourListeRecapitulative("30", Type.delaisEnJour, true),
	delaiEnvoiSommationListeRecapitulative("15", Type.delaisEnJour, true),
	delaiRetourSommationListeRecapitulative("10", Type.delaisEnJour, true),
	delaiEcheanceSommationListeRecapitualtive("15", Type.delaisEnJour, true),

	delaiRetentionRapportTravailInactif ("24", Type.delaisEnMois, true),

	anneeMinimaleForDebiteur("2009", Type.annee, true), // [UNIREG-2507]
	dateExclusionDecedeEnvoiDI("15.11",Type.jourDansAnnee,true),//[UNIREG-1952]

	ageRentierHomme("65", Type.entierPositif, true),
	ageRentierFemme("64", Type.entierPositif, true);

	/**
	 * Les differents type de paramètres possibles
	 */
	public enum Type {
		entierPositif, annee, jourDansAnnee, jourDansMois, delaisEnJour, delaisEnMois, date
	}

	private final Type type;
	private final String defaut;
	private final boolean resetable;

	ParametreEnum(String d, Type t, boolean resetable) {
		defaut = d;
		type = t;
		this.resetable = resetable;
	}

	/**
	 * @return la valeur par défaut du parametre
	 */
	public String getDefaut() {
		return defaut;
	}

	/**
	 * @return le type du parametre défini par {@link Type}
	 */
	public Type getType() {
		return type;
	}
	
	/**
	 * @return true si le parametre peut être remis à zéro par une action utilisateur
	 */
	public boolean isResetable() {
		return resetable;
	}

	/**
	 * Permet de verifier la validité des valeurs des paramètres en fonction de leur type.
	 *
	 * @param s valeur à valider
	 * @throws ValeurInvalideException
	 *             si la valeur n'est pas valable
	 */
	public void validerValeur(String s) throws ValeurInvalideException {

		String msgErr = String.format(" '%s' n'est pas une valeur valide pour le paramètre '%s' de type '%s'", s,
				toString(), type.toString());

		try {
			switch (type) {
			case delaisEnJour:
			case delaisEnMois:
				if (Integer.parseInt(s) < 0) {
					throw new ValeurInvalideException(msgErr + " - La valeur doit être un entier positif ou nul");
				}
				break;
			case entierPositif:
				if (Integer.parseInt(s) <= 0) {
					throw new ValeurInvalideException(msgErr + " - La valeur doit être un entier positif");
				}
				break;
			case annee:
				if (Integer.parseInt(s) < 1900 || Integer.parseInt(s) > 9999) {
					throw new ValeurInvalideException(msgErr + " - La valeur doit etre un entier compris entre 1900 et 9999");
				}
				break;
			case jourDansAnnee:
				try {
					stringToDayMonth(s);
				}
				catch (IncorrectDayMonthException e) {
					throw new ValeurInvalideException(msgErr + " - " + e.getMessage(), e);
				}
				break;
			case jourDansMois:
				if (Integer.parseInt(s) < 1 || Integer.parseInt(s) > 28) {
					throw new ValeurInvalideException(msgErr + " - La valeur doit etre un entier compris entre 1 et 28");
				}
				break;
			case date:
				try {
					RegDateHelper.displayStringToRegDate(s, false);
				}
				catch (ParseException e) {
					throw new ValeurInvalideException(msgErr + " - " + e.getMessage(), e);
				}
				break;
			default:
				throw new RuntimeException("Code théoriquement inatteignable...");
			}
		}
		catch (NumberFormatException e) {
			throw new ValeurInvalideException(msgErr + " - " + e.getMessage(), e);
		}
	}

	/**
	 * Formate une valeur valide.
	 *
	 * La méthode {@link #validerValeur(String)} est appelée pour verifier la validité de la valeur avant d'effectuer le formatage de
	 * celle-ci.
	 *
	 * @param valeur
	 *            La valeur a formatter
	 *
	 * @return la valeur formatée
	 *
	 * @throws IllegalArgumentException si la valeur a formatée n'est pas valide.
	 *
	 */
	public String formaterValeur(String valeur) {
		try {
			validerValeur(valeur);
		}
		catch (ValeurInvalideException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		switch (type) {
			case jourDansAnnee: {
				final String[] jourMois = valeur.split("\\.");
				final int jour = Integer.parseInt(jourMois[0]);
				final int mois = Integer.parseInt(jourMois[1]);
				valeur = String.format("%02d.%02d", jour, mois);
				break;
			}
			case date: {
				final String[] jourMoisAnnee = valeur.split("\\.");
				final int jour = Integer.parseInt(jourMoisAnnee[0]);
				final int mois = Integer.parseInt(jourMoisAnnee[1]);
				final int annee = Integer.parseInt(jourMoisAnnee[2]);
				valeur = String.format("%02d.%02d.%04d", jour, mois, annee);
				break;
			}
			default:
				break;
		}
		return valeur;
	}

	/**
	 * Converti une {@link String} representant un jour de l'année en un tableau de 2 {@link Integer}.<br>
	 *
	 * @param string
	 *            un jour de l'année sous la forme : jour.mois (par ex. : 01.01; 25.12; 1.8; 2.01)
	 *
	 * @return un tableau de 2 Integer.<br>
	 *         <ul>
	 *         <li>A l'index 0 le jour du mois
	 *         <li>A l'index 1 le mois
	 *         </ul>
	 * @throws IncorrectDayMonthException si la string passée en paramètre n'est pas au format requis ou si elle represente une date
	 *        incohérente ou si elle est renseignée au 29 février.

	 *
	 */
	private static Integer[] stringToDayMonth(String string) throws IncorrectDayMonthException {
		string = string.trim();
		final String regexp = "^\\d?\\d\\.\\d?\\d$";
		if (string.matches(regexp)) {
			String[] arr = string.split("\\.");
			Integer jour = Integer.valueOf(arr[0]);
			Integer mois = Integer.valueOf(arr[1]);
			// Verification sur la plausibilité de la date
			if (jour < 1 || mois < 1 || jour > 29 && mois == 2 || jour > 30 && Arrays.asList(4, 6, 9, 11).contains(mois) || jour > 31 || mois > 12) {
				throw new IncorrectDayMonthException(string + " n'est pas une date cohérente");
			}
			if (jour == 29 && mois == 2) {
				throw new IncorrectDayMonthException("Impossible de spécifier une date au 29 février");
			}
			return new Integer[] {
					jour, mois
			};
		}
		else {
			throw new IncorrectDayMonthException('\'' + string + "' ne matche avec l'expression régulière suivante : " + regexp);
		}
	}

	/**
	 * Classe privée. Utilisée pour signaler une valeur Jour dans l'année incorrecte.
	 *
	 */
	private static class IncorrectDayMonthException extends Exception {
		public IncorrectDayMonthException(String string) {
			super(string);
		}
	}

	/**
	 * Exception signalant que la valeur assigné à un paramètre est invalide
	 *
	 */
	public static class ValeurInvalideException extends Exception {
		public ValeurInvalideException(String string) {
			super(string);
		}

		public ValeurInvalideException(String string, Throwable t) {
			super(string, t);
		}
	}

	/**
	 * Convertit la valeur d'un type de parametre de {@link String} vers sa valeur typée.
	 *
	 * <ul>
	 * 	<li>Tableau d'{@link Integer} pour les jours de l'année
	 * 	<li>{@link Integer} pour les autres
	 * </ul>
	 *
	 */
	public Object convertirStringVersValeurTypee (String valeur) {
		if (Type.jourDansAnnee == type) {
			final String[] split = valeur.split("\\.");
			return new Integer[] {
					Integer.parseInt(split[0]),
					Integer.parseInt(split[1])
			};
		}
		else if (Type.date == type) {
			final String[] split = valeur.split("\\.");
			return new Integer[] {
					Integer.parseInt(split[0]),
					Integer.parseInt(split[1]),
					Integer.parseInt(split[2])
			};
		}
		else {
			return Integer.parseInt(valeur);
		}
	}

	/**
	 *
	 * Convertit la valeur typée d'un paramètre en {@link String}
	 *
	 */
	public String convertirValeurTypeeVersString (Object valeur) {
		if (Type.jourDansAnnee == type) {
			final Integer[] val = (Integer[]) valeur;
			return String.format("%02d.%02d", val[0], val[1]);
		}
		else if (Type.date == type) {
			final Integer[] val = (Integer[]) valeur;
			return String.format("%02d.%02d.%04d", val[0], val[1], val[2]);
		}
		else {
			return valeur.toString();
		}
	}
}