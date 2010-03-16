package ch.vd.uniregctb.parametrage;

import java.beans.PropertyDescriptor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanUtils;

/**
 * Enumeration des differents paramètres de l'application
 *
 * @author xsifnr
 *
 */
public enum ParametreEnum {

	premierePeriodeFiscale("2003", Type.annee, false),

	noel("25.12", Type.jourDansAnnee, false),
	nouvelAn("01.01", Type.jourDansAnnee, false),
	lendemainNouvelAn("02.01", Type.jourDansAnnee, false),
	feteNationale("01.08", Type.jourDansAnnee, false),

	nbMaxParListe("100", Type.entierPositif, true),
	nbMaxParPage("10", Type.entierPositif, true),

	delaiAttenteDeclarationImpotPersonneDecedee("30", Type.delaisEnJour, true),
	delaiRetourDeclarationImpotEmiseManuellement("60", Type.delaisEnJour, true),
	delaiCadevImpressionDeclarationImpot("3", Type.delaisEnJour, true),
	delaiEnvoiSommationDeclarationImpot("15", Type.delaisEnJour, true),
	delaiEcheanceSommationDeclarationImpot("30", Type.delaisEnJour, true),

	jourDuMoisEnvoiListesRecapitulatives("20", Type.jourDansMois, true),
 	delaiCadevImpressionListesRecapitulatives("3", Type.delaisEnJour, true),
	delaiRetourListeRecapitulative("30", Type.delaisEnJour, true),
	delaiEnvoiSommationListeRecapitulative("15", Type.delaisEnJour, true),
	delaiRetourSommationListeRecapitulative("10", Type.delaisEnJour, true),
	delaiEcheanceSommationListeRecapitualtive("15", Type.delaisEnJour, true),

	delaiRetentionRapportTravailInactif ("24", Type.delaisEnMois, true);

	/**
	 * Les differents type de paramétres possibles
	 *
	 * @author xsifnr
	 *
	 */
	public enum Type {
		entierPositif, annee, jourDansAnnee, jourDansMois, delaisEnJour, delaisEnMois
	}

	private Type type;
	private String defaut;
	private boolean resetable;

	private ParametreEnum(String d, Type t, boolean r) {
		defaut = d;
		type = t;
		resetable = r;
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
	 * @param s
	 * @throws ValeurInvalideException
	 *             si la valeur n'est pas valable
	 */
	public void validerValeur(String s) throws ValeurInvalideException {

		String msgErr = MessageFormat.format(" ''{0}'' n''est pas une valeur valide pour le paramètre ''{1}'' de type ''{2}''", s,
				toString(), type.toString());

		try {
			switch (type) {
			case entierPositif:
			case delaisEnJour:
			case delaisEnMois:
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
	 * @throw {@link IllegalArgumentException} si la valeur a formatée n'est pas valide.
	 */
	public String formaterValeur(String valeur) {
		try {
			validerValeur(valeur);
		}
		catch (ValeurInvalideException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		switch (type) {
			case jourDansAnnee:
				String[] jourMois = valeur.split("\\.");
				Integer jour = Integer.parseInt(jourMois[0]);
				Integer mois = Integer.parseInt(jourMois[1]);
				valeur = String.format("%02d.%02d", jour, mois);
				break;
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
	 * @throws IncorrectDayMonthException
	 *
	 * @throw {@link IncorrectDayMonthException} si la string passée en paramètre n'est pas au format requis ou si elle represente une date
	 *        incohérente ou si elle est renseignée au 29 février.
	 */
	private static Integer[] stringToDayMonth(String string) throws IncorrectDayMonthException {
		string = string.trim();
		final String regexp = "^\\d?\\d\\.\\d?\\d$";
		if (string.matches(regexp)) {
			String[] arr = string.split("\\.");
			Integer jour = Integer.valueOf(arr[0]);
			Integer mois = Integer.valueOf(arr[1]);
			// Verification sur la plausibilité de la date
			if (jour < 1 || mois < 1 || jour > 30 && Arrays.asList(2, 4, 6, 9, 11).contains(mois) || jour > 31 || mois > 12) {
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
			throw new IncorrectDayMonthException("'" + string + "' ne matche avec la expression régulière suivant : " + regexp);
		}
	}

	/**
	 * Classe privée. Utilisée pour signaler une valeur Jour dans l'année incorrecte.
	 *
	 * @author xsifnr
	 *
	 */
	@SuppressWarnings("serial")
	private static class IncorrectDayMonthException extends Exception {
		public IncorrectDayMonthException(String string) {
			super(string);
		}
	}

	/**
	 * Exception signalant que la valeur assigné à un paramètre est invalide
	 *
	 * @author xsifnr
	 *
	 */
	@SuppressWarnings("serial")
	public static class ValeurInvalideException extends Exception {
		public ValeurInvalideException(String string) {
			super(string);
		}

		public ValeurInvalideException(String string, Throwable t) {
			super(string, t);
		}
	}

	/**
	 * Copie les propriétés dont le nom est défini dans l'enum
	 *
	 * @param beanSrc
	 *            Le bean source
	 * @param beanDest
	 *            Le bean destination
	 */
	public static void copyProperties(Object beanSrc, Object beanDest) {

		for (ParametreEnum p : ParametreEnum.values()) {
			PropertyDescriptor pdSrc = BeanUtils.getPropertyDescriptor(beanSrc.getClass(), p.name());
			PropertyDescriptor pdDest = BeanUtils.getPropertyDescriptor(beanDest.getClass(), p.name());
			try {
				if (pdDest.getPropertyType().equals(pdSrc.getPropertyType())) {
					pdDest.getWriteMethod().invoke(beanDest, pdSrc.getReadMethod().invoke(beanSrc));
				} else if (pdDest.getPropertyType().equals(Integer.class) && pdSrc.getPropertyType().equals(String.class)) {
					pdDest.getWriteMethod().invoke(beanDest, p.convertirStringVersValeurTypee((String)pdSrc.getReadMethod().invoke(beanSrc)));
				} else if (pdDest.getPropertyType().equals(Integer[].class) && pdSrc.getPropertyType().equals(String.class)) {
					pdDest.getWriteMethod().invoke(beanDest, p.convertirStringVersValeurTypee((String)pdSrc.getReadMethod().invoke(beanSrc)));
				} else if (pdDest.getPropertyType().equals(String.class) && pdSrc.getPropertyType().equals(Integer.class)) {
					pdDest.getWriteMethod().invoke(beanDest, p.convertirValeurTypeeVersString(pdSrc.getReadMethod().invoke(beanSrc)));
				} else if (pdDest.getPropertyType().equals(String.class) && pdSrc.getPropertyType().equals(Integer[].class)) {
					pdDest.getWriteMethod().invoke(beanDest, p.convertirValeurTypeeVersString(pdSrc.getReadMethod().invoke(beanSrc)));
				} else {
					throw new IllegalArgumentException(
							"Impossible de copier une proriété du type "
							+ pdSrc.getPropertyType().getName()
							+ " vers du "
							+ pdDest.getPropertyType().getName());
				}
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Erreur lors de la copie des propriétés relatives a ParametreEnum. "
						+ "Vérifiez que le bean source et le bean destination déclarent toutes les propriétés présentes dans l'enum.", e);
			}
		}
	}

	/**
	 * Verifie si la classe passée en argument définie une propriété JavaBeans
	 * pour chacune des valeurs possibles de {@link ParametreEnum}.
	 *
	 * @param clazz
	 * @return Liste des noms des propriétés manquantes.
	 *
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getMissingProperties (Class clazz) {
		// Verifie l'integrité de la classe.
		// Cette classe doit avoir une propriété JavaBean pour toute les ParametreEnum défini.
		List<String> missingProp = new ArrayList<String>();
		for(ParametreEnum p : ParametreEnum.values()) {
			PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(clazz, p.name());
			if (pd == null) {
				missingProp.add(p.name());
			}
		}
		return missingProp;
	}

	/**
	 * Détermine si la {@link Class} passée en argument définie une propriété JavaBean
	 * pour chaque valeur possible de {@link ParametreEnum}.
	 *
	 * @param clazz la classe à tester
	 *
	 * @return true true si la classe définie toutes les propriétés
	 */
	@SuppressWarnings("unchecked")
	public static boolean isClassCompatible (Class clazz) {
		return getMissingProperties(clazz).size() == 0;
	}

	/**
	 * Construit une {@link String} message repertoriant les propriétés manquantes de
	 * la classe passée en paramètre
	 *
	 * @param clazz la classe à tester
	 *
	 * @return le message
	 */
	@SuppressWarnings("unchecked")
	public static String getMissingPropertiesMessage (Class clazz) {
		return getMissingPropertiesMessage(clazz.getName(), getMissingProperties(clazz));
	}

	/**
	 * Construit une {@link String} message repertoriant les propriétés manquantes
	 * (renseignées dans la liste passée en argument)
	 *
	 * @param className nom de la classe
	 * @param missingProperties une liste de {@link String} contenant le nom des propriétés manquantes
	 *
	 * @return le message
	 */
	public static String getMissingPropertiesMessage (String className, List<String> missingProperties) {
		StringBuilder sb = null;
		if (missingProperties.size() > 0 ) {
			for (String s: missingProperties) {
				if (sb == null) {
					sb = new StringBuilder("Les propriétés suivantes sont manquantes pour la classe " + className + ": [" + s);
				} else {
					sb.append(", " + s);
				}
			}
			sb.append("]");
			return sb.toString();
		} else {
			return "";
		}
	}

	/**
	 * tests rapides ...
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		copyProperties(new Object(), new Object());
	}

	/**
	 * Convertit la valeur d'un type de parametre de {@link String} vers sa valeur typée.
	 *
	 * <ul>
	 * 	<li>Tableau d'{@link Integer} pour les jours de l'année
	 * 	<li>{@link Integer} pour les autres
	 * </ul>
	 *
	 * @param valeur
	 * @return
	 */
	public Object convertirStringVersValeurTypee (String valeur) {
		if (Type.jourDansAnnee.equals(type)) {
			return new Integer[] {
					Integer.parseInt(valeur.split("\\.")[0]),
					Integer.parseInt(valeur.split("\\.")[1])
			};
		} else {
			return new Integer(Integer.parseInt(valeur));
		}
	}

	/**
	 *
	 * Convertit la valeur typée d'un paramètre en {@link String}
	 *
	 * @param valeur
	 * @return
	 */
	public String convertirValeurTypeeVersString (Object valeur) {
		if (Type.jourDansAnnee.equals(type)) {
			Integer[] val = (Integer[]) valeur;
			return String.format("%02d.%02d", val[0], val[1]);
		} else {
			return valeur.toString();
		}
	}


}