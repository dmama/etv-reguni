package ch.vd.uniregctb.indexer.tiers;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import ch.vd.uniregctb.indexer.IndexableData;

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
	public static final String NPA = "S_NPA";
	public static final String NATURE_JURIDIQUE = "S_NATURE_JURIDIQUE"; // (PP ou PM)
	public static final String DATE_NAISSANCE = "S_DATE_NAISSANCE";
	public static final String NUMERO_ASSURE_SOCIAL = "S_NUMERO_ASSURE_SOCIAL";
	public static final String ANNULE = "S_ANNULE";
	public static final String DEBITEUR_INACTIF = "S_DEBITEUR_INACTIF";
	public static final String CATEGORIE_DEBITEUR_IS = "S_CATEGORIE_DEBITEUR_IS";
	public static final String MODE_IMPOSITION = "S_MODE_IMPOSITION";
	public static final String NO_SYMIC = "S_NO_SYMIC";
	public static final String TIERS_ACTIF = "S_TIERS_ACTIF";

	// champs de stockage (pas recherchables)
	public static final String NOM1 = "D_NOM1";
	public static final String NOM2 = "D_NOM2";
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
	
	// champs de recherche
	private String numeros;
	private String nomRaison;
	private String autresNom;
	private String dateNaissance;
	private String noOfsForPrincipal;
	private String typeOfsForPrincipal;
	private String nosOfsAutresFors;
	private String npa;
	private String localiteEtPays;
	private String natureJuridique;
	private String numeroAssureSocial;
	private String categorieDebiteurIs;
	private String modeImposition;
	private String noSymic;
	private String tiersActif;
	private String annule;
	private String debiteurInactif;

	// champs de stockage (pas recherchables)
	private String nom1;
	private String nom2;
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

	public TiersIndexableData(Long id, String type, String subType) {
		super(id, type, subType);
	}

	public Document asDoc() {

		Document d = super.asDoc();

		// Note : pour des raisons de performance de l'index Lucene, il est important que l'ordre des champs soit constant

		// champs de recherche
		addAnalyzedValue(d, TiersIndexableData.NUMEROS, numeros);
		addAnalyzedValue(d, TiersIndexableData.NOM_RAISON, nomRaison);
		addAnalyzedValue(d, TiersIndexableData.AUTRES_NOM, autresNom);
		addAnalyzedValue(d, TiersIndexableData.DATE_NAISSANCE, dateNaissance);
		addAnalyzedValue(d, TiersIndexableData.NO_OFS_FOR_PRINCIPAL, noOfsForPrincipal);
		addAnalyzedValue(d, TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL, typeOfsForPrincipal);
		addAnalyzedValue(d, TiersIndexableData.NOS_OFS_AUTRES_FORS, nosOfsAutresFors);
		addAnalyzedValue(d, TiersIndexableData.NPA, npa);
		addAnalyzedValue(d, TiersIndexableData.LOCALITE_PAYS, localiteEtPays);
		addAnalyzedValue(d, TiersIndexableData.NATURE_JURIDIQUE, natureJuridique);
		addAnalyzedValue(d, TiersIndexableData.NUMERO_ASSURE_SOCIAL, numeroAssureSocial);
		addAnalyzedValue(d, TiersIndexableData.CATEGORIE_DEBITEUR_IS, categorieDebiteurIs);
		addAnalyzedValue(d, TiersIndexableData.MODE_IMPOSITION, modeImposition);
		addAnalyzedValue(d, TiersIndexableData.NO_SYMIC, noSymic);
		addAnalyzedValue(d, TiersIndexableData.TIERS_ACTIF, tiersActif);
		addAnalyzedValue(d, TiersIndexableData.ANNULE, annule);
		addAnalyzedValue(d, TiersIndexableData.DEBITEUR_INACTIF, debiteurInactif);

		// champs de stockage (pas recherchables)
		addStockedValue(d, TiersIndexableData.NOM1, nom1);
		addStockedValue(d, TiersIndexableData.NOM2, nom2);
		addStockedValue(d, TiersIndexableData.ROLE_LIGNE1, roleLigne1);
		addStockedValue(d, TiersIndexableData.ROLE_LIGNE2, roleLigne2);
		addStockedValue(d, TiersIndexableData.DATE_DECES, dateDeces);
		addStockedValue(d, TiersIndexableData.RUE, rue);
		addStockedValue(d, TiersIndexableData.LOCALITE, localite);
		addStockedValue(d, TiersIndexableData.PAYS, pays);
		addStockedValue(d, TiersIndexableData.FOR_PRINCIPAL, forPrincipal);
		addStockedValue(d, TiersIndexableData.DATE_OUVERTURE_FOR, dateOuvertureFor);
		addStockedValue(d, TiersIndexableData.DATE_FERMETURE_FOR, dateFermtureFor);
		addStockedValue(d, TiersIndexableData.DOMICILE_VD, domicileVd);
		addStockedValue(d, TiersIndexableData.NO_OFS_DOMICILE_VD, noOfsDomicileVd);
		addStockedValue(d, TiersIndexableData.INDEXATION_DATE, indexationDate);

		return d;
	}

	private void addStockedValue(Document d, String name, String value) {
		d.add(new Field(name, toString(value), Field.Store.YES, Field.Index.NO));
	}

	private void addAnalyzedValue(Document d, String name, String value) {
		d.add(new Field(name, toString(value), Field.Store.YES, Field.Index.ANALYZED));
	}

	private String toString(String value) {
		return value == null ? "" : value;
	}

	public String getType() {
		return type;
	}

	public String getSubType() {
		return subType;
	}

	public String getNumeros() {
		return numeros;
	}

	public void setNumeros(String numeros) {
		this.numeros = numeros;
	}

	public void addNumeros(String numeros) {
		this.numeros = add(this.numeros, numeros);
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

	public String getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(String dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public void addDateNaissance(String dateNaissance) {
		this.dateNaissance = add(this.dateNaissance, dateNaissance);
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

	public String getNpa() {
		return npa;
	}

	public void setNpa(String npa) {
		this.npa = npa;
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

	public void addNatureJuridique(String natureJuridique) {
		this.natureJuridique = add(this.natureJuridique, natureJuridique);
	}

	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public void setNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = numeroAssureSocial;
	}

	public void addNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = add(this.numeroAssureSocial, numeroAssureSocial);
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
