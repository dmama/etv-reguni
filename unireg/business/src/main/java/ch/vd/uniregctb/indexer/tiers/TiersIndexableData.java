package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.indexer.IndexableData;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;

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
	public static final String ANCIEN_NUMERO_SOURCIER = "S_ANCIEN_NUMERO_SOURCIER";
	public static final String ANNULE = "S_ANNULE";
	public static final String DEBITEUR_INACTIF = "S_DEBITEUR_INACTIF";
	public static final String CATEGORIE_DEBITEUR_IS = "S_CATEGORIE_DEBITEUR_IS";
	public static final String MODE_IMPOSITION = "S_MODE_IMPOSITION";
	public static final String NO_SYMIC = "S_NO_SYMIC";
	public static final String TIERS_ACTIF = "S_TIERS_ACTIF";
	public static final String TOUT = "S_TOUT";

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
	private List<RegDate> datesNaissance;
	private String noOfsForPrincipal;
	private String typeOfsForPrincipal;
	private String nosOfsAutresFors;
	private String npa;
	private String localiteEtPays;
	private String natureJuridique;
	private String numeroAssureSocial;
	private String ancienNumeroSourcier;
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

	@Override
	public Document asDoc() {

		Document d = super.asDoc();

		// Note : pour des raisons de performance de l'index Lucene, il est important que l'ordre des champs soit constant

		// champs de recherche
		addNotAnalyzedValue(d, TiersIndexableData.NUMEROS, numeros);
		addAnalyzedValue(d, TiersIndexableData.NOM_RAISON, nomRaison);
		addAnalyzedValue(d, TiersIndexableData.AUTRES_NOM, autresNom);
		addAnalyzedValue(d, TiersIndexableData.DATE_NAISSANCE, IndexerFormatHelper.objectToString(datesNaissance));
		addNotAnalyzedValue(d, TiersIndexableData.NO_OFS_FOR_PRINCIPAL, noOfsForPrincipal);
		addNotAnalyzedValue(d, TiersIndexableData.TYPE_OFS_FOR_PRINCIPAL, typeOfsForPrincipal);
		addNotAnalyzedValue(d, TiersIndexableData.NOS_OFS_AUTRES_FORS, nosOfsAutresFors);
		addNotAnalyzedValue(d, TiersIndexableData.NPA, npa);
		addAnalyzedValue(d, TiersIndexableData.LOCALITE_PAYS, localiteEtPays);
		addNotAnalyzedValue(d, TiersIndexableData.NATURE_JURIDIQUE, natureJuridique);
		addAnalyzedValue(d, TiersIndexableData.NUMERO_ASSURE_SOCIAL, numeroAssureSocial);
		addNotAnalyzedValue(d, TiersIndexableData.ANCIEN_NUMERO_SOURCIER, ancienNumeroSourcier);
		addNotAnalyzedValue(d, TiersIndexableData.CATEGORIE_DEBITEUR_IS, categorieDebiteurIs);
		addNotAnalyzedValue(d, TiersIndexableData.MODE_IMPOSITION, modeImposition);
		addNotAnalyzedValue(d, TiersIndexableData.NO_SYMIC, noSymic);
		addNotAnalyzedValue(d, TiersIndexableData.TIERS_ACTIF, tiersActif);
		addNotAnalyzedValue(d, TiersIndexableData.ANNULE, annule);
		addNotAnalyzedValue(d, TiersIndexableData.DEBITEUR_INACTIF, debiteurInactif);

		// on aggrège tous les valeurs utiles dans un seul champ pour une recherche de type google
		addToutValues(d, numeros, nomRaison, autresNom, toSearchString(datesNaissance), forPrincipal, rue, npa, localiteEtPays, natureJuridique, numeroAssureSocial, ancienNumeroSourcier, categorieDebiteurIs, noSymic);

		// champs de stockage (pas recherchables)
		addStoredValue(d, TiersIndexableData.NOM1, nom1);
		addStoredValue(d, TiersIndexableData.NOM2, nom2);
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
		d.add(new Field(name, toString(value), Field.Store.YES, Field.Index.NO));
	}

	private void addAnalyzedValue(Document d, String name, String value) {
		d.add(new Field(name, toString(value), Field.Store.YES, Field.Index.ANALYZED));
	}

	private void addNotAnalyzedValue(Document d, String name, String value) {
		d.add(new Field(name, toString(value), Field.Store.YES, Field.Index.NOT_ANALYZED));
	}

	private void addToutValues(Document d, String... values) {
		final StringBuilder sb = new StringBuilder();
		for (String value : values) {
			if (StringUtils.isNotBlank(value)) {
				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append(value);
			}
		}
		d.add(new Field(TiersIndexableData.TOUT, sb.toString(), Field.Store.YES, Field.Index.ANALYZED));
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
		if (date != null) {
			if (this.datesNaissance == null) {
				this.datesNaissance = new ArrayList<RegDate>();
			}
			this.datesNaissance.add(date);
		}
	}

	public void addDatesNaissance(List<RegDate> list) {
		if (list != null && !list.isEmpty()) {
			if (this.datesNaissance == null) {
				this.datesNaissance = new ArrayList<RegDate>();
			}
			this.datesNaissance.addAll(list);
		}
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

	public String getNumeroAssureSocial() {
		return numeroAssureSocial;
	}

	public void setNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = numeroAssureSocial;
	}

	public void addNumeroAssureSocial(String numeroAssureSocial) {
		this.numeroAssureSocial = add(this.numeroAssureSocial, numeroAssureSocial);
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
