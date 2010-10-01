package ch.vd.uniregctb.tiers;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeImposition;

/**
 * Critères de recherche pour les tiers.
 */
public class TiersCriteria implements Serializable {

	private static final long serialVersionUID = 4798896788504617011L;

	public enum TypeRecherche {
		CONTIENT,
		PHONETIQUE,
		EST_EXACTEMENT;

	    public static TypeRecherche fromValue(String v) {
	        return valueOf(v);
	    }
	}

	public enum TypeRechercheLocalitePays {
		ALL,
		PAYS,
		LOCALITE
	}

	public enum TypeRechercheForFiscal {
		ALL,
		COMMUNE_OU_FRACTION_VD,
		COMMUNE_HC,
		PAYS_HS
	}

	public enum TypeTiers {
		TIERS,
		CONTRIBUABLE,
		DEBITEUR_PRESTATION_IMPOSABLE,
		PERSONNE_PHYSIQUE,
		NON_HABITANT,
		HABITANT,
		MENAGE_COMMUN,
		ENTREPRISE,
		ETABLISSEMENT,
		COLLECTIVITE_ADMINISTRATIVE,
		AUTRE_COMMUNAUTE,
		NON_HABITANT_OU_MENAGE_COMMUN,
		CONTRIBUABLE_PP
	}

	public enum TypeVisualisation {
		LIMITEE,
		COMPLETE
	}
	/**
	 * Le numero de contribuable.
	 */
	private Long numero;

	/**
	 * Le type du tiers
	 */
	private TypeTiers typeTiers;

	/**
	 * Le type de recherche du nom
	 */
	private TypeRecherche typeRechercheDuNom = TypeRecherche.EST_EXACTEMENT;

	/**
	 * Le type de recherche par pays ou localite
	 */
	private TypeRechercheLocalitePays typeRechercheDuPaysLocalite = TypeRechercheLocalitePays.ALL;

	/**
	 * le type de visualisation
	 */
	private TypeVisualisation typeVisualisation = TypeVisualisation.COMPLETE;

	/**
	 * Le nom courrier
	 */
	private String nomRaison;

	/**
	 * La localite ou le pays
	 */
	private String localiteOuPays;
	/**
	 * Le NPA pour une adresse en suisse
	 */
	private String npa;

	/**
	 * La nature juridique
	 */
	private String natureJuridique;

	/**
	 * La date de naissance
	 */
	private SerializableDate dateNaissance;

	/**
	 * Le numéro AVS
	 */
	private String numeroAVS;

	/**
	 * Le numéro étranger
	 */
	private String numeroEtranger;

	/**
	 * La forme juridique
	 */
	private String formeJuridique;

	/**
	 * Numéro OFS du for
	 */
	private String noOfsFor;

	/**
	 * For principal actif uniquement
	 */
	private boolean forPrincipalActif;

	/**
	 * Mode imposition
	 */
	private ModeImposition modeImposition;

	/**
	 * Numero SYMIC
	 */
	private String noSymic;


	/**
	 * Origine I107
	 */
	private boolean inclureI107;

	/**
	 * Tiers annules
	 */
	private boolean inclureTiersAnnules;

	/**
	 * Tiers annulés seulement
	 */
	private boolean tiersAnnulesSeulement;

	/**
	 * La catégorie de débiteur impôt source (uniquement valable pour les débiteurs de prestations imposables)
	 */
	private CategorieImpotSource categorieDebiteurIs;

	/**
	 * Vrai si le tiers est actif, c'est-à-dire qu'il possède un for principal ouvert à la date d'indexation.
	 */
	private Boolean tiersActif;

	private static class SerializableDate implements Serializable {

		private static final long serialVersionUID = 1L;

		private int year;
		private int month;
		private int day;

		SerializableDate() {
		}

		SerializableDate(RegDate date) {
			this.year = date.year();
			this.month = date.month();
			this.day = date.day();
		}

		private RegDate asRegDate() {
			if (month == RegDate.UNDEFINED) {
				return RegDate.get(year);
			}
			else if (day == RegDate.UNDEFINED) {
				return RegDate.get(year, month);
			}
			else {
				return RegDate.get(year, month, day);
			}
		}
	}

	/**
	 * @return true si aucun paramétre de recherche n'est renseigné. false
	 *         autrement.
	 */
	public boolean isEmpty() {
		return (numero == null)
				&& (nomRaison == null || "".equals(nomRaison))
				&& (dateNaissance == null)
				&& (numeroEtranger == null || "".equals(numeroEtranger))
				&& (numeroAVS == null || "".equals(numeroAVS))
				&& (localiteOuPays == null || "".equals(localiteOuPays))
				&& (npa == null || "".equals(npa))
				&& (natureJuridique == null || "".equals(natureJuridique))
				&& (noOfsFor == null || "".equals(noOfsFor))
				&& (noSymic == null || "".equals(noSymic))
				&& (formeJuridique == null || "".equals(formeJuridique))
				&& (modeImposition == null)
				&& (categorieDebiteurIs == null)
				&& (tiersActif == null);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	/**
	 * @return the numero
	 */
	public Long getNumero() {
		return numero;
	}

	/**
	 * @param numero
	 *            the numero to set
	 */
	public void setNumero(Long numero) {
		this.numero = numero;
	}


	/**
	 * @return the typeRechercheDuNom
	 */
	public TypeRecherche getTypeRechercheDuNom() {
		return typeRechercheDuNom;
	}

	/**
	 * @param typeRechercheDuNom
	 *            the typeRechercheDuNom to set
	 */
	public void setTypeRechercheDuNom(TypeRecherche typeRechercheDuNom) {
		this.typeRechercheDuNom = typeRechercheDuNom;
	}

	/**
	 * @return the nomCourrier
	 */
	public String getNomRaison() {
		return nomRaison;
	}

	/**
	 * @param nomCourrier
	 *            the nomCourrier to set
	 */
	public void setNomRaison(String nomCourrier) {
		this.nomRaison = nomCourrier;
	}

	/**
	 * @return the natureJuridique
	 */
	public String getNatureJuridique() {
		return natureJuridique;
	}

	/**
	 * @param natureJuridique
	 *            the natureJuridique to set
	 */
	public void setNatureJuridique(String natureJuridique) {
		this.natureJuridique = natureJuridique;
	}


	/**
	 * @return the dateNaissance
	 */
	public RegDate getDateNaissance() {
		return dateNaissance == null ? null : dateNaissance.asRegDate();
	}

	/**
	 * @param dateNaissance
	 *            the dateNaissance to set
	 */
	public void setDateNaissance(RegDate dateNaissance) {
		if (dateNaissance == null) {
			this.dateNaissance = null;
		}
		else {
			this.dateNaissance = new SerializableDate(dateNaissance);
		}
	}

	/**
	 * @return the numeroAVS
	 */
	public String getNumeroAVS() {
		return numeroAVS;
	}

	/**
	 * @param numeroAVS
	 *            the numeroAVS to set
	 */
	public void setNumeroAVS(String numeroAVS) {
		this.numeroAVS = numeroAVS;
	}

	/**
	 * @return the numeroEtranger
	 */
	public String getNumeroEtranger() {
		return numeroEtranger;
	}

	/**
	 * @param numeroEtranger
	 *            the numeroEtranger to set
	 */
	public void setNumeroEtranger(String numeroEtranger) {
		this.numeroEtranger = numeroEtranger;
	}

	/**
	 * @return the formeJuridique
	 */
	public String getFormeJuridique() {
		return formeJuridique;
	}

	/**
	 * @param formeJuridique
	 *            the formeJuridique to set
	 */
	public void setFormeJuridique(String formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	public String getLocaliteOuPays() {
		return localiteOuPays;
	}

	public void setLocaliteOuPays(String localiteOuPays) {
		this.localiteOuPays = localiteOuPays;
	}

	/**
	 * @return the forPrincipalActif
	 */
	public boolean isForPrincipalActif() {
		return forPrincipalActif;
	}

	/**
	 * @param forPrincipalActif the forPrincipalActif to set
	 */
	public void setForPrincipalActif(boolean forPrincipalActif) {
		this.forPrincipalActif = forPrincipalActif;
	}

	/**
	 * @return the typeRechercheDuPaysLocalite
	 */
	public TypeRechercheLocalitePays getTypeRechercheDuPaysLocalite() {
		return typeRechercheDuPaysLocalite;
	}

	/**
	 * @param typeRechercheDuPaysLocalite the typeRechercheDuPaysLocalite to set
	 */
	public void setTypeRechercheDuPaysLocalite(TypeRechercheLocalitePays typeRechercheDuPaysLocalite) {
		this.typeRechercheDuPaysLocalite = typeRechercheDuPaysLocalite;
	}

	/**
	 * @return the typeVisualisation
	 */
	public TypeVisualisation getTypeVisualisation() {
		return typeVisualisation;
	}

	/**
	 * @param typeRechercheForFiscal the typeRechercheForFiscal to set
	 */
	public void setTypeVisualisation(TypeVisualisation typeVisualisation) {
		this.typeVisualisation = typeVisualisation;
	}

	public String getNoOfsFor() {
		return noOfsFor;
	}

	public void setNoOfsFor(String noOfsFor) {
		this.noOfsFor = noOfsFor;
	}

	public TypeTiers getTypeTiers() {
		return typeTiers;
	}

	public void setTypeTiers(TypeTiers typeTiers) {
		this.typeTiers = typeTiers;
	}

	public boolean isInclureI107() {
		return inclureI107;
	}

	public void setInclureI107(boolean inclureI107) {
		this.inclureI107 = inclureI107;
	}

	public boolean isInclureTiersAnnules() {
		return inclureTiersAnnules;
	}

	public void setInclureTiersAnnules(boolean inclureTiersAnnules) {
		this.inclureTiersAnnules = inclureTiersAnnules;
	}

	public boolean isTiersAnnulesSeulement() {
		return tiersAnnulesSeulement;
	}

	public void setTiersAnnulesSeulement(boolean tiersAnnulesSeulement) {
		this.tiersAnnulesSeulement = tiersAnnulesSeulement;
	}

	public String getNpa() {
		return npa;
	}

	public void setNpa(String npa) {
		this.npa = npa;
	}

	public ModeImposition getModeImposition() {
		return modeImposition;
	}

	public void setModeImposition(ModeImposition modeImposition) {
		this.modeImposition = modeImposition;
	}

	public String getNoSymic() {
		return noSymic;
	}

	public void setNoSymic(String noSymic) {
		this.noSymic = noSymic;
	}

	public CategorieImpotSource getCategorieDebiteurIs() {
		return categorieDebiteurIs;
	}

	public void setCategorieDebiteurIs(CategorieImpotSource categorieDebiteurIs) {
		this.categorieDebiteurIs = categorieDebiteurIs;
	}

	public Boolean isTiersActif() {
		return tiersActif;
	}

	public void setTiersActif(Boolean tiersActif) {
		this.tiersActif = tiersActif;
	}
}
