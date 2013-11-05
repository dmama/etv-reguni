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

	public void setParametrePeriodeFiscale(	Set<ParametrePeriodeFiscale> theParametrePeriodeFiscale) {
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
	 * @return le {@link ParametrePeriodeFiscale} en fonction du {@link TypeContribuable} pour la période
	 */
	public ParametrePeriodeFiscale getParametrePeriodeFiscale(TypeContribuable typeContribuable) {
		assert typeContribuable != null : "typeContribuable ne peut être null";
		for (ParametrePeriodeFiscale ppf : parametrePeriodeFiscale) {
			if (typeContribuable == ppf.getTypeContribuable()) {
				return ppf;
			}
		}
		return null;
	}

	@Column(name = "CODE_CTRL_SOMM_DI", nullable = false)
	public boolean isShowCodeControleSommationDeclaration() {
		return showCodeControleSommationDeclaration;
	}

	public void setShowCodeControleSommationDeclaration(boolean showCodeControleSommationDeclaration) {
		this.showCodeControleSommationDeclaration = showCodeControleSommationDeclaration;
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les contribuables vaudois
	 */
	@Transient
	public ParametrePeriodeFiscale getParametrePeriodeFiscaleVaud () {
		return getParametrePeriodeFiscale(TypeContribuable.VAUDOIS_ORDINAIRE);
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les contribuables vaudois
	 */
	@Transient
	public ParametrePeriodeFiscale getParametrePeriodeFiscaleDepense () {
		return getParametrePeriodeFiscale(TypeContribuable.VAUDOIS_DEPENSE);
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les diplomates Suisses basés à l'étranger
	 */
	@Transient
	public ParametrePeriodeFiscale getParametrePeriodeFiscaleDiplomateSuisse() {
		return getParametrePeriodeFiscale(TypeContribuable.DIPLOMATE_SUISSE);
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les contribuables hors canton de Vaud
	 */
	@Transient
	public ParametrePeriodeFiscale getParametrePeriodeFiscaleHorsCanton () {
		return getParametrePeriodeFiscale(TypeContribuable.HORS_CANTON);
	}

	/**
	 * @return le {@link ParametrePeriodeFiscale} pour les contribuables hors Suisse
	 */
	@Transient
	public ParametrePeriodeFiscale getParametrePeriodeFiscaleHorsSuisse () {
		return getParametrePeriodeFiscale(TypeContribuable.HORS_SUISSE);
	}
	/**
	 * @return
	 * 		La date la plus avancée pour la fin d'envoi de masse des DI parmis les parametres de la periode.<br>
	 * 		La date du jour s'il n'y a pas de {@link ParametrePeriodeFiscale} associés à la periode. (ce qui ne devrait pas arriver)
	 */
	@Transient
	public RegDate getLatestDateFinEnvoiMasseDI() {
		RegDate date = null;
		for (ParametrePeriodeFiscale ppf : getParametrePeriodeFiscale()) {
			if (date == null || date.isBefore(ppf.getDateFinEnvoiMasseDI())) {
				date = ppf.getDateFinEnvoiMasseDI();
			}
		}
		return date != null ? date : RegDate.get();
	}

	/**
	 *  Initialise les parametres de la periode fiscale avec des valeurs par défaut
	 */
	public void setDefaultPeriodeFiscaleParametres () {
		setAllPeriodeFiscaleParametres(
			RegDate.get(this.getAnnee() + 1, 1, 31), // valeur par défaut des envoi de masse DI au 31 janvier
			RegDate.get(this.getAnnee() + 1, 3, 31), // valeur par défaut du terme reglementaire pour les sommations au 31 mars
			RegDate.get(this.getAnnee() + 1, 4, 30)	 // valeur par défaut du terme effectif pour les sommations au 30 avril
		);
	}

	/**
	 * Crée le {@link Set} de 4 {@link ParametrePeriodeFiscale} (1 pour chaque type de contribuable) avec les dates initialisées aux valeurs les arguments de la méthode
	 *
	 * @param dateEnvoiMasseDI la date d'envoi de masse des DI pour les 4 {@link ParametrePeriodeFiscale}
	 * @param dateTermeGeneralSommationReglementaire du terme general réglementaire des sommations pour les 4 {@link ParametrePeriodeFiscale}
	 * @param dateTermeGeneralSommationEffectif la date du terme general effectif des sommations pour les 4 {@link ParametrePeriodeFiscale}
	 */
	public void setAllPeriodeFiscaleParametres(RegDate dateEnvoiMasseDI, RegDate dateTermeGeneralSommationReglementaire, RegDate dateTermeGeneralSommationEffectif) {

		Set<ParametrePeriodeFiscale> setParametrePeriodeFiscale = new HashSet<>(4);

		ParametrePeriodeFiscale ppf = new ParametrePeriodeFiscale();
		ppf.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		ppf.setPeriodefiscale(this);
		ppf.setTermeGeneralSommationEffectif(dateTermeGeneralSommationEffectif);
		ppf.setTermeGeneralSommationReglementaire(dateTermeGeneralSommationReglementaire);
		ppf.setDateFinEnvoiMasseDI(dateEnvoiMasseDI);
		setParametrePeriodeFiscale.add(ppf);

		ppf = new ParametrePeriodeFiscale();
		ppf.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		ppf.setPeriodefiscale(this);
		ppf.setTermeGeneralSommationEffectif(dateTermeGeneralSommationEffectif);
		ppf.setTermeGeneralSommationReglementaire(dateTermeGeneralSommationReglementaire);
		ppf.setDateFinEnvoiMasseDI(dateEnvoiMasseDI);
		setParametrePeriodeFiscale.add(ppf);

		ppf = new ParametrePeriodeFiscale();
		ppf.setTypeContribuable(TypeContribuable.DIPLOMATE_SUISSE);
		ppf.setPeriodefiscale(this);
		ppf.setTermeGeneralSommationEffectif(dateTermeGeneralSommationEffectif);
		ppf.setTermeGeneralSommationReglementaire(dateTermeGeneralSommationReglementaire);
		ppf.setDateFinEnvoiMasseDI(dateEnvoiMasseDI);
		setParametrePeriodeFiscale.add(ppf);

		ppf = new ParametrePeriodeFiscale();
		ppf.setTypeContribuable(TypeContribuable.HORS_CANTON);
		ppf.setPeriodefiscale(this);
		ppf.setTermeGeneralSommationEffectif(dateTermeGeneralSommationEffectif);
		ppf.setTermeGeneralSommationReglementaire(dateTermeGeneralSommationReglementaire);
		ppf.setDateFinEnvoiMasseDI(dateEnvoiMasseDI);
		setParametrePeriodeFiscale.add(ppf);

		ppf = new ParametrePeriodeFiscale();
		ppf.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		ppf.setPeriodefiscale(this);
		ppf.setTermeGeneralSommationEffectif(dateTermeGeneralSommationEffectif);
		ppf.setTermeGeneralSommationReglementaire(dateTermeGeneralSommationReglementaire);
		ppf.setDateFinEnvoiMasseDI(dateEnvoiMasseDI);
		setParametrePeriodeFiscale.add(ppf);

		setParametrePeriodeFiscale(setParametrePeriodeFiscale);
	}

	@Transient
	public boolean possedeTypeDocument(TypeDocument typeDocument) {
		for (ModeleDocument modele : getModelesDocument())  {
			if (typeDocument == modele.getTypeDocument()) {
				return true;
			}
		}
		return false;
	}

}
