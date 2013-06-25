package ch.vd.uniregctb.indexer.tiers;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;

public class TiersIndexedData implements Serializable {

	private static final long serialVersionUID = 3102867183859739055L;

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
	private final CategorieImpotSource categorieImpotSource;
	private final ModeCommunication modeCommunication;
	private final boolean annule;
	private final boolean debiteurInactif;
	private final Boolean dansLeCanton;
	private final Integer noOfsCommuneDomicile;
	private final Long ancienNumeroSourcier;

	public TiersIndexedData(Document doc) {
		tiersType = getDocValue(LuceneHelper.F_DOCSUBTYPE, doc);
		numero = getDocValue(LuceneHelper.F_ENTITYID, doc);
		dateNaissance = getDocValue(TiersIndexableData.D_DATE_NAISSANCE, doc);
		regDateNaissance = indexStringToDateNaissance(dateNaissance, tiersType);
		dateDeces = getDocValue(TiersIndexableData.DATE_DECES, doc);
		nom1 = getDocValue(TiersIndexableData.NOM1, doc);
		nom2 = getDocValue(TiersIndexableData.NOM2, doc);
		roleLigne1 = getDocValue(TiersIndexableData.ROLE_LIGNE1, doc);
		roleLigne2 = getDocValue(TiersIndexableData.ROLE_LIGNE2, doc);
		dateOuvertureFor = DateHelper.indexStringToDate(getDocValue(TiersIndexableData.DATE_OUVERTURE_FOR, doc));
		dateFermetureFor = DateHelper.indexStringToDate(getDocValue(TiersIndexableData.DATE_FERMETURE_FOR, doc));
		rue = getDocValue(TiersIndexableData.RUE, doc);
		npa = getDocValue(TiersIndexableData.NPA_COURRIER, doc);
		localite = getDocValue(TiersIndexableData.LOCALITE, doc);
		pays = getDocValue(TiersIndexableData.PAYS, doc);
		localiteOuPays = getDocValue(TiersIndexableData.LOCALITE_PAYS, doc);
		forPrincipal = getDocValue(TiersIndexableData.FOR_PRINCIPAL, doc);
		annule = getBooleanValue(TiersIndexableData.ANNULE, doc, Boolean.FALSE);
		debiteurInactif = getBooleanValue(TiersIndexableData.DEBITEUR_INACTIF, doc, Boolean.FALSE);
		ancienNumeroSourcier = getLongValue(TiersIndexableData.ANCIEN_NUMERO_SOURCIER, doc);
		dansLeCanton = getBooleanValue(TiersIndexableData.DOMICILE_VD, doc, null);
		noOfsCommuneDomicile = getIntegerValue(TiersIndexableData.NO_OFS_DOMICILE_VD, doc);
		categorieImpotSource = getEnumValue(TiersIndexableData.CATEGORIE_DEBITEUR_IS, doc, CategorieImpotSource.class);
		modeCommunication = getEnumValue(TiersIndexableData.MODE_COMMUNICATION, doc, ModeCommunication.class);
	}

	private static boolean isBlank(String value) {
		return IndexerFormatHelper.isBlank(value);
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

	public CategorieImpotSource getCategorieImpotSource() {
		return categorieImpotSource;
	}

	/**
	 * @return le mode de communication du débiteur; <b>null</b> si le tiers n'est pas un débiteur.
	 */
	public ModeCommunication getModeCommunication() {
		return modeCommunication;
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
	 * @return le numéro Ofs de la commune de domicile du tiers, ou <b>null</b> si cette information n'est pas disponible ou si le tiers n'est pas domicilié dans le canton.
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
		final String str = document.get(key);
		return isBlank(str) ? StringUtils.EMPTY : str;
	}

	/**
	 * Renvoie la valeur dans le document Lucene Ou chaine vide si non trouvé Ne renvoie jamais NULL
	 *
	 * @param key      la clé sous laquelle est stocké la valeur
	 * @param document le document Lucene
	 * @return la valeur du document Lucene
	 */
	private static Long getLongValue(String key, Document document) {
		final String str = document.get(key);
		return isBlank(str) ? null : Long.valueOf(str);
	}

	private static Boolean getBooleanValue(String key, Document document, @Nullable Boolean defaultValue) {
		final String str = document.get(key);
		if (isBlank(str)) {
			return defaultValue;
		}
		else {
			return Constants.OUI.equals(str);
		}
	}

	private static Integer getIntegerValue(String key, Document document) {
		final String str = document.get(key);
		return isBlank(str) ? null : Integer.valueOf(str);
	}

	private static <T extends Enum<T>> T getEnumValue(String key, Document document, Class<T> clazz) {
		final String str = document.get(key);
		return isBlank(str) ? null : Enum.valueOf(clazz, str);
	}
}
