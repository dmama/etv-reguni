package ch.vd.uniregctb.tiers;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEtatEntreprise;

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

	private static final long serialVersionUID = 6605993235524782552L;

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
		ETABLISSEMENT_PRINCIPAL,
		ETABLISSEMENT_SECONDAIRE,
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

	public enum TypeInscriptionRC {
		/**
		 * Retrouve les entreprises qui possèdent une inscription au RC, quelle qu'elle soit
		 */
		AVEC_INSCRIPTION,

		/**
		 * Retrouve les entreprises qui ne possèdent aucune inscription au RC, quelle qu'elle soit
		 */
		SANS_INSCRIPTION,

		/**
		 * Retrouve les entreprises dont l'inscription au RC les décrit comme actives
		 */
		INSCRIT_ACTIF,

		/**
		 * Retrouve les entreprises dont l'inscription au RC les décrit comme radiées
		 */
		INSCRIT_RADIE
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
	private ValueOrNull<RegDate> dateNaissanceInscriptionRC;

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
	private FormeJuridiqueEntreprise formeJuridique;

	/**
	 * La catégorie d'entreprise découlant de la forme juridique
	 */
	private CategorieEntreprise categorieEntreprise;

	/**
	 * Numéro OFS du for
	 */
	private String noOfsFor;

	/**
	 * For principal actif uniquement
	 */
	private boolean forPrincipalActif;

	/**
	 * Motifs de fermeture du dernier for principal
	 */
	private Set<MotifFor> motifsFermetureDernierForPrincipal;

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
	 * Numéro IDE associé au tiers
	 */
	private String numeroIDE;

	/**
	 * Numéro RC/FOSC associé au tiers
	 */
	private String numeroRC;

	/**
	 * Types d'états entreprise non-présents (si présent, ce critère excluera de la liste de résultats toutes les
	 * entreprises dont l'un des états non-annulés est l'un des états du critère)
	 */
	private Set<TypeEtatEntreprise> etatsEntrepriseInterdits;

	/**
	 * Types d'états entreprise interdits en tant qu'état courant (si présent, ce critère excluera de la liste des résultats
	 * toutes les entreprises dont l'état courant est cité)
	 */
	private Set<TypeEtatEntreprise> etatsEntrepriseCourantsInterdits;

	/**
	 * Type d'état entreprise recherché comme état courant (en principe, donc, mettre une valeur ici qui est également
	 * dans le champ {@link #etatsEntrepriseCourantsInterdits} causera une liste de résultats vide)
	 */
	private TypeEtatEntreprise etatEntrepriseCourant;

	/**
	 * Etat courant vis-à-vis du Registre du Commerce
	 */
	private TypeInscriptionRC etatInscriptionRC;

	/**
	 * <code>true</code> si on ne veut que les entreprises qui ont un jour absorbé une autre entreprise, <code>false</code> si on ne veut que
	 * les entreprises qui n'ont jamais rien absorbé, et <code>null</code> si cela n'a pas d'importance
	 */
	private Boolean isCorporationMergeResult;

	/**
	 * <code>true</code> si on ne veut que les entreprises, personnes physiques et établissements connus au civil, <code>false</code> si on ne
	 * veut que ces entités
	 */
	private Boolean isConnuAuCivil;

	/**
	 * <code>true</code> si on ne veut que les entreprises qui ont un jour subi une scission, <code>false</code> si on ne veut que celles qui n'ont pas subi
	 * ce genre d'opération, et <code>null</code> si cela n'a pas d'importance
	 */
	private Boolean isCorporationSplit;

	/**
	 * <code>true</code> si on ne veut que les entreprises qui ont un jour transféré (= émis) du patrimoine vers une ou plusieurs autres entreprises, <code>false</code>
	 * si on ne veut justement pas ces entreprises, et <code>null</code> si cela n'a aucune importance
	 */
	private Boolean hasCorporationTransferedPatrimony;

	/**
	 * @return true si aucun paramètre de recherche n'est renseigné. false
	 *         autrement.
	 */
	public boolean isEmpty() {
		return numero == null
				&& StringUtils.isBlank(nomRaison)
				&& dateNaissanceInscriptionRC == null
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
				&& motifsFermetureDernierForPrincipal == null
				&& StringUtils.isBlank(noSymic)
				&& formeJuridique == null
				&& categorieEntreprise == null
				&& modeImposition == null
				&& categorieDebiteurIs == null
				&& tiersActif == null
				&& ancienNumeroSourcier == null
				&& StringUtils.isBlank(numeroIDE)
				&& StringUtils.isBlank(numeroRC)
				&& etatInscriptionRC == null
				&& etatEntrepriseCourant == null;
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
	public ValueOrNull<RegDate> getDateNaissanceInscriptionRC() {
		return dateNaissanceInscriptionRC;
	}

	/**
	 * @param dateNaissanceInscriptionRC
	 *            the dateNaissance to set
	 */
	public void setDateNaissanceInscriptionRC(RegDate dateNaissanceInscriptionRC) {
		if (dateNaissanceInscriptionRC == null) {
			this.dateNaissanceInscriptionRC = null;
		}
		else {
			this.dateNaissanceInscriptionRC = new ValueOrNull<>(dateNaissanceInscriptionRC, false);
		}
	}

	public void setDateNaissanceOrNull(RegDate dateNaissance) {
		this.dateNaissanceInscriptionRC = new ValueOrNull<>(dateNaissance, true);
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
	public FormeJuridiqueEntreprise getFormeJuridique() {
		return formeJuridique;
	}

	/**
	 * @param formeJuridique
	 *            the formeJuridique to set
	 */
	public void setFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	public CategorieEntreprise getCategorieEntreprise() {
		return categorieEntreprise;
	}

	public void setCategorieEntreprise(CategorieEntreprise categorieEntreprise) {
		this.categorieEntreprise = categorieEntreprise;
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

	public Set<MotifFor> getMotifsFermetureDernierForPrincipal() {
		return motifsFermetureDernierForPrincipal;
	}

	public void setMotifsFermetureDernierForPrincipal(Set<MotifFor> motifsFermetureDernierForPrincipal) {
		this.motifsFermetureDernierForPrincipal = motifsFermetureDernierForPrincipal;
	}

	public void setMotifFermetureDernierForPrincipal(MotifFor motifFermetureDernierForPrincipal) {
		if (motifFermetureDernierForPrincipal == null) {
			this.motifsFermetureDernierForPrincipal = null;
		}
		else {
			this.motifsFermetureDernierForPrincipal = EnumSet.of(motifFermetureDernierForPrincipal);
		}
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

	public String getNumeroRC() {
		return numeroRC;
	}

	public void setNumeroRC(String numeroRC) {
		this.numeroRC = numeroRC;
	}

	public Set<TypeEtatEntreprise> getEtatsEntrepriseInterdits() {
		return etatsEntrepriseInterdits;
	}

	public void setEtatsEntrepriseInterdits(Set<TypeEtatEntreprise> etatsEntrepriseInterdits) {
		this.etatsEntrepriseInterdits = etatsEntrepriseInterdits;
	}

	public Set<TypeEtatEntreprise> getEtatsEntrepriseCourantsInterdits() {
		return etatsEntrepriseCourantsInterdits;
	}

	public void setEtatsEntrepriseCourantsInterdits(Set<TypeEtatEntreprise> etatsEntrepriseCourantsInterdits) {
		this.etatsEntrepriseCourantsInterdits = etatsEntrepriseCourantsInterdits;
	}

	public TypeInscriptionRC getEtatInscriptionRC() {
		return etatInscriptionRC;
	}

	public void setEtatInscriptionRC(TypeInscriptionRC etatInscriptionRC) {
		this.etatInscriptionRC = etatInscriptionRC;
	}

	public TypeEtatEntreprise getEtatEntrepriseCourant() {
		return etatEntrepriseCourant;
	}

	public void setEtatEntrepriseCourant(TypeEtatEntreprise etatEntrepriseCourant) {
		this.etatEntrepriseCourant = etatEntrepriseCourant;
	}

	public Boolean getCorporationMergeResult() {
		return isCorporationMergeResult;
	}

	public void setCorporationMergeResult(Boolean corporationMergeResult) {
		isCorporationMergeResult = corporationMergeResult;
	}

	public Boolean getCorporationSplit() {
		return isCorporationSplit;
	}

	public void setCorporationSplit(Boolean corporationSplit) {
		isCorporationSplit = corporationSplit;
	}

	public Boolean hasCorporationTransferedPatrimony() {
		return hasCorporationTransferedPatrimony;
	}

	public void setCorporationTransferedPatrimony(Boolean hasCorporationTransferedPatrimony) {
		this.hasCorporationTransferedPatrimony = hasCorporationTransferedPatrimony;
	}

	public Boolean getConnuAuCivil() {
		return isConnuAuCivil;
	}

	public void setConnuAuCivil(Boolean connuAuCivil) {
		isConnuAuCivil = connuAuCivil;
	}
}
