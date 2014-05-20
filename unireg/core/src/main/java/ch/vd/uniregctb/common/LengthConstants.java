package ch.vd.uniregctb.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public abstract class LengthConstants {

	public final static int HIBERNATE_LOGUSER = 65;

	public final static int TIERS_PERSONNE = 200;
	public final static int TIERS_NOM = 250;
	public final static int TIERS_REMARQUE = 2000;
	public final static int TIERS_ADRESSEBICSWIFT = 15;
	public final static int TIERS_SEXE = 8;
	public final static int TIERS_CATETRANGER = 50;
	public final static int TIERS_NUMCOMPTE = 34;
	public final static int TIERS_NUMAVS = 13;
	public final static int TIERS_NUMTEL = 35;
	public final static int TIERS_EMAIL = 255;
	public static final int TIERS_LIB_ORIGINE = 250;
	public static final int TIERS_NOM_PRENOMS_PARENT = 100;
	public static final int TIERS_TOUS_PRENOMS = 100;

	public final static int IDENTPERSONNE_IDENTIFIANT = 13;
	public final static int IDENTPERSONNE_CATEGORIE = 10;
	public final static int IDENT_DEMANDE_TYPE = 30;

	public final static int IDENT_ENTREPRISE_IDE = 12;

	public final static int FOR_RATTACHEMENT = 26;
	public final static int FOR_MOTIF = 49;
	public final static int FOR_IMPOSITION = 11;
	public final static int FOR_GENRE = 29;
	public final static int FOR_AUTORITEFISCALE = 22;

	public final static int TACHE_ETAT = 11;
	public final static int TACHE_TYPE = 15;

	public final static int SITUATIONFAMILLE_TARIF = 11;
	public final static int SITUATIONFAMILLE_ETATCIVIL = 34;


	public final static int DPI_NOM1 = 250;
	public final static int DPI_NOM2 = 250;
	public final static int DPI_CATEGORIEIS = 32;
	public final static int DPI_PERIODICITE = 11;
	public final static int DPI_PERIODE_DECOMPTE = 3;
	public final static int DPI_MODECOM = 12;

	public final static int AC_FORME = 5;

	public final static int MVTDOSSIER_ETAT = 15;

	public final static int EVTFISCAL_TYPE = 29;

	public final static int EVTCIVILREG_ETAT = 10;
	public final static int EVTCIVILREG_TYPE = 45;
	public final static int EVTCIVILREG_COMMENT = 255;

	public final static int EVTCIVILECH_ETAT = 10;
	public final static int EVTCIVILECH_TYPE = 40;
	public final static int EVTCIVILECH_ACTION = 20;
	public final static int EVTCIVILECH_COMMENT = 255;

	public final static int EVTCIVILERREUR_TYPE = 7;
	public final static int EVTCIVILERREUR_MESSAGE = 1024;

	public final static int EVTEXTERNE_ETAT = 10;
	public final static int EVTEXTERNE_QLR_TYPE = 13;

	public final static int ADRESSE_TYPETIERS = 14;
	public final static int ADRESSE_TYPECIVIL = 10;
	public final static int ADRESSE_TYPEPM = 11;
	public final static int ADRESSE_TYPESUPPLEM = 15;
	public final static int ADRESSE_NUM = 35;
	public final static int ADRESSE_NOM = 100;

	public final static int LOG_LEVEL = 7;
	public final static int LOG_MESSAGE = 255;

	public final static int DI_TYPE = 17;
	public final static int DI_ETAT = 12;
	public final static int DI_QUALIF = 16;
	public final static int DI_ADRESSE_RETOUR = 4;
	public static final int DI_CODE_CONTROLE = 6;

	public final static int MODELEDOC_TYPE = 32;

	public final static int DOCINDEX_NOM = 100;
	public final static int DOCINDEX_DESC = 255;

	public static final int NUMERO_IMMEUBLE = 20;
	public static final int NATURE_IMMEUBLE = 255;
	public static final int GENRE_PROPRIETE = 12;
	public static final int DERNIERE_MUTATION = 28;
	public static final int TYPE_IMMEUBLE = 27;
	public static final int NOM_COMMUNE = 255;
	public static final int REF_ESTIM_FISCALE = 255;
	public static final int LIEN_RF = 500;

	public final static int MAXLEN = 2000;

	private static final Pattern MULTI_BLANKS = Pattern.compile("\\s{2,}");

	public static String streamlineField(String fieldValue, int maxLength, boolean removeSuccessiveBlanks) {

		// on enlève d'abord tous les blancs en surplus : trim + blanc successifs si demandé
		String value = StringUtils.trimToNull(fieldValue);
		if (value != null) {

			// on retire maintenant les blancs successifs
			if (removeSuccessiveBlanks) {
				final Matcher matcher = MULTI_BLANKS.matcher(value);
				value = matcher.replaceAll(" ");
			}

			// si la chaîne est trop longue, on tronque et puis c'est tout!
			if (value.length() > maxLength) {
				value = StringUtils.trimToNull(value.substring(0, maxLength));
			}
		}
		return value;
	}
}
