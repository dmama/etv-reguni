package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

@Entity
@Table(name = "PERIODE_FISCALE")
public class PeriodeFiscale extends HibernateEntity {

	private Long id;
	private Integer annee;
	private Set<ModeleDocument> modelesDocument;
	private Set<ParametrePeriodeFiscale> parametrePeriodeFiscale;
	private boolean showCodeControleSommationDeclaration = false;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long theId) {
		this.id = theId;
	}

	@Column(name = "ANNEE", unique = true, nullable = false)
	public Integer getAnnee() {
		return annee;
	}

	public void setAnnee(Integer theAnnee) {
		annee = theAnnee;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "PERIODE_ID")
	@ForeignKey(name = "FK_PARAM_PF_ID")
	public Set<ParametrePeriodeFiscale> getParametrePeriodeFiscale() {
		return parametrePeriodeFiscale;
	}

	public void setParametrePeriodeFiscale(Set<ParametrePeriodeFiscale> theParametrePeriodeFiscale) {
		parametrePeriodeFiscale = theParametrePeriodeFiscale;
	}

	public void addParametrePeriodeFiscale(ParametrePeriodeFiscale param) {
		if (parametrePeriodeFiscale == null) {
			parametrePeriodeFiscale = new HashSet<>();
		}
		param.setPeriodefiscale(this);
		parametrePeriodeFiscale.add(param);
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "PERIODE_ID")
	@ForeignKey(name = "FK_DOC_PF_ID")
	public Set<ModeleDocument> getModelesDocument() {
		return modelesDocument;
	}

	/**
	 * Ajoute le modèle spécifié à la période fiscale.
	 */
	public boolean addModeleDocument(ModeleDocument modele) {
		if (modelesDocument == null) {
			modelesDocument = new HashSet<>();
		}
		return modelesDocument.add(modele);
	}

	public void setModelesDocument(Set<ModeleDocument> set) {
		this.modelesDocument = set;
	}

	/**
	 * @return le {@link ParametrePeriodeFiscalePP} en fonction du {@link TypeContribuable} pour la période
	 */
	@Nullable
	public ParametrePeriodeFiscalePP getParametrePeriodeFiscalePP(TypeContribuable typeContribuable) {
		assert typeContribuable != null : "typeContribuable ne peut être null";
		for (ParametrePeriodeFiscale ppf : parametrePeriodeFiscale) {
			if (ppf instanceof ParametrePeriodeFiscalePP && typeContribuable == ppf.getTypeContribuable()) {
				return (ParametrePeriodeFiscalePP) ppf;
			}
		}
		return null;
	}

	/**
	 * @return le {@link ParametrePeriodeFiscalePM} en fonction du {@link TypeContribuable} pour la période
	 */
	@Nullable
	public ParametrePeriodeFiscalePM getParametrePeriodeFiscalePM(TypeContribuable typeContribuable) {
		assert typeContribuable != null : "typeContribuable ne peut être null";
		for (ParametrePeriodeFiscale ppf : parametrePeriodeFiscale) {
			if (ppf instanceof ParametrePeriodeFiscalePM && typeContribuable == ppf.getTypeContribuable()) {
				return (ParametrePeriodeFiscalePM) ppf;
			}
		}
		return null;
	}

	@Column(name = "CODE_CTRL_SOMM_DI_PP", nullable = false)
	public boolean isShowCodeControleSommationDeclaration() {
		return showCodeControleSommationDeclaration;
	}

	public void setShowCodeControleSommationDeclaration(boolean showCodeControleSommationDeclaration) {
		this.showCodeControleSommationDeclaration = showCodeControleSommationDeclaration;
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les contribuables PP vaudois ordinaires
	 */
	@Transient
	public ParametrePeriodeFiscalePP getParametrePeriodeFiscalePPVaudoisOrdinaire() {
		return getParametrePeriodeFiscalePP(TypeContribuable.VAUDOIS_ORDINAIRE);
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les contribuables PP vaudois à la dépense
	 */
	@Transient
	public ParametrePeriodeFiscalePP getParametrePeriodeFiscalePPDepense() {
		return getParametrePeriodeFiscalePP(TypeContribuable.VAUDOIS_DEPENSE);
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les diplomates Suisses basés à l'étranger
	 */
	@Transient
	public ParametrePeriodeFiscalePP getParametrePeriodeFiscalePPDiplomateSuisse() {
		return getParametrePeriodeFiscalePP(TypeContribuable.DIPLOMATE_SUISSE);
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les contribuables PP hors canton de Vaud
	 */
	@Transient
	public ParametrePeriodeFiscalePP getParametrePeriodeFiscalePPHorsCanton() {
		return getParametrePeriodeFiscalePP(TypeContribuable.HORS_CANTON);
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les contribuables PP hors Suisse
	 */
	@Transient
	public ParametrePeriodeFiscalePP getParametrePeriodeFiscalePPHorsSuisse() {
		return getParametrePeriodeFiscalePP(TypeContribuable.HORS_SUISSE);
	}

	/**
	 * @return
	 * 		La date la plus avancée pour la fin d'envoi de masse des DI parmis les parametres de la periode.<br>
	 * 		La date du jour s'il n'y a pas de {@link ParametrePeriodeFiscale} associés à la periode. (ce qui ne devrait pas arriver)
	 */
	@Transient
	@NotNull
	public RegDate getLatestDateFinEnvoiMasseDIPP() {
		RegDate date = null;
		for (ParametrePeriodeFiscale ppf : getParametrePeriodeFiscale()) {
			if (ppf instanceof ParametrePeriodeFiscalePP) {
				final ParametrePeriodeFiscalePP ppfpp = (ParametrePeriodeFiscalePP) ppf;
				if (date == null || date.isBefore(ppfpp.getDateFinEnvoiMasseDI())) {
					date = ppfpp.getDateFinEnvoiMasseDI();
				}
			}
		}
		return date != null ? date : RegDate.get();
	}

	/**
	 *  Initialise les parametres de la periode fiscale avec des valeurs par défaut
	 */
	public void setDefaultPeriodeFiscaleParametres() {
		addAllPeriodeFiscaleParametresPP(RegDate.get(this.getAnnee() + 1, 1, 31), // valeur par défaut des envoi de masse DI au 31 janvier
		                                 RegDate.get(this.getAnnee() + 1, 3, 31), // valeur par défaut du terme reglementaire pour les sommations au 31 mars
		                                 RegDate.get(this.getAnnee() + 1, 4, 30)     // valeur par défaut du terme effectif pour les sommations au 30 avril
		);
		addAllPeriodeFiscaleParametresPM(6, false, 75, false);
	}

	/**
	 * Ajoute les {@link ParametrePeriodeFiscalePP} (1 pour chaque type de contribuable PP) avec les dates initialisées aux valeurs les arguments de la méthode
	 *
	 * @param dateEnvoiMasseDI la date d'envoi de masse des DI pour les 5 {@link ParametrePeriodeFiscale}
	 * @param dateTermeGeneralSommationReglementaire du terme general réglementaire des sommations pour les 5 {@link ParametrePeriodeFiscale}
	 * @param dateTermeGeneralSommationEffectif la date du terme general effectif des sommations pour les 5 {@link ParametrePeriodeFiscale}
	 */
	public void addAllPeriodeFiscaleParametresPP(RegDate dateEnvoiMasseDI, RegDate dateTermeGeneralSommationReglementaire, RegDate dateTermeGeneralSommationEffectif) {
		addParametrePeriodeFiscale(new ParametrePeriodeFiscalePP(TypeContribuable.VAUDOIS_ORDINAIRE, dateEnvoiMasseDI, dateTermeGeneralSommationReglementaire, dateTermeGeneralSommationEffectif, this));
		addParametrePeriodeFiscale(new ParametrePeriodeFiscalePP(TypeContribuable.VAUDOIS_DEPENSE, dateEnvoiMasseDI, dateTermeGeneralSommationReglementaire, dateTermeGeneralSommationEffectif, this));
		addParametrePeriodeFiscale(new ParametrePeriodeFiscalePP(TypeContribuable.DIPLOMATE_SUISSE, dateEnvoiMasseDI, dateTermeGeneralSommationReglementaire, dateTermeGeneralSommationEffectif, this));
		addParametrePeriodeFiscale(new ParametrePeriodeFiscalePP(TypeContribuable.HORS_CANTON, dateEnvoiMasseDI, dateTermeGeneralSommationReglementaire, dateTermeGeneralSommationEffectif, this));
		addParametrePeriodeFiscale(new ParametrePeriodeFiscalePP(TypeContribuable.HORS_SUISSE, dateEnvoiMasseDI, dateTermeGeneralSommationReglementaire, dateTermeGeneralSommationEffectif, this));
	}

	/**
	 * Ajoute les {@link ParametrePeriodeFiscalePM} avec les dates initialisées aux valeurs les arguments de la méthode
	 * @param delaiImprimeMois le délai imprimé sur la DI envoyée
	 * @param delaiImprimeRepousseFinDeMois <code>true</code> si le délai effectivement imprimé doit être repoussé à la fin du mois
	 * @param toleranceJours le délai effectif utilisé pour les DI de PM
	 * @param delaiTolereRepousseFinDeMois <code>true</code> si le délai effectivement pris en compte (avec tolérance) doit être repoussé à la fin du mois
	 */
	public void addAllPeriodeFiscaleParametresPM(int delaiImprimeMois, boolean delaiImprimeRepousseFinDeMois,
	                                             int toleranceJours, boolean delaiTolereRepousseFinDeMois) {
		addParametrePeriodeFiscale(new ParametrePeriodeFiscalePM(TypeContribuable.VAUDOIS_ORDINAIRE, delaiImprimeMois, delaiImprimeRepousseFinDeMois, toleranceJours, delaiTolereRepousseFinDeMois, this));
		addParametrePeriodeFiscale(new ParametrePeriodeFiscalePM(TypeContribuable.HORS_CANTON, delaiImprimeMois, delaiImprimeRepousseFinDeMois, toleranceJours, delaiTolereRepousseFinDeMois, this));
		addParametrePeriodeFiscale(new ParametrePeriodeFiscalePM(TypeContribuable.HORS_SUISSE, delaiImprimeMois, delaiImprimeRepousseFinDeMois, toleranceJours, delaiTolereRepousseFinDeMois, this));
	}

	public boolean possedeTypeDocument(TypeDocument typeDocument) {
		return get(typeDocument) != null;
	}

	public ModeleDocument get(TypeDocument typeDocument) {
		for (ModeleDocument modele : getModelesDocument()) {
			if (typeDocument == modele.getTypeDocument()) {
				return modele;
			}
		}
		return null;
	}
}
