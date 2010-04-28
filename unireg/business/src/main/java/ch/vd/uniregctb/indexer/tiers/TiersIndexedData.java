package ch.vd.uniregctb.indexer.tiers;

import java.io.Serializable;
import java.util.Date;

import org.apache.lucene.document.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.indexer.LuceneEngine;

public class TiersIndexedData implements Serializable {

	private static final long serialVersionUID = 1L;

	//private static final Logger LOGGER = Logger.getLogger(TiersIndexableData.class);

	private String tiersType;
	private String numero;
	private String dateNaissance;
	private String dateDeces;
	private String nom1;
	private String nom2;
	private String roleLigne1;
	private String roleLigne2;
	private Date dateOuvertureFor;
	private Date dateFermetureFor;
	private String rue;
	private String npa;
	private String localite;
	private String pays;
	private String localiteOuPays;
	private String forPrincipal;
	private boolean annule;
	private boolean debiteurInactif;
	private Boolean dansLeCanton;
	private Integer noOfsCommuneDomicile;

	public TiersIndexedData(Document doc) {
		tiersType = getDocValue(LuceneEngine.F_DOCSUBTYPE, doc);
		numero = getDocValue(LuceneEngine.F_ENTITYID, doc);
		dateNaissance = getDocValue(TiersIndexableData.DATE_NAISSANCE, doc);
		dateDeces = getDocValue(TiersIndexableData.DATE_DECES, doc);
		nom1 = getDocValue(TiersIndexableData.NOM1, doc);
		nom2 = getDocValue(TiersIndexableData.NOM2, doc);
		roleLigne1 = getDocValue(TiersIndexableData.ROLE_LIGNE1, doc);
		roleLigne2 = getDocValue(TiersIndexableData.ROLE_LIGNE2, doc);
		dateOuvertureFor = DateHelper.indexStringToDate(getDocValue(TiersIndexableData.DATE_OUVERTURE_FOR, doc));
		dateFermetureFor = DateHelper.indexStringToDate(getDocValue(TiersIndexableData.DATE_FERMETURE_FOR, doc));
		rue = getDocValue(TiersIndexableData.RUE, doc);
		npa = getDocValue(TiersIndexableData.NPA, doc);
		localite = getDocValue(TiersIndexableData.LOCALITE, doc);
		pays = getDocValue(TiersIndexableData.PAYS, doc);
		localiteOuPays = getDocValue(TiersIndexableData.LOCALITE_PAYS, doc);
		forPrincipal = getDocValue(TiersIndexableData.FOR_PRINCIPAL, doc);
		annule = Constants.OUI.equals(getDocValue(TiersIndexableData.ANNULE, doc));
		debiteurInactif = Constants.OUI.equals(getDocValue(TiersIndexableData.DEBITEUR_INACTIF, doc));

		final String estDansLeCanton = getDocValue(TiersIndexableData.DOMICILE_VD, doc);
		if (estDansLeCanton == null || "".equals(estDansLeCanton)) {
			dansLeCanton = null;
		}
		else {
			dansLeCanton = Constants.OUI.equals(estDansLeCanton);
		}

		final String noOfs = getDocValue(TiersIndexableData.NO_OFS_DOMICILE_VD, doc);
		if (noOfs == null || "".equals(noOfs)) {
			noOfsCommuneDomicile = null;
		}
		else {
			noOfsCommuneDomicile = Integer.valueOf(noOfs);
		}
	}

	/**
	 * @return le type de tiers indexé, c'est-à-dire le nom de la classe concrète en lettres minuscules (e.g. 'nonhabitant, 'debiteurprestationimposable', ...)
	 * @see {@link HabitantIndexable#SUB_TYPE}, {@link NonHabitantIndexable#SUB_TYPE}, {@link EntrepriseIndexable#SUB_TYPE}, {@link MenageCommunIndexable#SUB_TYPE}, {@link
	 *      AutreCommunauteIndexable#SUB_TYPE}, {@link EntrepriseIndexable#SUB_TYPE}, {@link DebiteurPrestationImposableIndexable#SUB_TYPE}
	 */
	public String getTiersType() {
		return tiersType;
	}

	public Long getNumero() {
		return Long.parseLong(numero);
	}

	public String getDateNaissance() {
		return dateNaissance;
	}

	public String getDateDeces() {
		return dateDeces;
	}

	public String getNom1() {
		return nom1;
	}

	public String getNom2() {
		return nom2;
	}

	public String getRoleLigne1() {
		return roleLigne1;
	}

	public String getRoleLigne2() {
		return roleLigne2;
	}

	public Date getDateOuvertureFor() {
		return dateOuvertureFor;
	}

	public Date getDateFermetureFor() {
		return dateFermetureFor;
	}

	public String getRue() {
		return rue;
	}

	public String getNpa() {
		return npa;
	}

	public String getLocalite() {
		return localite;
	}

	public String getPays() {
		return pays;
	}

	public String getLocaliteOuPays() {
		return localiteOuPays;
	}

	public String getForPrincipal() {
		return forPrincipal;
	}

	public boolean isAnnule() {
		return annule;
	}

	public boolean isDebiteurInactif() {
		return debiteurInactif;
	}

	/**
	 * @return <b>true</b> si le contribuable est domicilié dans le canton de Vaud, <b>false</b> s'il est domicilié hors-Canton ou hors-Suisse et <b>null</b> si cette information n'est pas disponible.
	 */
	public Boolean isDomicilieDansLeCanton() {
		return dansLeCanton;
	}

	/**
	 * @return le numéro Ofs étendu de la commune de domicile du tiers, ou <b>null</b> si cette information n'est pas disponible ou si le tiers n'est pas domicilié dans le canton.
	 */
	public Integer getNoOfsCommuneDomicile() {
		return noOfsCommuneDomicile;
	}

	/**
	 * Renvoie la valeur dans le document Lucene Ou chaine vide si non trouvé Ne renvoie jamais NULL
	 *
	 * @param key
	 * @param document
	 * @return la valeur du document Lucene
	 */
	private static String getDocValue(String key, Document document) {

		String str = document.get(key);
		if (str == null) {
			str = "";
		}
		return str;
	}
}
