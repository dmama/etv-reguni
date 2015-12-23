package ch.vd.uniregctb.indexer.tiers;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.lucene.document.Document;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.avatar.TypeAvatar;
import ch.vd.uniregctb.indexer.lucene.DocumentExtractorHelper;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;

public class TiersIndexedData implements Serializable {

	private static final long serialVersionUID = 1549012269648391839L;

	//private static final Logger LOGGER = LoggerFactory.getLogger(TiersIndexableData.class);

	private final String tiersType;
	private final String numero;
	private final String dateNaissance;
	private final RegDate regDateNaissance;
	private final String dateDeces;
	private final String nom1;
	private final String nom2;
	private final String navs13_1;
	private final String navs13_2;
	private final String roleLigne1;
	private final String roleLigne2;
	private final Date dateOuvertureFor;
	private final Date dateFermetureFor;
	private final Date dateOuvertureForVd;
	private final Date dateFermetureForVd;
	private final String rue;
	private final String npa;
	private final String localite;
	private final String pays;
	private final String localiteOuPays;
	private final String forPrincipal;
	private final CategorieImpotSource categorieImpotSource;
	private final ModeCommunication modeCommunication;
	private final TypeAssujettissement assujettissementPP;
	private final boolean annule;
	private final boolean debiteurInactif;
	private final Boolean dansLeCanton;
	private final Integer noOfsCommuneDomicile;
	private final Long ancienNumeroSourcier;
	private final List<String> numerosIDE;
	private final TypeAvatar typeAvatar;

	public TiersIndexedData(Document doc) {
		tiersType = DocumentExtractorHelper.getDocValue(LuceneHelper.F_DOCSUBTYPE, doc);
		numero = DocumentExtractorHelper.getDocValue(LuceneHelper.F_ENTITYID, doc);
		dateNaissance = DocumentExtractorHelper.getDocValue(TiersIndexableData.D_DATE_NAISSANCE, doc);
		regDateNaissance = indexStringToDateNaissance(dateNaissance, tiersType);
		dateDeces = DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_DECES, doc);
		nom1 = DocumentExtractorHelper.getDocValue(TiersIndexableData.NOM1, doc);
		nom2 = DocumentExtractorHelper.getDocValue(TiersIndexableData.NOM2, doc);
		navs13_1 = DocumentExtractorHelper.getDocValue(TiersIndexableData.NAVS13_1, doc);
		navs13_2 = DocumentExtractorHelper.getDocValue(TiersIndexableData.NAVS13_2, doc);
		roleLigne1 = DocumentExtractorHelper.getDocValue(TiersIndexableData.ROLE_LIGNE1, doc);
		roleLigne2 = DocumentExtractorHelper.getDocValue(TiersIndexableData.ROLE_LIGNE2, doc);
		dateOuvertureFor = DateHelper.indexStringToDate(DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_OUVERTURE_FOR, doc));
		dateFermetureFor = DateHelper.indexStringToDate(DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_FERMETURE_FOR, doc));
		dateOuvertureForVd = DateHelper.indexStringToDate(DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_OUVERTURE_FOR, doc));
		dateFermetureForVd = DateHelper.indexStringToDate(DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_FERMETURE_FOR, doc));
		rue = DocumentExtractorHelper.getDocValue(TiersIndexableData.RUE, doc);
		npa = DocumentExtractorHelper.getDocValue(TiersIndexableData.NPA_COURRIER, doc);
		localite = DocumentExtractorHelper.getDocValue(TiersIndexableData.LOCALITE, doc);
		pays = DocumentExtractorHelper.getDocValue(TiersIndexableData.PAYS, doc);
		localiteOuPays = DocumentExtractorHelper.getDocValue(TiersIndexableData.LOCALITE_PAYS, doc);
		forPrincipal = DocumentExtractorHelper.getDocValue(TiersIndexableData.FOR_PRINCIPAL, doc);
		annule = DocumentExtractorHelper.getBooleanValue(TiersIndexableData.ANNULE, doc, Boolean.FALSE);
		debiteurInactif = DocumentExtractorHelper.getBooleanValue(TiersIndexableData.DEBITEUR_INACTIF, doc, Boolean.FALSE);
		ancienNumeroSourcier = DocumentExtractorHelper.getLongValue(TiersIndexableData.ANCIEN_NUMERO_SOURCIER, doc);
		dansLeCanton = DocumentExtractorHelper.getBooleanValue(TiersIndexableData.DOMICILE_VD, doc, null);
		noOfsCommuneDomicile = DocumentExtractorHelper.getIntegerValue(TiersIndexableData.NO_OFS_DOMICILE_VD, doc);
		categorieImpotSource = DocumentExtractorHelper.getEnumValue(TiersIndexableData.CATEGORIE_DEBITEUR_IS, doc, CategorieImpotSource.class);
		modeCommunication = DocumentExtractorHelper.getEnumValue(TiersIndexableData.MODE_COMMUNICATION, doc, ModeCommunication.class);
		assujettissementPP = DocumentExtractorHelper.getEnumValue(TiersIndexableData.ASSUJETTISSEMENT_PP, doc, TypeAssujettissement.class);
		numerosIDE = DocumentExtractorHelper.getList(DocumentExtractorHelper.getDocValue(TiersIndexableData.IDE, doc));
		typeAvatar = DocumentExtractorHelper.getEnumValue(TiersIndexableData.AVATAR, doc, TypeAvatar.class);
	}

	private static RegDate indexStringToDateNaissance(String dateNaissance, String tiersType) {
		if (tiersType.equals(MenageCommunIndexable.SUB_TYPE)) {
			// [UNIREG-2633] on n'affiche pas de dates de naissance sur les ménages communs
			return null;
		}
		return DocumentExtractorHelper.indexStringToDate(dateNaissance, true);
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

	public String getNavs13_1() {
		return navs13_1;
	}

	public String getNavs13_2() {
		return navs13_2;
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

	public Date getDateOuvertureForVd() {
		return dateOuvertureForVd;
	}

	public Date getDateFermetureForVd() {
		return dateFermetureForVd;
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

	/**
	 * @return le type d'assujettissement "PP" du contribuable; <b>null</b> si le tiers n'est ni une personne physique ni un ménage commun.
	 */
	public TypeAssujettissement getAssujettissementPP() {
		return assujettissementPP;
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
	 * @return la liste des numéros IDE associés au tiers
	 */
	public List<String> getNumerosIDE() {
		return numerosIDE;
	}

	/**
	 * @return le type d'avatar lié au tiers
	 */
	public TypeAvatar getTypeAvatar() {
		return typeAvatar;
	}
}
