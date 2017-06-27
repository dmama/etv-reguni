package ch.vd.uniregctb.indexer.tiers;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.uniregctb.avatar.TypeAvatar;
import ch.vd.uniregctb.indexer.lucene.DocumentExtractorHelper;
import ch.vd.uniregctb.indexer.lucene.LuceneHelper;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

public class TiersIndexedData implements Serializable {

	private static final long serialVersionUID = -6497510265480092708L;

	//private static final Logger LOGGER = LoggerFactory.getLogger(TiersIndexableData.class);

	private final String tiersType;
	private final String numero;
	private final String dateNaissanceInscriptionRC;
	private final RegDate regDateNaissanceInscriptionRC;
	private final String dateDeces;
	private final String nom1;
	private final String nom2;
	private final String navs13_1;
	private final String navs13_2;
	private final String roleLigne1;
	private final String roleLigne2;
	private final RegDate dateOuvertureFor;
	private final RegDate dateFermetureFor;
	private final RegDate dateOuvertureForVd;
	private final RegDate dateFermetureForVd;
	private final String rue;
	private final String npa;
	private final String localite;
	private final String pays;
	private final String localiteOuPays;
	private final String forPrincipal;
	private final CategorieImpotSource categorieImpotSource;
	private final ModeCommunication modeCommunication;
	private final TypeAssujettissement assujettissementPP;
	private final TypeAssujettissement assujettissementPM;
	private final boolean annule;
	private final boolean debiteurInactif;
	private final Boolean dansLeCanton;
	private final Integer noOfsCommuneDomicile;
	private final Long ancienNumeroSourcier;
	private final List<String> numerosIDE;
	private final TypeAvatar typeAvatar;
	private final TypeEtatEntreprise etatEntreprise;
	private final Set<TypeEtatEntreprise> tousEtatsEntreprise;
	private final String domicileEtablissementPrincipal;
	private final FormeLegale formeJuridique;

	public TiersIndexedData(Document doc) {
		tiersType = DocumentExtractorHelper.getDocValue(LuceneHelper.F_DOCSUBTYPE, doc);
		numero = DocumentExtractorHelper.getDocValue(LuceneHelper.F_ENTITYID, doc);
		dateNaissanceInscriptionRC = DocumentExtractorHelper.getDocValue(TiersIndexableData.D_DATE_NAISSANCE, doc);
		regDateNaissanceInscriptionRC = indexStringToDateNaissanceInscriptionRC(dateNaissanceInscriptionRC, tiersType);
		dateDeces = DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_DECES, doc);
		nom1 = DocumentExtractorHelper.getDocValue(TiersIndexableData.NOM1, doc);
		nom2 = DocumentExtractorHelper.getDocValue(TiersIndexableData.NOM2, doc);
		navs13_1 = DocumentExtractorHelper.getDocValue(TiersIndexableData.NAVS13_1, doc);
		navs13_2 = DocumentExtractorHelper.getDocValue(TiersIndexableData.NAVS13_2, doc);
		roleLigne1 = DocumentExtractorHelper.getDocValue(TiersIndexableData.ROLE_LIGNE1, doc);
		roleLigne2 = DocumentExtractorHelper.getDocValue(TiersIndexableData.ROLE_LIGNE2, doc);
		dateOuvertureFor = DocumentExtractorHelper.indexStringToDate(DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_OUVERTURE_FOR, doc), false);
		dateFermetureFor = DocumentExtractorHelper.indexStringToDate(DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_FERMETURE_FOR, doc), false);
		dateOuvertureForVd = DocumentExtractorHelper.indexStringToDate(DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_OUVERTURE_FOR_VD, doc), false);
		dateFermetureForVd = DocumentExtractorHelper.indexStringToDate(DocumentExtractorHelper.getDocValue(TiersIndexableData.DATE_FERMETURE_FOR_VD, doc), false);
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
		assujettissementPM = DocumentExtractorHelper.getEnumValue(TiersIndexableData.ASSUJETTISSEMENT_PM, doc, TypeAssujettissement.class);
		numerosIDE = DocumentExtractorHelper.getList(DocumentExtractorHelper.getDocValue(TiersIndexableData.NUM_IDE, doc));
		typeAvatar = DocumentExtractorHelper.getEnumValue(TiersIndexableData.AVATAR, doc, TypeAvatar.class);
		etatEntreprise = DocumentExtractorHelper.getEnumValue(TiersIndexableData.ETAT_ENTREPRISE_COURANT, doc, TypeEtatEntreprise.class);
		tousEtatsEntreprise = DocumentExtractorHelper.getEnumSet(DocumentExtractorHelper.getDocValues(TiersIndexableData.ETATS_ENTREPRISE, doc), TypeEtatEntreprise.class);
		formeJuridique = DocumentExtractorHelper.getValue(TiersIndexableData.FORME_JURIDIQUE, doc, FormeLegale::fromCode);
		domicileEtablissementPrincipal = DocumentExtractorHelper.getDocValue(TiersIndexableData.DOMICILE_ETABLISSEMENT_PRINCIPAL, doc);
	}

	private static RegDate indexStringToDateNaissanceInscriptionRC(String dateNaissance, String tiersType) {
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

	public String getDateNaissanceInscriptionRC() {
		return dateNaissanceInscriptionRC;
	}

	/**
	 * @return la date de naissance de la personne physique (RegDate); ou <b>null</b> pour tous les autres types de tiers.
	 */
	public RegDate getRegDateNaissanceInscriptionRC() {
		return regDateNaissanceInscriptionRC;
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

	public RegDate getDateOuvertureFor() {
		return dateOuvertureFor;
	}

	public RegDate getDateFermetureFor() {
		return dateFermetureFor;
	}

	public RegDate getDateOuvertureForVd() {
		return dateOuvertureForVd;
	}

	public RegDate getDateFermetureForVd() {
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

	/**
	 * @return le type d'assujettissement "Entreprise" du contribuable; <b>null</b> si le tiers n'est pas une entreprise.
	 */
	public TypeAssujettissement getAssujettissementPM() {
		return assujettissementPM;
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

	/**
	 * @return l'état courant de l'entreprise (<b>null</b> pour les autres types de tiers)
	 */
	public TypeEtatEntreprise getEtatEntreprise() {
		return etatEntreprise;
	}

	/**
	 * @return l'ensemble des états d'entreprise par lesquels l'entreprise est passée (vide pour les autres types de tiers)
	 */
	public Set<TypeEtatEntreprise> getTousEtatsEntreprise() {
		return tousEtatsEntreprise;
	}

	/**
	 * @return le nom de la commune/du pays du domicile de l'établissement principal de l'entreprise (= siège)
	 */
	public String getDomicileEtablissementPrincipal() {
		return domicileEtablissementPrincipal;
	}

	/**
	 * @return le nom de la forme juridique de l'entreprise
	 */
	public FormeLegale getFormeJuridique() {
		return formeJuridique;
	}
}
