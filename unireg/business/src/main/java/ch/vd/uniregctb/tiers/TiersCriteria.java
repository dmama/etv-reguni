package ch.vd.uniregctb.tiers;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.Sexe;

/**
 * Critères de recherche pour les tiers.
 */
public class TiersCriteria implements Serializable, TiersFilter {

	public static class ValueOrNull<T extends Serializable> implements Serializable {

		private static final long serialVersionUID = -5550274478312973053L;

		public final T value;
		public final boolean orNull;

		private ValueOrNull(T value, boolean orNull) {
			this.value = value;
			this.orNull = orNull;
		}

		@Override
		public String toString() {
			return "ValueOrNull{" +
					"value=" + value +
					", orNull=" + orNull +
					'}';
		}
	}

	private static final long serialVersionUID = 4997424146812883522L;

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
		CONTRIBUABLE_PP;

		public static TypeTiers fromCore(ch.vd.uniregctb.tiers.TypeTiers type) {
			switch (type) {
			case PERSONNE_PHYSIQUE:
				return PERSONNE_PHYSIQUE;
			case MENAGE_COMMUN:
				return MENAGE_COMMUN;
			case ENTREPRISE:
				return ENTREPRISE;
			case ETABLISSEMENT:
				return ETABLISSEMENT;
			case COLLECTIVITE_ADMINISTRATIVE:
				return COLLECTIVITE_ADMINISTRATIVE;
			case AUTRE_COMMUNAUTE:
				return AUTRE_COMMUNAUTE;
			case DEBITEUR_PRESTATION_IMPOSABLE:
				return DEBITEUR_PRESTATION_IMPOSABLE;
			default:
				throw new IllegalArgumentException("Type de tiers inconnu = [" + type + ']');
			}
		}
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
	 * Les types de tiers (ignoré si le numéro de contribuable est donné) demandé par un utilisateur
	 */
	private Set<TypeTiers> typesTiers;

	/**
	 * Les types de tiers pris en compte dans tous les cas (même si un numéro de contribuable est donné), contraintes métiers plutôt...
	 */
	private Set<TypeTiers> typesTiersImperatifs;

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
	 * Le NPA pour une adresse courrier en suisse
	 */
	private String npaCourrier;

	/**
	 * Le NPA sur toutes les adresses (y.c. domicile, représentation et poursuite)
	 */
	private ValueOrNull<String> npaTous;

	/**
	 * La nature juridique
	 */
	private String natureJuridique;

	/**
	 * La date de naissance
	 */
	private ValueOrNull<RegDate> dateNaissance;

	/**
	 * Le sexe
	 */
	private ValueOrNull<Sexe> sexe;

	/**
	 * Le numéro AVS (11 ou 13, on ne sait pas...)
	 */
	private String numeroAVS;

	/**
	 * Le numéro AVS 11
	 */
	private ValueOrNull<String> navs11;

	/**
	 * Le numéro AVS 13
	 */
	private ValueOrNull<String> navs13;

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

	/**
	 * Ancien numéro de sourcier
	 */
	private Long ancienNumeroSourcier;

	/**
	 * Numéro IDE assotié au tiers
	 */
	private String numeroIDE;

	/**
	 * @return true si aucun paramétre de recherche n'est renseigné. false
	 *         autrement.
	 */
	public boolean isEmpty() {
		return numero == null
				&& StringUtils.isBlank(nomRaison)
				&& dateNaissance == null
				&& sexe == null
				&& StringUtils.isBlank(numeroEtranger)
				&& StringUtils.isBlank(numeroAVS)
				&& navs11 == null
				&& navs13 == null
				&& StringUtils.isBlank(localiteOuPays)
				&& StringUtils.isBlank(npaCourrier)
				&& npaTous == null
				&& StringUtils.isBlank(natureJuridique)
				&& StringUtils.isBlank(noOfsFor)
				&& StringUtils.isBlank(noSymic)
				&& StringUtils.isBlank(formeJuridique)
				&& modeImposition == null
				&& categorieDebiteurIs == null
				&& tiersActif == null
				&& ancienNumeroSourcier == null
				&& StringUtils.isBlank(numeroIDE);
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
	public void setNumero(@Nullable Long numero) {
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
	public ValueOrNull<RegDate> getDateNaissance() {
		return dateNaissance;
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
			this.dateNaissance = new ValueOrNull<>(dateNaissance, false);
		}
	}

	public void setDateNaissanceOrNull(RegDate dateNaissance) {
		this.dateNaissance = new ValueOrNull<>(dateNaissance, true);
	}

	public ValueOrNull<Sexe> getSexe() {
		return sexe;
	}

	public void setSexe(Sexe sexe) {
		if (sexe == null) {
			this.sexe = null;
		}
		else {
			this.sexe = new ValueOrNull<>(sexe, false);
		}
	}

	public void setSexeOrNull(Sexe sexe) {
		this.sexe = new ValueOrNull<>(sexe, true);
	}

	public ValueOrNull<String> getNavs11() {
		return navs11;
	}

	public void setNavs11(String navs11) {
		if (StringUtils.isBlank(navs11)) {
			this.navs11 = null;
		}
		else {
			this.navs11 = new ValueOrNull<>(navs11, false);
		}
	}

	public void setNavs11OrNull(String navs11) {
		this.navs11 = new ValueOrNull<>(navs11, true);
	}

	public ValueOrNull<String> getNavs13() {
		return navs13;
	}

	public void setNavs13(String navs13) {
		if (StringUtils.isBlank(navs13)) {
			this.navs13 = null;
		}
		else {
			this.navs13 = new ValueOrNull<>(navs13, false);
		}
	}

	public void setNavs13OrNull(String navs13) {
		this.navs13 = new ValueOrNull<>(navs13, true);
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
		this.numeroAVS = StringUtils.isBlank(numeroAVS) ? null : numeroAVS;
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
	@Override
	public TypeVisualisation getTypeVisualisation() {
		return typeVisualisation;
	}

	public void setTypeVisualisation(TypeVisualisation typeVisualisation) {
		this.typeVisualisation = typeVisualisation;
	}

	public String getNoOfsFor() {
		return noOfsFor;
	}

	public void setNoOfsFor(String noOfsFor) {
		this.noOfsFor = noOfsFor;
	}

	@Override
	public Set<TypeTiers> getTypesTiers() {
		return typesTiers;
	}

	public void setTypesTiers(Set<TypeTiers> typesTiers) {
		this.typesTiers = typesTiers;
	}

	public void setTypeTiers(TypeTiers type) {
		if (type == null) {
			this.typesTiers = null;
		}
		else {
			this.typesTiers = EnumSet.of(type);
		}
	}

	public Set<TypeTiers> getTypesTiersImperatifs() {
		return typesTiersImperatifs;
	}

	public void setTypesTiersImperatifs(Set<TypeTiers> typesTiersImperatifs) {
		this.typesTiersImperatifs = typesTiersImperatifs;
	}

	public void setTypeTiersImperatif(TypeTiers type) {
		if (type == null) {
			this.typesTiersImperatifs = null;
		}
		else {
			this.typesTiersImperatifs = EnumSet.of(type);
		}
	}

	@Override
	public boolean isInclureI107() {
		return inclureI107;
	}

	public void setInclureI107(boolean inclureI107) {
		this.inclureI107 = inclureI107;
	}

	@Override
	public boolean isInclureTiersAnnules() {
		return inclureTiersAnnules;
	}

	public void setInclureTiersAnnules(boolean inclureTiersAnnules) {
		this.inclureTiersAnnules = inclureTiersAnnules;
	}

	@Override
	public boolean isTiersAnnulesSeulement() {
		return tiersAnnulesSeulement;
	}

	public void setTiersAnnulesSeulement(boolean tiersAnnulesSeulement) {
		this.tiersAnnulesSeulement = tiersAnnulesSeulement;
	}

	public String getNpaCourrier() {
		return npaCourrier;
	}

	public void setNpaCourrier(String npaCourrier) {
		this.npaCourrier = npaCourrier;
	}

	public ValueOrNull<String> getNpaTous() {
		return npaTous;
	}

	public void setNpaTous(String npaTous) {
		if (StringUtils.isBlank(npaTous)) {
			this.npaTous = null;
		}
		else {
			this.npaTous = new ValueOrNull<>(npaTous, false);
		}
	}

	public void setNpaTousOrNull(String npaTous) {
		this.npaTous = new ValueOrNull<>(npaTous, true);
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

	@Override
	public Boolean isTiersActif() {
		return tiersActif;
	}

	public void setTiersActif(Boolean tiersActif) {
		this.tiersActif = tiersActif;
	}

	public Long getAncienNumeroSourcier() {
		return ancienNumeroSourcier;
	}

	public void setAncienNumeroSourcier(Long ancienNumeroSourcier) {
		this.ancienNumeroSourcier = ancienNumeroSourcier;
	}

	public String getNumeroIDE() {
		return numeroIDE;
	}

	public void setNumeroIDE(String numeroIDE) {
		this.numeroIDE = numeroIDE;
	}
}
