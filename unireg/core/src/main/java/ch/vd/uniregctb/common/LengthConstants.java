package ch.vd.uniregctb.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public abstract class LengthConstants {

	public static final int HIBERNATE_LOGUSER = 65;

	public static final int TIERS_PERSONNE = 200;
	public static final int TIERS_NOM = 250;
	public static final int TIERS_REMARQUE = 2000;
	public static final int TIERS_ADRESSEBICSWIFT = 15;
	public static final int TIERS_SEXE = 8;
	public static final int TIERS_CATETRANGER = 50;
	public static final int TIERS_NUMCOMPTE = 34;
	public static final int TIERS_NUMAVS = 13;
	public static final int TIERS_NUMTEL = 35;
	public static final int TIERS_EMAIL = 255;
	public static final int TIERS_LIB_ORIGINE = 250;
	public static final int TIERS_LIBELLE_ORIGINE = 100;
	public static final int TIERS_CANTON_ORIGINE = 2;
	public static final int TIERS_NOM_PRENOMS_PARENT = 100;
	public static final int TIERS_TOUS_PRENOMS = 100;
	public static final int TIERS_SECTEUR_ACTIVITE = 1024;
	public static final int IDENTPERSONNE_IDENTIFIANT = 13;
	public static final int IDENTPERSONNE_CATEGORIE = 10;
	public static final int IDENT_DEMANDE_TYPE = 30;
	public static final int IDENT_TYPE_CTB = 30;

	public static final int IDENT_ENTREPRISE_IDE = 12;

	public static final int FOR_RATTACHEMENT = 26;
	public static final int FOR_MOTIF = 49;
	public static final int FOR_IMPOSITION = 11;
	public static final int FOR_GENRE = 29;
	public static final int FOR_AUTORITEFISCALE = 22;

	public static final int TACHE_ETAT = 11;
	public static final int TACHE_TYPE = 15;
	public static final int TACHE_CATEGORIE_ENTREPRISE = 10;
	public static final int TACHE_COMMENTAIRE = 100;

	public static final int SITUATIONFAMILLE_TARIF = 11;
	public static final int SITUATIONFAMILLE_ETATCIVIL = 34;

	public static final int DPI_NOM1 = 250;
	public static final int DPI_NOM2 = 250;
	public static final int DPI_CATEGORIEIS = 32;
	public static final int DPI_PERIODICITE = 11;
	public static final int DPI_PERIODE_DECOMPTE = 3;
	public static final int DPI_MODECOM = 12;

	public static final int AC_FORME = 5;
	public static final int PM_FORME = 15;

	public static final int MVTDOSSIER_ETAT = 15;

	public static final int EVTFISCAL_TYPE = 29;
	public static final int EVTFISCAL_TYPE_EVT_DECLARATION = 15;
	public static final int EVTFISCAL_TYPE_EVT_FOR = 20;
	public static final int EVTFISCAL_TYPE_EVT_PARENTE = 25;
	public static final int EVTFISCAL_TYPE_EVT_ALLEGEMENT = 15;
	public static final int EVTFISCAL_TYPE_EVT_REGIME = 15;
	public static final int EVTFISCAL_TYPE_EVT_FLAG = 15;
	public static final int EVTFISCAL_TYPE_EVT_INFO_COMPLEMENTAIRE = 60;
	public static final int EVTFISCAL_TYPE_EVT_BATIMENT = 24;
	public static final int EVTFISCAL_TYPE_EVT_DROIT = 12;
	public static final int EVTFISCAL_TYPE_EVT_SERVITUDE = 16;
	public static final int EVTFISCAL_TYPE_EVT_IMMEUBLE = 39;
	public static final int EVTFISCAL_TYPE_EVT_IMPLANTATION = 12;
	public static final int EVTFISCAL_TYPE_EVT_RAPPROCHEMENT = 10;

	public static final int EVTCIVILREG_ETAT = 10;
	public static final int EVTCIVILREG_TYPE = 45;
	public static final int EVTCIVILREG_COMMENT = 255;

	public static final int EVTCIVILECH_ETAT = 10;
	public static final int EVTCIVILECH_TYPE = 40;
	public static final int EVTCIVILECH_ACTION = 20;
	public static final int EVTCIVILECH_COMMENT = 255;

	public static final int EVTCIVILERREUR_TYPE = 7;
	public static final int EVTCIVILERREUR_MESSAGE = 1024;

	public static final int EVTORGANISATION_ETAT = 10;
	public static final int EVTORGANISATION_TYPE = 120;
	public static final int EVTORGANISATION_COMMENT = 255;
	public static final int EVTORGANISATION_BUSINESS_ID = 255;

	public static final int EVTORGANISATIONERREUR_TYPE = 7;
	public static final int EVTORGANISATIONERREUR_MESSAGE = 1024;

	public static final int REFANNONCEIDE_BUSINESS_ID = 64;

	public static final int EVTEXTERNE_ETAT = 10;
	public static final int EVTEXTERNE_QLR_TYPE = 13;

	public static final int ADRESSE_TYPETIERS = 14;
	public static final int ADRESSE_TYPECIVIL = 10;
	public static final int ADRESSE_TYPEPM = 11;
	public static final int ADRESSE_TYPESUPPLEM = 15;
	public static final int ADRESSE_NUM = 35;
	public static final int ADRESSE_NOM = 100;
	public static final int ADRESSE_CIVILITE = 30;

	public static final int LOG_LEVEL = 7;
	public static final int LOG_MESSAGE = 255;

	public static final int DI_TYPE_CTB = 17;
	public static final int DI_ETAT = 12;
	public static final int DI_QUALIF = 16;
	public static final int DI_ADRESSE_RETOUR = 4;
	public static final int DI_CODE_CONTROLE = 6;

	public static final int MODELEDOC_TYPE = 32;

	public static final int DOCINDEX_NOM = 100;
	public static final int DOCINDEX_DESC = 255;

	public static final int NUMERO_IMMEUBLE = 20;
	public static final int NATURE_IMMEUBLE = 255;
	public static final int GENRE_PROPRIETE = 12;
	public static final int DERNIERE_MUTATION = 28;
	public static final int TYPE_IMMEUBLE = 27;
	public static final int NOM_COMMUNE = 255;
	public static final int REF_ESTIM_FISCALE = 255;
	public static final int LIEN_RF = 500;

	public static final int REQDES_ROLE = 10;
	public static final int REQDES_ETAT_TRAITEMENT = 10;
	public static final int REQDES_ERREUR_MESSAGE = 1024;
	public static final int REQDES_ERREUR_TYPE = 7;
	public static final int REQDES_NUMERO_MINUTE = 30;
	public static final int REQDES_TRANSACTION_DESCRIPTION = 100;
	public static final int REQDES_MODE_INSCRIPTION = 12;
	public static final int REQDES_TYPE_INSCRIPTION = 15;
	public static final int REQDES_LIBELLE_ORIGINE = 50;

	public static final int DELAI_DECL_ETAT = 10;

	public static final int CLE_ARCHIVAGE_FOLDERS = 40;
	public static final int CLE_DOCUMENT_DPERM = 256;

	public static final int ETB_ENSEIGNE = 250;
	public static final int ETB_RAISON_SOCIALE = 250;

	public static final int REGIME_FISCAL_PORTEE = 2;
	public static final int REGIME_FISCAL_TYPE = 10;

	public static final int ALLEGEMENT_FISCAL_TYPE_COLLECTIVITE = 15;
	public static final int ALLEGEMENT_FISCAL_TYPE_IMPOT = 50;
	public static final int ALLEGEMENT_FISCAL_TYPE_ICC = 25;
	public static final int ALLEGEMENT_FISCAL_TYPE_IFD = 25;

	public static final int FLAG_ENTREPRISE_TYPE = 31;

	public static final int ETATENT_ETAT = 20;
	public static final int ETATENT_TYPE_GENERATION = 11;

	public static final int PARAMETRE_PF_TYPE = 5;
	public static final int PARAMETRE_PF_REF_DELAI = 12;
	public static final int PARAMETRE_PF_TYPE_DOCUMENT_EMOLUMENT = 15;

	public static final int MONNAIE_ISO = 3;

	public static final int MANDAT_TYPE = 15;
	public static final int MANDAT_PERSONNE_CONTACT = 100;
	public static final int MANDAT_GENRE_IMPOT = 10;

	public static final int AUTRE_DOCUMENT_FISCAL_TYPE = 25;
	public static final int LETTRE_BIENVENUE_TYPE = 20;

	public static final int ETIQUETTE_CODE = 50;
	public static final int ETIQUETTE_LIBELLE = 100;
	public static final int ETIQUETTE_TYPE_TIERS = 15;
	public static final int ETIQUETTE_AUTO_DECES = 255;

	public static final int ETIQUETTE_TIERS_COMMENTAIRE = 200;

	public static final int RAPPROCHEMENT_RF_TYPE = 15;

	public static final int RF_TYPE_IMPORT = 12;
	public static final int RF_ETAT_EVENEMENT = 13;
	public static final int RF_TYPE_ENTITE = 14;
	public static final int RF_TYPE_MUTATION = 13;
	public static final int RF_ID_RF = 33;
	public static final int RF_FILE_URL = 512;
	public static final int RF_ERROR_MESSAGE = 1000;
	public static final int RF_TYPE_BATIMENT = 255;
	public static final int RF_PM_NUMRC = 20;
	public static final int RF_TIERS_RAISON_SOCIALE = 255;
	public static final int RF_TYPE_COMMUNAUTE = 22;
	public static final int RF_NOM_COMMUNE = 50;
	public static final int RF_IDENTIFIANT_DROIT = 15;
	public static final int RF_GENRE_PROPRIETE = 14;
	public static final int RF_NO_AFFAIRE = 40;
	public static final int RF_MOTIF = 255;
	public static final int RF_REFERENCE_ESTIMATION = 25;
	public static final int RF_EGRID = 14;
	public static final int RF_URL_INTERCAPI = 2000;
	public static final int RF_NOM_PP = 250;
	public static final int RF_PRENOM_PP = 250;
	public static final int RF_TYPE_SURFACE_AU_SOL = 250;

	public static final int AFONC_TYPE = 20;

	public static final int MAXLEN = 2000;

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
