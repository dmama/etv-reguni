package ch.vd.uniregctb.indexer.tiers;

import java.util.Date;

import org.apache.lucene.document.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.indexer.LuceneEngine;

public class TiersIndexedData {

	//private static final Logger LOGGER = Logger.getLogger(TiersIndexableData.class);

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
		return getDocValue(TiersIndexableData.DATE_NAISSANCE);
	}

	public String getDateDeces() {
		return getDocValue(TiersIndexableData.DATE_DECES);
	}

	public String getNom1() {
		return getDocValue(TiersIndexableData.NOM1);
	}

	public String getNom2() {
		return getDocValue(TiersIndexableData.NOM2);
	}

	public String getRoleLigne1() {
		return getDocValue(TiersIndexableData.ROLE_LIGNE1);
	}
	public String getRoleLigne2() {
		return getDocValue(TiersIndexableData.ROLE_LIGNE2);
	}

	public Date getDateOuvertureFor() {
		final String sDate = getDocValue(TiersIndexableData.DATE_OUVERTURE_FOR);
		return DateHelper.indexStringToDate(sDate);
	}

	public Date getDateFermetureFor() {
		final String sDate = getDocValue(TiersIndexableData.DATE_FERMETURE_FOR);
		return DateHelper.indexStringToDate(sDate);
	}

	public String getRue() {
		return getDocValue(TiersIndexableData.RUE);
	}

	public String getNpa() {
		return getDocValue(TiersIndexableData.NPA);
	}

	public String getLocalite() {
		return getDocValue(TiersIndexableData.LOCALITE);
	}

	public String getPays() {
		return getDocValue(TiersIndexableData.PAYS);
	}

	public String getLocaliteOuPays() {
		return getDocValue(TiersIndexableData.LOCALITE_PAYS);
	}

	public String getForPrincipal() {
		return getDocValue(TiersIndexableData.FOR_PRINCIPAL);
	}

	public boolean isAnnule() {
		return Constants.OUI.equals(getDocValue(TiersIndexableData.ANNULE));
	}

	public boolean isDebiteurInactif() {
		return Constants.OUI.equals(getDocValue(TiersIndexableData.DEBITEUR_INACTIF));
	}

	/**
	 * @return <b>true</b> si le contribuable est domicilié dans le canton de Vaud, <b>false</b> s'il est domicilié hors-Canton ou
	 *         hors-Suisse et <b>null</b> si cette information n'est pas disponible.
	 */
	public Boolean isDomicilieDansLeCanton() {
		final String estDansLeCanton = getDocValue(TiersIndexableData.DOMICILE_VD);
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
		final String noOfs = getDocValue(TiersIndexableData.NO_OFS_DOMICILE_VD);
		if (noOfs == null || "".equals(noOfs)) {
			return null;
		}
		return Integer.valueOf(noOfs);
	}

	public Date getIndexationDate() {
		String string = getDocValue(TiersIndexableData.INDEXATION_DATE);
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
