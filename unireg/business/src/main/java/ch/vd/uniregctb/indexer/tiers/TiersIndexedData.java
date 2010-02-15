package ch.vd.uniregctb.indexer.tiers;

import java.util.Date;

import org.apache.lucene.document.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.indexer.LuceneEngine;

public class TiersIndexedData {

	//private static final Logger LOGGER = Logger.getLogger(TiersIndexedData.class);

	public static final String NOM1 = "D_NOM1";
	public static final String NOM2 = "D_NOM2";
	public static final String ROLE_LIGNE1 = "D_ROLE1";
	public static final String ROLE_LIGNE2 = "D_ROLE2";
	public static final String DATE_NAISSANCE = "D_DATE_NAISSANCE";
	public static final String DATE_DECES = "D_DATE_DECES";
	public static final String DATE_OUVERTURE_FOR = "D_DATE_OUVERTURE_FOR";
	public static final String DATE_FERMETURE_FOR = "D_DATE_FERMETURE_FOR";
	public static final String RUE = "D_RUE";
	public static final String NPA = "D_NPA";
	public static final String LOCALITE = "D_LOCALITE";
	public static final String LOCALITE_PAYS = "D_LOCALITE_PAYS";
	public static final String PAYS = "D_PAYS";
	public static final String FOR_PRINCIPAL = "D_FOR_PRINCIPAL";
	public static final String ANNULE = "D_ANNULE";
	public static final String DEBITEUR_INACTIF = "D_DEBITEUR_INACTIF";
	public static final String DOMICILE_VD = "D_DOMICILE_VD";
	public static final String NO_OFS_DOMICILE_VD = "D_NO_OFS_DOMICILE_VD";
	public static final String INDEXATION_DATE = "D_INDEXATION_DATE";

	private final Document document;

	public TiersIndexedData(Document doc) {
		document = doc;
	}

	/**
	 * @return le type de tiers indexé, c'est-à-dire le nom de la classe concrète en lettres minuscules (e.g. 'nonhabitant,
	 *         'debiteurprestationimposable', ...)
	 * @see {@link HabitantIndexable#SUB_TYPE}, {@link NonHabitantIndexable#SUB_TYPE}, {@link EntrepriseIndexable#SUB_TYPE},
	 *      {@link MenageCommunIndexable#SUB_TYPE}, {@link AutreCommunauteIndexable#SUB_TYPE}, {@link EntrepriseIndexable#SUB_TYPE},
	 *      {@link DebiteurPrestationImposableIndexable#SUB_TYPE}
	 */
	public String getTiersType() {
		String str = getDocValue(LuceneEngine.F_DOCSUBTYPE);
		return str;
	}

	public Long getNumero() {
		String str = getDocValue(LuceneEngine.F_ENTITYID);
		return Long.parseLong(str);
	}

	public String getDateNaissance() {
		return getDocValue(TiersIndexedData.DATE_NAISSANCE);
	}

	public String getDateDeces() {
		return getDocValue(TiersIndexedData.DATE_DECES);
	}

	public String getNom1() {
		return getDocValue(TiersIndexedData.NOM1);
	}

	public String getNom2() {
		return getDocValue(TiersIndexedData.NOM2);
	}

	public String getRoleLigne1() {
		return getDocValue(TiersIndexedData.ROLE_LIGNE1);
	}
	public String getRoleLigne2() {
		return getDocValue(TiersIndexedData.ROLE_LIGNE2);
	}

	public Date getDateOuvertureFor() {
		String sDate = getDocValue(TiersIndexedData.DATE_OUVERTURE_FOR);
		Date sDateRtr = DateHelper.indexStringToDate(sDate);
		//LOGGER.debug("sDate:" + sDate + " Date: '" + sDateRtr + "'");
		return sDateRtr;
	}

	public Date getDateFermetureFor() {
		String sDate = getDocValue(TiersIndexedData.DATE_FERMETURE_FOR);
		Date sDateRtr = DateHelper.indexStringToDate(sDate);
		//LOGGER.debug("sDate:" + sDate + " Date: '" + sDateRtr + "'");
		return sDateRtr;
	}

	public String getRue() {
		return getDocValue(TiersIndexedData.RUE);
	}

	public String getNpa() {
		return getDocValue(TiersIndexedData.NPA);
	}

	public String getLocalite() {
		return getDocValue(TiersIndexedData.LOCALITE);
	}

	public String getPays() {
		return getDocValue(TiersIndexedData.PAYS);
	}

	public String getLocaliteOuPays() {
		return getDocValue(TiersIndexedData.LOCALITE_PAYS);
	}

	public String getForPrincipal() {
		return getDocValue(TiersIndexedData.FOR_PRINCIPAL);
	}

	public boolean isAnnule() {
		return getDocValue(TiersIndexedData.ANNULE).equals(Constants.OUI);
	}

	public boolean isDebiteurInactif() {
		return getDocValue(TiersIndexedData.DEBITEUR_INACTIF).equals(Constants.OUI);
	}

	/**
	 * @return <b>true</b> si le contribuable est domicilié dans le canton de Vaud, <b>false</b> s'il est domicilié hors-Canton ou
	 *         hors-Suisse et <b>null</b> si cette information n'est pas disponible.
	 */
	public Boolean isDomicilieDansLeCanton() {
		final String estDansLeCanton = getDocValue(TiersIndexedData.DOMICILE_VD);
		if (estDansLeCanton == null || "".equals(estDansLeCanton)) {
			return null;
		}
		return Constants.OUI.equals(estDansLeCanton);
	}

	/**
	 * @return le numéro Ofs étendu de la commune de domicile du tiers, ou <b>null</b> si cette information n'est pas disponible ou si le
	 *         tiers n'est pas domicilié dans le canton.
	 */
	public Integer getNoOfsCommuneDomicile() {
		final String noOfs = getDocValue(TiersIndexedData.NO_OFS_DOMICILE_VD);
		if (noOfs == null || "".equals(noOfs)) {
			return null;
		}
		return Integer.valueOf(noOfs);
	}

	public Date getIndexationDate() {
		String string = getDocValue(INDEXATION_DATE);
		long milliseconds = Long.parseLong(string);
		return new Date(milliseconds);
	}

	/**
	 * Renvoie la valeur dans le document Lucene Ou chaine vide si non trouvé Ne renvoie jamais NULL
	 *
	 * @param key
	 * @return la valeur du document Lucene
	 */
	private String getDocValue(String key) {

		String str = document.get(key);
		if (str == null) {
			str = "";
		}
		return str;
	}

	public Document getDocument() {
		return document;
	}
}
