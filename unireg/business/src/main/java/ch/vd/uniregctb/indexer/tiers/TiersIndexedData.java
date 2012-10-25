package ch.vd.uniregctb.indexer.tiers;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;

public class TiersIndexedData implements Serializable {

	private static final long serialVersionUID = 1L;

	//private static final Logger LOGGER = Logger.getLogger(TiersIndexableData.class);

	private final String tiersType;
	private final String numero;
	private final String dateNaissance;
	private final RegDate regDateNaissance;
	private final String dateDeces;
	private final String nom1;
	private final String nom2;
	private final String roleLigne1;
	private final String roleLigne2;
	private final Date dateOuvertureFor;
	private final Date dateFermetureFor;
	private final String rue;
	private final String npa;
	private final String localite;
	private final String pays;
	private final String localiteOuPays;
	private final String forPrincipal;
	private final String categorieImpotSource;
	private final boolean annule;
	private final boolean debiteurInactif;
	private final Boolean dansLeCanton;
	private final Integer noOfsCommuneDomicile;
	private final Long ancienNumeroSourcier;

	public TiersIndexedData(Document doc) {
		tiersType = getDocValue(LuceneHelper.F_DOCSUBTYPE, doc);
		numero = getDocValue(LuceneHelper.F_ENTITYID, doc);
		dateNaissance = getDocValue(TiersIndexableData.DATE_NAISSANCE, doc);
		regDateNaissance = indexStringToDateNaissance(dateNaissance, tiersType);
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
		categorieImpotSource = getDocValue(TiersIndexableData.CATEGORIE_DEBITEUR_IS, doc);
		annule = Constants.OUI.equals(getDocValue(TiersIndexableData.ANNULE, doc));
		debiteurInactif = Constants.OUI.equals(getDocValue(TiersIndexableData.DEBITEUR_INACTIF, doc));
		ancienNumeroSourcier = getLongValue(TiersIndexableData.ANCIEN_NUMERO_SOURCIER, doc);

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

	private static RegDate indexStringToDateNaissance(String dateNaissance, String tiersType) {
		if (tiersType.equals(MenageCommunIndexable.SUB_TYPE)) {
			// [UNIREG-2633] on n'affiche pas de dates de naissance sur les ménages communs
			return null;
		}

		try {
			return RegDateHelper.StringFormat.INDEX.fromString(dateNaissance, true);
		}
		catch (ParseException e) {
			return null;
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

	/**
	 * @return la date de naissance de la personne physique (RegDate); ou <b>null</b> pour tous les autres types de tiers.
	 */
	public RegDate getRegDateNaissance() {
		return regDateNaissance;
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

	public String getCategorieImpotSource() {
		return categorieImpotSource;
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

	public Long getAncienNumeroSourcier() {
		return ancienNumeroSourcier;
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

	/**
	 * Renvoie la valeur dans le document Lucene Ou chaine vide si non trouvé Ne renvoie jamais NULL
	 *
	 * @param key      la clé sous laquelle est stocké la valeur
	 * @param document le document Lucene
	 * @return la valeur du document Lucene
	 */
	private static Long getLongValue(String key, Document document) {
		String str = document.get(key);
		if (str == null || StringUtils.isBlank(str)) {
			return null;
		}
		return Long.valueOf(str);
	}
}
