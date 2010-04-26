package ch.vd.uniregctb.indexer.tiers;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import ch.vd.uniregctb.indexer.IndexableData;

@SuppressWarnings({"UnusedDeclaration"})
public class TiersIndexableData extends IndexableData {

	private String subType;

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
	private String domicileVd;
	private String noOfsDomicileVd;
	private String indexationDate;

	public TiersIndexableData() {
		
	}

	/**
	 * Constructeur temporaire utilisé pendant la reécriture des indexables
	 */
	public TiersIndexableData(TiersIndexable indexable) {
		super(indexable.getID(), indexable.getType(), indexable.getSubType());

		final HashMap<String, String> kv = indexable.getKeyValues();

		this.numeros = kv.get(TiersSearchFields.NUMEROS);
		this.nomRaison = kv.get(TiersSearchFields.NOM_RAISON);
		this.autresNom = kv.get(TiersSearchFields.AUTRES_NOM);
		this.dateNaissance = kv.get(TiersSearchFields.DATE_NAISSANCE);
		this.noOfsForPrincipal = kv.get(TiersSearchFields.NO_OFS_FOR_PRINCIPAL);
		this.typeOfsForPrincipal = kv.get(TiersSearchFields.TYPE_OFS_FOR_PRINCIPAL);
		this.nosOfsAutresFors = kv.get(TiersSearchFields.NOS_OFS_AUTRES_FORS);
		this.npa = kv.get(TiersSearchFields.NPA);
		this.localiteEtPays = kv.get(TiersSearchFields.LOCALITE_PAYS);
		this.natureJuridique = kv.get(TiersSearchFields.NATURE_JURIDIQUE);
		this.numeroAssureSocial = kv.get(TiersSearchFields.NUMERO_ASSURE_SOCIAL);
		this.categorieDebiteurIs = kv.get(TiersSearchFields.CATEGORIE_DEBITEUR_IS);
		this.modeImposition = kv.get(TiersSearchFields.MODE_IMPOSITION);
		this.noSymic = kv.get(TiersSearchFields.NO_SYMIC);
		this.tiersActif = kv.get(TiersSearchFields.TIERS_ACTIF);
		this.annule = kv.get(TiersSearchFields.ANNULE);
		this.debiteurInactif = kv.get(TiersSearchFields.DEBITEUR_INACTIF);

		this.nom1 = kv.get(TiersIndexedData.NOM1);
		this.nom2 = kv.get(TiersIndexedData.NOM2);
		this.roleLigne1 = kv.get(TiersIndexedData.ROLE_LIGNE1);
		this.roleLigne2 = kv.get(TiersIndexedData.ROLE_LIGNE2);
		this.dateDeces = kv.get(TiersIndexedData.DATE_DECES);
		this.rue = kv.get(TiersIndexedData.RUE);
		this.localite = kv.get(TiersIndexedData.LOCALITE);
		this.pays = kv.get(TiersIndexedData.PAYS);
		this.forPrincipal = kv.get(TiersIndexedData.FOR_PRINCIPAL);
		this.domicileVd = kv.get(TiersIndexedData.DOMICILE_VD);
		this.noOfsDomicileVd = kv.get(TiersIndexedData.NO_OFS_DOMICILE_VD);
		this.indexationDate = kv.get(TiersIndexedData.INDEXATION_DATE);
	}

	public Document asDoc() {

		Document d = super.asDoc();

		// Note : pour des raisons de performance de l'index Lucene, il est important que l'ordre des champs soit constant

		// champs de recherche
		addAnalyzedValue(d, TiersSearchFields.NUMEROS, numeros);
		addAnalyzedValue(d, TiersSearchFields.NOM_RAISON, nomRaison);
		addAnalyzedValue(d, TiersSearchFields.AUTRES_NOM, autresNom);
		addAnalyzedValue(d, TiersSearchFields.DATE_NAISSANCE, dateNaissance);
		addAnalyzedValue(d, TiersSearchFields.NO_OFS_FOR_PRINCIPAL, noOfsForPrincipal);
		addAnalyzedValue(d, TiersSearchFields.TYPE_OFS_FOR_PRINCIPAL, typeOfsForPrincipal);
		addAnalyzedValue(d, TiersSearchFields.NOS_OFS_AUTRES_FORS, nosOfsAutresFors);
		addAnalyzedValue(d, TiersSearchFields.NPA, npa);
		addAnalyzedValue(d, TiersSearchFields.LOCALITE_PAYS, localiteEtPays);
		addAnalyzedValue(d, TiersSearchFields.NATURE_JURIDIQUE, natureJuridique);
		addAnalyzedValue(d, TiersSearchFields.NUMERO_ASSURE_SOCIAL, numeroAssureSocial);
		addAnalyzedValue(d, TiersSearchFields.CATEGORIE_DEBITEUR_IS, categorieDebiteurIs);
		addAnalyzedValue(d, TiersSearchFields.MODE_IMPOSITION, modeImposition);
		addAnalyzedValue(d, TiersSearchFields.NO_SYMIC, noSymic);
		addAnalyzedValue(d, TiersSearchFields.TIERS_ACTIF, tiersActif);
		addAnalyzedValue(d, TiersSearchFields.ANNULE, annule);
		addAnalyzedValue(d, TiersSearchFields.DEBITEUR_INACTIF, debiteurInactif);

		// champs de stockage (pas recherchables)
		addStockedValue(d, TiersIndexedData.NOM1, nom1);
		addStockedValue(d, TiersIndexedData.NOM2, nom2);
		addStockedValue(d, TiersIndexedData.ROLE_LIGNE1, roleLigne1);
		addStockedValue(d, TiersIndexedData.ROLE_LIGNE2, roleLigne2);
		addStockedValue(d, TiersIndexedData.DATE_DECES, dateDeces);
		addStockedValue(d, TiersIndexedData.RUE, rue);
		addStockedValue(d, TiersIndexedData.LOCALITE, localite);
		addStockedValue(d, TiersIndexedData.PAYS, pays);
		addStockedValue(d, TiersIndexedData.FOR_PRINCIPAL, forPrincipal);
		addStockedValue(d, TiersIndexedData.DOMICILE_VD, domicileVd);
		addStockedValue(d, TiersIndexedData.NO_OFS_DOMICILE_VD, noOfsDomicileVd);
		addStockedValue(d, TiersIndexedData.INDEXATION_DATE, indexationDate);

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

	public void setType(String type) {
		this.type = type;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
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

	public String getAutresNom() {
		return autresNom;
	}

	public void setAutresNom(String autresNom) {
		this.autresNom = autresNom;
	}

	public String getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(String dateNaissance) {
		this.dateNaissance = dateNaissance;
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

	public String getNatureJuridique() {
		return natureJuridique;
	}

	public void setNatureJuridique(String natureJuridique) {
		this.natureJuridique = natureJuridique;
	}

	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public void setNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = numeroAssureSocial;
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
}
