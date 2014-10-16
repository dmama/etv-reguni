package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.Sexe;

@SuppressWarnings({"UnusedDeclaration"})
public class TiersIndexableData extends IndexableData {

	// champs de recherche
	public static final String NUMEROS = "S_NUMEROS";
	public static final String NOM_RAISON = "S_NOM_RAISON";
	public static final String AUTRES_NOM = "S_AUTRES_NOM";
	public static final String NO_OFS_FOR_PRINCIPAL = "S_NO_OFS_FOR_PRINCIPAL";
	public static final String TYPE_OFS_FOR_PRINCIPAL = "S_TYPE_OFS_FOR_PRINCIPAL";
	public static final String NOS_OFS_AUTRES_FORS = "S_NOS_OFS_AUTRES_FORS";
	public static final String LOCALITE_PAYS = "S_LOCALITE_PAYS";
	public static final String NPA_COURRIER = "S_NPA_COURRIER";
	public static final String NPA_TOUS = "S_NPA_TOUS";
	public static final String NATURE_JURIDIQUE = "S_NATURE_JURIDIQUE"; // (PP ou PM)
	public static final String S_DATE_NAISSANCE = "S_DATE_NAISSANCE";
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
	public static final String DOMICILE_VD = "D_DOMICILE_VD";
	public static final String NO_OFS_DOMICILE_VD = "D_NO_OFS_DOMICILE_VD";
	public static final String INDEXATION_DATE = "D_INDEXATION_DATE";
	public static final String MODE_COMMUNICATION = "D_MODE_COMMUNICATION";
	public static final String D_DATE_NAISSANCE = "D_DATE_NAISSANCE";
	public static final String ASSUJETTISSEMENT_PP = "D_ASSUJETTISSEMENT_PP";

	// champs de recherche
	private String numeros;
	private String nomRaison;
	private String autresNom;
	private List<RegDate> datesNaissance;       // valeurs utilisées pour la recherche (calculées à partir de la date connue)
	private String sexe;
	private String noOfsForPrincipal;
	private String typeOfsForPrincipal;
	private String nosOfsAutresFors;
	private String npaCourrier;
	private String npaTous;
	private String localiteEtPays;
	private String natureJuridique;
	private String navs11;
	private String navs13;
	private String ancienNumeroSourcier;
	private String categorieDebiteurIs;
	private String modeImposition;
	private String noSymic;
	private String tiersActif;
	private String annule;
	private String debiteurInactif;
	private String ide;                     // identifiant d'entreprise [SIFISC-11689]

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
	private String dateFermtureFor;
	private String domicileVd;
	private String noOfsDomicileVd;
	private String indexationDate;
	private String modeCommunication;   // uniquement renseigné sur les débiteurs (SIFISC-6587)
	private String assujettissementPP;  // seulement sur les PP/MC (SIFISC-11102)

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
		addAnalyzedValue(d, TiersIndexableData.S_DATE_NAISSANCE, IndexerFormatHelper.dateCollectionToString(datesNaissance, IndexerFormatHelper.DateStringMode.INDEXATION));
		addAnalyzedValue(d, TiersIndexableData.SEXE, sexe);
		addNotAnalyzedValue(d, TiersIndexableData.NO_OFS_FOR_PRINCIPAL, noOfsForPrincipal);
		addNotAnalyzedValue(d, TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL, typeOfsForPrincipal);
		addNotAnalyzedValue(d, TiersIndexableData.NOS_OFS_AUTRES_FORS, nosOfsAutresFors);
		addNotAnalyzedValue(d, TiersIndexableData.NPA_COURRIER, npaCourrier);
		addAnalyzedValue(d, TiersIndexableData.NPA_TOUS, npaTous);
		addAnalyzedValue(d, TiersIndexableData.LOCALITE_PAYS, localiteEtPays);
		addNotAnalyzedValue(d, TiersIndexableData.NATURE_JURIDIQUE, natureJuridique);
		addAnalyzedValue(d, TiersIndexableData.NAVS11, navs11);
		addAnalyzedValue(d, TiersIndexableData.NAVS13, navs13);
		addNotAnalyzedValue(d, TiersIndexableData.ANCIEN_NUMERO_SOURCIER, ancienNumeroSourcier);
		addNotAnalyzedValue(d, TiersIndexableData.CATEGORIE_DEBITEUR_IS, categorieDebiteurIs);
		addNotAnalyzedValue(d, TiersIndexableData.MODE_IMPOSITION, modeImposition);
		addNotAnalyzedValue(d, TiersIndexableData.NO_SYMIC, noSymic);
		addNotAnalyzedValue(d, TiersIndexableData.TIERS_ACTIF, tiersActif);
		addNotAnalyzedValue(d, TiersIndexableData.ANNULE, annule);
		addNotAnalyzedValue(d, TiersIndexableData.DEBITEUR_INACTIF, debiteurInactif);
		addAnalyzedValue(d, TiersIndexableData.IDE, ide);

		// on aggrège tous les valeurs utiles dans un seul champ pour une recherche de type google
		addToutValues(d, numeros, nomRaison, autresNom, toSearchString(datesNaissance), forPrincipal, rue, npaCourrier, localiteEtPays, natureJuridique, navs11, navs13, ancienNumeroSourcier, categorieDebiteurIs, noSymic, ide);

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
		addStoredValue(d, TiersIndexableData.DATE_FERMETURE_FOR, dateFermtureFor);
		addStoredValue(d, TiersIndexableData.DOMICILE_VD, domicileVd);
		addStoredValue(d, TiersIndexableData.NO_OFS_DOMICILE_VD, noOfsDomicileVd);
		addStoredValue(d, TiersIndexableData.INDEXATION_DATE, indexationDate);
		addStoredValue(d, TiersIndexableData.MODE_COMMUNICATION, modeCommunication);
		addStoredValue(d, TiersIndexableData.D_DATE_NAISSANCE, IndexerFormatHelper.dateCollectionToString(datesNaissance, IndexerFormatHelper.DateStringMode.STORAGE));
		addStoredValue(d, TiersIndexableData.ASSUJETTISSEMENT_PP, assujettissementPP);

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

	private void addStoredValue(Document d, String name, String value) {
		d.add(new StoredField(name, toString(value)));
	}

	private void addAnalyzedValue(Document d, String name, String value) {
		d.add(new TextField(name, toString(value), Field.Store.YES));
	}

	private void addNotAnalyzedValue(Document d, String name, String value) {
		d.add(new StringField(name, toString(value), Field.Store.YES));
	}

	private static final Pattern NULL_VALUE_PATTERN = Pattern.compile("\\b" + IndexerFormatHelper.nullValue() + "\\b");

	private void addToutValues(Document d, String... values) {
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

	private String toString(String value) {
		return value == null ? "" : value;
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

	public List<RegDate> getDatesNaissance() {
		return datesNaissance;
	}

	public void setDatesNaissance(List<RegDate> datesNaissance) {
		this.datesNaissance = datesNaissance;
	}

	public void addDateNaissance(RegDate date) {
		if (this.datesNaissance == null) {
			this.datesNaissance = new ArrayList<>();
		}
		this.datesNaissance.add(date);
	}

	public void addDatesNaissance(List<RegDate> list) {
		if (list != null && !list.isEmpty()) {
			if (this.datesNaissance == null) {
				this.datesNaissance = new ArrayList<>();
			}
			this.datesNaissance.addAll(list);
		}
	}

	public void setSexe(Sexe sexe) {
		this.sexe = IndexerFormatHelper.enumToString(sexe);
	}

	public void addSexe(Sexe sexe) {
		this.sexe = add(this.sexe, IndexerFormatHelper.enumToString(sexe));
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

	public String getTiersActif() {
		return tiersActif;
	}

	public void setTiersActif(String tiersActif) {
		this.tiersActif = tiersActif;
	}

	public String getAnnule() {
		return annule;
	}

	public void setAnnule(String annule) {
		this.annule = annule;
	}

	public String getDebiteurInactif() {
		return debiteurInactif;
	}

	public void setDebiteurInactif(String debiteurInactif) {
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

	public String getDateFermtureFor() {
		return dateFermtureFor;
	}

	public void setDateFermtureFor(String dateFermtureFor) {
		this.dateFermtureFor = dateFermtureFor;
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
		this.modeCommunication = (modeCommunication == null ? StringUtils.EMPTY : modeCommunication.name());
	}

	public void setAssujettissementPP(TypeAssujettissement assujettissement) {
		this.assujettissementPP = (assujettissement == null ? StringUtils.EMPTY : assujettissement.name());
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
}
