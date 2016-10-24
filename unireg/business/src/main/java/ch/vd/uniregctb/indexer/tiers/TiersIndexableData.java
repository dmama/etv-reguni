package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.avatar.TypeAvatar;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

@SuppressWarnings({"UnusedDeclaration"})
public class TiersIndexableData extends IndexableData {

	/**
	 * La différence entre ce {@link StringRenderer} et {@link IndexerFormatHelper#DEFAULT_RENDERER} est que, en cas de valeur absente,
	 * nous renvoyons ici {@link StringUtils#EMPTY} tandis que {@link IndexerFormatHelper#DEFAULT_RENDERER} renvoie {@link IndexerFormatHelper#nullValue()}.
	 */
	private static final StringRenderer<Enum<?>> ENUM_RENDERER = modality -> modality == null ? StringUtils.EMPTY : modality.name();

	/**
	 * La différence entre ce {@link StringRenderer} et {@link IndexerFormatHelper#BOOLEAN_RENDERER} est que, en cas de valeur absente,
	 * nous renvoyons ici {@link StringUtils#EMPTY} tandis que {@link IndexerFormatHelper#BOOLEAN_RENDERER} renvoie {@link IndexerFormatHelper#nullValue()}.
	 */
	private static final StringRenderer<Boolean> BOOLEAN_RENDERER = object -> object == null ? StringUtils.EMPTY : IndexerFormatHelper.booleanToString(object);

	// champs de recherche
	public static final String NUMEROS = "S_NUMEROS";
	public static final String NOM_RAISON = "S_NOM_RAISON";
	public static final String AUTRES_NOM = "S_AUTRES_NOM";
	public static final String NO_OFS_FOR_PRINCIPAL = "S_NO_OFS_FOR_PRINCIPAL";
	public static final String TYPE_OFS_FOR_PRINCIPAL = "S_TYPE_OFS_FOR_PRINCIPAL";
	public static final String NOS_OFS_AUTRES_FORS = "S_NOS_OFS_AUTRES_FORS";
	public static final String MOTIF_FERMETURE_DERNIER_FOR_PRINCIPAL = "S_MOTIF_FERM_DERNIER_FFP";
	public static final String LOCALITE_PAYS = "S_LOCALITE_PAYS";
	public static final String NPA_COURRIER = "S_NPA_COURRIER";
	public static final String NPA_TOUS = "S_NPA_TOUS";
	public static final String NATURE_JURIDIQUE = "S_NATURE_JURIDIQUE"; // (PP ou PM)
	public static final String FORME_JURIDIQUE = "S_FORME_JURIDIQUE";
	public static final String CATEGORIE_ENTREPRISE = "S_CATEGORY_ENTREPRISE";
	public static final String S_DATE_NAISSANCE_INSCRIPTION_RC = "S_DATE_NAISSANCE_INSCRIPTION_RC";
	public static final String SEXE = "S_SEXE";
	public static final String NAVS11 = "S_NAVS11";
	public static final String NAVS13 = "S_NAVS13";
	public static final String ANCIEN_NUMERO_SOURCIER = "S_ANCIEN_NUMERO_SOURCIER";
	public static final String ANNULE = "S_ANNULE";
	public static final String DEBITEUR_INACTIF = "S_DEBITEUR_INACTIF";
	public static final String CATEGORIE_DEBITEUR_IS = "S_CATEGORIE_DEBITEUR_IS";
	public static final String MODE_IMPOSITION = "S_MODE_IMPOSITION";
	public static final String NO_SYMIC = "S_NO_SYMIC";
	public static final String TIERS_ACTIF = "S_TIERS_ACTIF";
	public static final String IDE = "S_IDE";
	public static final String TOUT = "S_TOUT";
	public static final String ETAT_ENTREPRISE_COURANT = "S_ETAT_ENTREPRISE_COURANT";
	public static final String ETATS_ENTREPRISE = "S_ETATS_ENTREPRISE";
	public static final String INSCRIPTION_RC = "S_INSCRIPTION_RC";
	public static final String CORPORATION_IS_MERGE_RESULT = "S_CORP_MERGE_RESULT";
	public static final String CORPORATION_WAS_SPLIT = "S_CORP_SPLIT";
	public static final String CORPORATION_TRANSFERED_PATRIMONY = "S_CORP_TRANSFERED_PATRIMONY";
	public static final String CONNU_CIVIL = "S_CONNU_CIVIL";
	public static final String TYPE_ETABLISSEMENT = "S_TYPE_ETB";

	// champs de stockage (pas recherchables)
	public static final String NOM1 = "D_NOM1";
	public static final String NOM2 = "D_NOM2";
	public static final String NAVS13_1 = "D_AVS13_1";
	public static final String NAVS13_2 = "D_AVS13_2";
	public static final String ROLE_LIGNE1 = "D_ROLE1";
	public static final String ROLE_LIGNE2 = "D_ROLE2";
	public static final String DATE_DECES = "D_DATE_DECES";
	public static final String RUE = "D_RUE";
	public static final String LOCALITE = "D_LOCALITE";
	public static final String PAYS = "D_PAYS";
	public static final String FOR_PRINCIPAL = "D_FOR_PRINCIPAL";
	public static final String DATE_OUVERTURE_FOR = "D_DATE_OUVERTURE_FOR";
	public static final String DATE_FERMETURE_FOR = "D_DATE_FERMETURE_FOR";
	public static final String DATE_OUVERTURE_FOR_VD = "D_DATE_OUVERTURE_FOR_VD";
	public static final String DATE_FERMETURE_FOR_VD = "D_DATE_FERMETURE_FOR_VD";
	public static final String DOMICILE_VD = "D_DOMICILE_VD";
	public static final String NO_OFS_DOMICILE_VD = "D_NO_OFS_DOMICILE_VD";
	public static final String INDEXATION_DATE = "D_INDEXATION_DATE";
	public static final String MODE_COMMUNICATION = "D_MODE_COMMUNICATION";
	public static final String D_DATE_NAISSANCE = "D_DATE_NAISSANCE";
	public static final String ASSUJETTISSEMENT_PP = "D_ASSUJETTISSEMENT_PP";
	public static final String ASSUJETTISSEMENT_PM = "D_ASSUJETTISSEMENT_PM";
	public static final String AVATAR = "D_AVATAR";
	public static final String DOMICILE_ETABLISSEMENT_PRINCIPAL = "D_DOMICILE_ETABLISSEMENT_PRINCIPAL";

	// champs de recherche
	private String numeros;
	private String nomRaison;
	private String autresNom;
	private List<RegDate> datesNaissanceInscriptionRC;       // valeurs utilisées pour la recherche (calculées à partir de la date connue)
	private String sexe;
	private String noOfsForPrincipal;
	private String typeOfsForPrincipal;
	private String nosOfsAutresFors;
	private MotifFor motifFermetureDernierForPrincipal;
	private String npaCourrier;
	private String npaTous;
	private String localiteEtPays;
	private String natureJuridique;
	private String formeJuridique;
	private String categorieEntreprise;
	private String navs11;
	private String navs13;
	private String ancienNumeroSourcier;
	private String categorieDebiteurIs;
	private String modeImposition;
	private String noSymic;
	private Boolean tiersActif;
	private Boolean annule;
	private Boolean debiteurInactif;
	private String ide;                     // identifiant d'entreprise [SIFISC-11689]
	private TypeEtatEntreprise etatEntrepriseCourant;
	private Set<TypeEtatEntreprise> etatsEntreprise;
	private TypeEtatInscriptionRC etatInscriptionRC;
	private Boolean corporationMergeResult;     // vrai si l'entreprise a été par le passé le résultat d'une fusion d'entreprises
	private Boolean connuAuCivil;               // vrai si la personne physique, l'entreprise ou l'établissement est connu au civil, false sinon (vide si non-applicable)
	private Boolean corporationSplit;           // vrai si l'entreprise a subi une scission
	private Boolean corporationTransferedPatrimony; // vrai si l'entreprise a émis (= transmis) du patrimoine à une autre entreprise
	private Set<TypeEtablissement> typesEtablissement;

	// champs de stockage (pas recherchables)
	private String nom1;
	private String nom2;
	private String navs13_1;
	private String navs13_2;
	private String roleLigne1;
	private String roleLigne2;
	private String dateDeces;
	private String rue;
	private String localite;
	private String pays;
	private String forPrincipal;
	private String dateOuvertureFor;
	private String dateFermetureFor;
	private String dateOuvertureForVd;
	private String dateFermetureForVd;
	private String domicileVd;
	private String noOfsDomicileVd;
	private String indexationDate;
	private ModeCommunication modeCommunication;   // uniquement renseigné sur les débiteurs (SIFISC-6587)
	private TypeAssujettissement assujettissementPP;  // seulement sur les PP/MC (SIFISC-11102)
	private TypeAssujettissement assujettissementPM;  // seulement sur les entreprises (SIFISC-21524)
	private TypeAvatar typeAvatar;
	private String domicileEtablissementPrincipal;

	public TiersIndexableData(Long id, String type, String subType) {
		super(id, type, subType);
	}

	@Override
	public Document asDoc() {

		final Document d = super.asDoc();

		// Note : pour des raisons de performance de l'index Lucene, il est important que l'ordre des champs soit constant

		// champs de recherche
		addNotAnalyzedValue(d, TiersIndexableData.NUMEROS, numeros);
		addAnalyzedValue(d, TiersIndexableData.NOM_RAISON, nomRaison);
		addAnalyzedValue(d, TiersIndexableData.AUTRES_NOM, autresNom);
		addAnalyzedValue(d, TiersIndexableData.S_DATE_NAISSANCE_INSCRIPTION_RC, IndexerFormatHelper.dateCollectionToString(datesNaissanceInscriptionRC, IndexerFormatHelper.DateStringMode.INDEXATION));
		addAnalyzedValue(d, TiersIndexableData.SEXE, sexe);
		addNotAnalyzedValue(d, TiersIndexableData.NO_OFS_FOR_PRINCIPAL, noOfsForPrincipal);
		addNotAnalyzedValue(d, TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL, typeOfsForPrincipal);
		addNotAnalyzedValue(d, TiersIndexableData.NOS_OFS_AUTRES_FORS, nosOfsAutresFors);
		addNotAnalyzedValue(d, TiersIndexableData.MOTIF_FERMETURE_DERNIER_FOR_PRINCIPAL, IndexerFormatHelper.enumToString(motifFermetureDernierForPrincipal));
		addNotAnalyzedValue(d, TiersIndexableData.NPA_COURRIER, npaCourrier);
		addAnalyzedValue(d, TiersIndexableData.NPA_TOUS, npaTous);
		addAnalyzedValue(d, TiersIndexableData.LOCALITE_PAYS, localiteEtPays);
		addNotAnalyzedValue(d, TiersIndexableData.NATURE_JURIDIQUE, natureJuridique);
		addNotAnalyzedValue(d, TiersIndexableData.FORME_JURIDIQUE, formeJuridique);
		addNotAnalyzedValue(d, TiersIndexableData.CATEGORIE_ENTREPRISE, categorieEntreprise);
		addAnalyzedValue(d, TiersIndexableData.NAVS11, navs11);
		addAnalyzedValue(d, TiersIndexableData.NAVS13, navs13);
		addNotAnalyzedValue(d, TiersIndexableData.ANCIEN_NUMERO_SOURCIER, ancienNumeroSourcier);
		addNotAnalyzedValue(d, TiersIndexableData.CATEGORIE_DEBITEUR_IS, categorieDebiteurIs);
		addNotAnalyzedValue(d, TiersIndexableData.MODE_IMPOSITION, modeImposition);
		addNotAnalyzedValue(d, TiersIndexableData.NO_SYMIC, noSymic);
		addNotAnalyzedValue(d, TiersIndexableData.TIERS_ACTIF, tiersActif, BOOLEAN_RENDERER);
		addNotAnalyzedValue(d, TiersIndexableData.ANNULE, annule, BOOLEAN_RENDERER);
		addNotAnalyzedValue(d, TiersIndexableData.DEBITEUR_INACTIF, debiteurInactif, BOOLEAN_RENDERER);
		addAnalyzedValue(d, TiersIndexableData.IDE, ide);
		addNotAnalyzedValue(d, TiersIndexableData.ETAT_ENTREPRISE_COURANT, etatEntrepriseCourant, ENUM_RENDERER);
		addMultiValuedNotAnalyzedValue(d, TiersIndexableData.ETATS_ENTREPRISE, etatsEntreprise, ENUM_RENDERER);
		addNotAnalyzedValue(d, TiersIndexableData.INSCRIPTION_RC, etatInscriptionRC, ENUM_RENDERER);
		addNotAnalyzedValue(d, TiersIndexableData.CORPORATION_IS_MERGE_RESULT, corporationMergeResult, BOOLEAN_RENDERER);
		addNotAnalyzedValue(d, TiersIndexableData.CONNU_CIVIL, connuAuCivil, BOOLEAN_RENDERER);
		addNotAnalyzedValue(d, TiersIndexableData.CORPORATION_WAS_SPLIT, corporationSplit, BOOLEAN_RENDERER);
		addNotAnalyzedValue(d, TiersIndexableData.CORPORATION_TRANSFERED_PATRIMONY, corporationTransferedPatrimony, BOOLEAN_RENDERER);
		addMultiValuedNotAnalyzedValue(d, TiersIndexableData.TYPE_ETABLISSEMENT, typesEtablissement, ENUM_RENDERER);

		// on aggrège tous les valeurs utiles dans un seul champ pour une recherche de type google
		addToutValues(d, numeros, nomRaison, autresNom, toSearchString(datesNaissanceInscriptionRC), forPrincipal, rue, npaCourrier, localiteEtPays, natureJuridique, navs11, navs13, ancienNumeroSourcier, categorieDebiteurIs, noSymic, ide);

		// champs de stockage (pas recherchables)
		addStoredValue(d, TiersIndexableData.NOM1, nom1);
		addStoredValue(d, TiersIndexableData.NOM2, nom2);
		addStoredValue(d, TiersIndexableData.NAVS13_1, navs13_1);
		addStoredValue(d, TiersIndexableData.NAVS13_2, navs13_2);
		addStoredValue(d, TiersIndexableData.ROLE_LIGNE1, roleLigne1);
		addStoredValue(d, TiersIndexableData.ROLE_LIGNE2, roleLigne2);
		addStoredValue(d, TiersIndexableData.DATE_DECES, dateDeces);
		addStoredValue(d, TiersIndexableData.RUE, rue);
		addStoredValue(d, TiersIndexableData.LOCALITE, localite);
		addStoredValue(d, TiersIndexableData.PAYS, pays);
		addStoredValue(d, TiersIndexableData.FOR_PRINCIPAL, forPrincipal);
		addStoredValue(d, TiersIndexableData.DATE_OUVERTURE_FOR, dateOuvertureFor);
		addStoredValue(d, TiersIndexableData.DATE_FERMETURE_FOR, dateFermetureFor);
		addStoredValue(d, TiersIndexableData.DATE_OUVERTURE_FOR_VD, dateOuvertureForVd);
		addStoredValue(d, TiersIndexableData.DATE_FERMETURE_FOR_VD, dateFermetureForVd);
		addStoredValue(d, TiersIndexableData.DOMICILE_VD, domicileVd);
		addStoredValue(d, TiersIndexableData.NO_OFS_DOMICILE_VD, noOfsDomicileVd);
		addStoredValue(d, TiersIndexableData.INDEXATION_DATE, indexationDate);
		addStoredValue(d, TiersIndexableData.MODE_COMMUNICATION, modeCommunication, ENUM_RENDERER);
		addStoredValue(d, TiersIndexableData.D_DATE_NAISSANCE, IndexerFormatHelper.dateCollectionToString(datesNaissanceInscriptionRC, IndexerFormatHelper.DateStringMode.STORAGE));
		addStoredValue(d, TiersIndexableData.ASSUJETTISSEMENT_PP, assujettissementPP, ENUM_RENDERER);
		addStoredValue(d, TiersIndexableData.ASSUJETTISSEMENT_PM, assujettissementPM, ENUM_RENDERER);
		addStoredValue(d, TiersIndexableData.AVATAR, typeAvatar, IndexerFormatHelper.DEFAULT_RENDERER);
		addStoredValue(d, TiersIndexableData.DOMICILE_ETABLISSEMENT_PRINCIPAL, domicileEtablissementPrincipal);

		return d;
	}

	@Nullable
	private static String toSearchString(List<RegDate> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		for (RegDate date : list) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(RegDateHelper.dateToDisplayString(date));
		}
		return sb.toString().replaceAll("\\.", ""); // [SIFISC-6093] on supprime tous les points ('.') dans les strings de recherche.
	}

	private static final Pattern NULL_VALUE_PATTERN = Pattern.compile("\\b" + IndexerFormatHelper.nullValue() + "\\b");

	private static void addToutValues(Document d, String... values) {
		final StringBuilder sb = new StringBuilder();
		for (String value : values) {
			if (value != null) {
				final Matcher matcher = NULL_VALUE_PATTERN.matcher(value);
				value = matcher.replaceAll(StringUtils.EMPTY);
			}
			if (StringUtils.isNotBlank(value)) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(value);
			}
		}
		d.add(new TextField(TiersIndexableData.TOUT, sb.toString().replaceAll("\\s+", " "), Field.Store.YES));
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getSubType() {
		return subType;
	}

	public String getNumeros() {
		return numeros;
	}

	public void setNumeros(String numeros) {
		this.numeros = numeros;
	}

	public String getNomRaison() {
		return nomRaison;
	}

	public void setNomRaison(String nomRaison) {
		this.nomRaison = nomRaison;
	}

	public void addNomRaison(String nomRaison) {
		this.nomRaison = add(this.nomRaison, nomRaison);
	}

	public String getAutresNom() {
		return autresNom;
	}

	public void setAutresNom(String autresNom) {
		this.autresNom = autresNom;
	}

	public void addAutresNom(String autresNom) {
		this.autresNom = add(this.autresNom, autresNom);
	}

	public List<RegDate> getDatesNaissanceInscriptionRC() {
		return datesNaissanceInscriptionRC;
	}

	public void setDatesNaissanceInscriptionRC(List<RegDate> datesNaissanceInscriptionRC) {
		this.datesNaissanceInscriptionRC = datesNaissanceInscriptionRC;
	}

	public void addDateNaissance(RegDate date) {
		if (this.datesNaissanceInscriptionRC == null) {
			this.datesNaissanceInscriptionRC = new ArrayList<>();
		}
		this.datesNaissanceInscriptionRC.add(date);
	}

	public void addDatesNaissance(List<RegDate> list) {
		if (list != null && !list.isEmpty()) {
			if (this.datesNaissanceInscriptionRC == null) {
				this.datesNaissanceInscriptionRC = new ArrayList<>();
			}
			this.datesNaissanceInscriptionRC.addAll(list);
		}
	}

	public void setSexe(Sexe sexe) {
		this.sexe = IndexerFormatHelper.enumToString(sexe);
	}

	public void addSexe(Sexe sexe) {
		this.sexe = add(this.sexe, IndexerFormatHelper.enumToString(sexe));
	}

	public RegDate getDateInscriptionRc() {
		if (datesNaissanceInscriptionRC != null && ! datesNaissanceInscriptionRC.isEmpty()) {
			datesNaissanceInscriptionRC.get(0);
		}
		return null;
	}

	public String getNoOfsForPrincipal() {
		return noOfsForPrincipal;
	}

	public void setNoOfsForPrincipal(String noOfsForPrincipal) {
		this.noOfsForPrincipal = noOfsForPrincipal;
	}

	public String getTypeOfsForPrincipal() {
		return typeOfsForPrincipal;
	}

	public void setTypeOfsForPrincipal(String typeOfsForPrincipal) {
		this.typeOfsForPrincipal = typeOfsForPrincipal;
	}

	public String getNosOfsAutresFors() {
		return nosOfsAutresFors;
	}

	public void setNosOfsAutresFors(String nosOfsAutresFors) {
		this.nosOfsAutresFors = nosOfsAutresFors;
	}

	public MotifFor getMotifFermetureDernierForPrincipal() {
		return motifFermetureDernierForPrincipal;
	}

	public void setMotifFermetureDernierForPrincipal(MotifFor motifFermetureDernierForPrincipal) {
		this.motifFermetureDernierForPrincipal = motifFermetureDernierForPrincipal;
	}

	public String getNpaCourrier() {
		return npaCourrier;
	}

	public void setNpaCourrier(String npaCourrier) {
		this.npaCourrier = npaCourrier;
	}

	public String getNpaTous() {
		return npaTous;
	}

	public void setNpaTous(String npaTous) {
		this.npaTous = IndexerFormatHelper.nullableStringToString(npaTous);
	}

	public void addNpaTous(String npa) {
		this.npaTous = add(this.npaTous, npa);
	}

	public String getLocaliteEtPays() {
		return localiteEtPays;
	}

	public void setLocaliteEtPays(String localiteEtPays) {
		this.localiteEtPays = localiteEtPays;
	}

	public void addLocaliteEtPays(String localiteEtPays) {
		this.localiteEtPays = add(this.localiteEtPays, localiteEtPays);
	}

	public String getNatureJuridique() {
		return natureJuridique;
	}

	public void setNatureJuridique(String natureJuridique) {
		this.natureJuridique = natureJuridique;
	}

	public String getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(String formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	public String getCategorieEntreprise() {
		return categorieEntreprise;
	}

	public void setCategorieEntreprise(String categorieEntreprise) {
		this.categorieEntreprise = categorieEntreprise;
	}

	public String getNavs11() {
		return navs11;
	}

	public void setNavs11(String navs11) {
		this.navs11 = navs11;
	}
	
	public void addNavs11(String navs11) {
		this.navs11 = add(this.navs11, IndexerFormatHelper.nullableStringToString(navs11));
	}

	public String getNavs13() {
		return navs13;
	}

	public void setNavs13(String navs13) {
		this.navs13 = navs13;
	}
	
	public void addNavs13(String navs13) {
		this.navs13 = add(this.navs13, IndexerFormatHelper.nullableStringToString(navs13));
	}

	public String getAncienNumeroSourcier() {
		return ancienNumeroSourcier;
	}

	public void setAncienNumeroSourcier(String ancienNumeroSourcier) {
		this.ancienNumeroSourcier = ancienNumeroSourcier;
	}

	public String getCategorieDebiteurIs() {
		return categorieDebiteurIs;
	}

	public void setCategorieDebiteurIs(String categorieDebiteurIs) {
		this.categorieDebiteurIs = categorieDebiteurIs;
	}

	public String getModeImposition() {
		return modeImposition;
	}

	public void setModeImposition(String modeImposition) {
		this.modeImposition = modeImposition;
	}

	public String getNoSymic() {
		return noSymic;
	}

	public void setNoSymic(String noSymic) {
		this.noSymic = noSymic;
	}

	public Boolean getTiersActif() {
		return tiersActif;
	}

	public void setTiersActif(Boolean tiersActif) {
		this.tiersActif = tiersActif;
	}

	public Boolean getAnnule() {
		return annule;
	}

	public void setAnnule(Boolean annule) {
		this.annule = annule;
	}

	public Boolean getDebiteurInactif() {
		return debiteurInactif;
	}

	public void setDebiteurInactif(Boolean debiteurInactif) {
		this.debiteurInactif = debiteurInactif;
	}

	public String getNom1() {
		return nom1;
	}

	public void setNom1(String nom1) {
		this.nom1 = nom1;
	}

	public void addNom1(String nom1) {
		this.nom1 = add(this.nom1, nom1);
	}

	public String getNom2() {
		return nom2;
	}

	public void setNom2(String nom2) {
		this.nom2 = nom2;
	}

	public String getNavs13_1() {
		return navs13_1;
	}

	public void setNavs13_1(String navs13_1) {
		this.navs13_1 = navs13_1;
	}

	public String getNavs13_2() {
		return navs13_2;
	}

	public void setNavs13_2(String navs13_2) {
		this.navs13_2 = navs13_2;
	}

	public String getRoleLigne1() {
		return roleLigne1;
	}

	public void setRoleLigne1(String roleLigne1) {
		this.roleLigne1 = roleLigne1;
	}

	public String getRoleLigne2() {
		return roleLigne2;
	}

	public void setRoleLigne2(String roleLigne2) {
		this.roleLigne2 = roleLigne2;
	}

	public String getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(String dateDeces) {
		this.dateDeces = dateDeces;
	}

	public String getRue() {
		return rue;
	}

	public void setRue(String rue) {
		this.rue = rue;
	}

	public String getLocalite() {
		return localite;
	}

	public void setLocalite(String localite) {
		this.localite = localite;
	}

	public String getPays() {
		return pays;
	}

	public void setPays(String pays) {
		this.pays = pays;
	}

	public String getForPrincipal() {
		return forPrincipal;
	}

	public void setForPrincipal(String forPrincipal) {
		this.forPrincipal = forPrincipal;
	}

	public String getDateOuvertureFor() {
		return dateOuvertureFor;
	}

	public void setDateOuvertureFor(String dateOuvertureFor) {
		this.dateOuvertureFor = dateOuvertureFor;
	}

	public String getDateFermetureFor() {
		return dateFermetureFor;
	}

	public void setDateFermetureForVd(String dateFermetureForVd) {
		this.dateFermetureForVd = dateFermetureForVd;
	}

	public String getDateOuvertureForVd() {
		return dateOuvertureForVd;
	}

	public void setDateOuvertureForVd(String dateOuvertureForVd) {
		this.dateOuvertureForVd = dateOuvertureForVd;
	}

	public String getDateFermetureForVd() {
		return dateFermetureForVd;
	}

	public void setDateFermetureFor(String dateFermetureFor) {
		this.dateFermetureFor = dateFermetureFor;
	}

	public String getDomicileVd() {
		return domicileVd;
	}

	public void setDomicileVd(String domicileVd) {
		this.domicileVd = domicileVd;
	}

	public String getNoOfsDomicileVd() {
		return noOfsDomicileVd;
	}

	public void setNoOfsDomicileVd(String noOfsDomicileVd) {
		this.noOfsDomicileVd = noOfsDomicileVd;
	}

	public String getIndexationDate() {
		return indexationDate;
	}

	public void setIndexationDate(String indexationDate) {
		this.indexationDate = indexationDate;
	}

	public void setModeCommunication(ModeCommunication modeCommunication) {
		this.modeCommunication = modeCommunication;
	}

	public void setAssujettissementPP(TypeAssujettissement assujettissement) {
		this.assujettissementPP = assujettissement;
	}

	public void setAssujettissementPM(TypeAssujettissement assujettissement) {
		this.assujettissementPM = assujettissement;
	}

	public String getIde() {
		return ide;
	}

	public void setIde(String ide) {
		this.ide = ide;
	}

	public void addIde(String ide) {
		this.ide = add(this.ide, IndexerFormatHelper.noIdeToString(ide));
	}

	public TypeEtatEntreprise getEtatEntrepriseCourant() {
		return etatEntrepriseCourant;
	}

	public void setEtatEntrepriseCourant(TypeEtatEntreprise etatEntrepriseCourant) {
		this.etatEntrepriseCourant = etatEntrepriseCourant;
	}

	public Set<TypeEtatEntreprise> getEtatsEntreprise() {
		return etatsEntreprise;
	}

	public void addEtatEntreprise(TypeEtatEntreprise etat) {
		if (etatsEntreprise == null) {
			etatsEntreprise = EnumSet.noneOf(TypeEtatEntreprise.class);
		}
		etatsEntreprise.add(etat);
	}

	public TypeEtatInscriptionRC getEtatInscriptionRC() {
		return etatInscriptionRC;
	}

	public void setEtatInscriptionRC(TypeEtatInscriptionRC etat) {
		etatInscriptionRC = etat;
	}

	public String getDomicileEtablissementPrincipal() {
		return domicileEtablissementPrincipal;
	}

	public void setDomicileEtablissementPrincipal(String domicileEtablissementPrincipal) {
		this.domicileEtablissementPrincipal = domicileEtablissementPrincipal;
	}

	private static String add(String left, String right) {
		if (StringUtils.isBlank(left)) {
			return StringUtils.trimToEmpty(right);
		}
		else if (StringUtils.isBlank(right)) {
			return left.trim();
		}
		else {
			return String.format("%s %s", left.trim(), right.trim());
		}
	}

	public void setTypeAvatar(TypeAvatar typeAvatar) {
		this.typeAvatar = typeAvatar;
	}

	public Boolean getCorporationMergeResult() {
		return corporationMergeResult;
	}

	public void setCorporationMergeResult(Boolean corporationMergeResult) {
		this.corporationMergeResult = corporationMergeResult;
	}

	public Boolean getCorporationSplit() {
		return corporationSplit;
	}

	public void setCorporationSplit(Boolean corporationSplit) {
		this.corporationSplit = corporationSplit;
	}

	public Boolean getCorporationTransferedPatrimony() {
		return corporationTransferedPatrimony;
	}

	public void setCorporationTransferedPatrimony(Boolean corporationTransferedPatrimony) {
		this.corporationTransferedPatrimony = corporationTransferedPatrimony;
	}

	public Boolean getConnuAuCivil() {
		return connuAuCivil;
	}

	public void setConnuAuCivil(Boolean connuAuCivil) {
		this.connuAuCivil = connuAuCivil;
	}

	public Set<TypeEtablissement> getTypesEtablissement() {
		return typesEtablissement;
	}

	public void addTypeEtablissement(TypeEtablissement type) {
		if (typesEtablissement == null) {
			typesEtablissement = EnumSet.noneOf(TypeEtablissement.class);
		}
		typesEtablissement.add(type);
	}
}
