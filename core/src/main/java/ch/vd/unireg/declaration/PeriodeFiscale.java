package ch.vd.unireg.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPM;
import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPP;
import ch.vd.unireg.parametrage.ParametreDemandeDelaisOnline;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscale;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleEmolument;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePM;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscalePP;
import ch.vd.unireg.parametrage.ParametrePeriodeFiscaleSNC;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeDocumentEmolument;
import ch.vd.unireg.type.delai.Delai;

@Entity
@Table(name = "PERIODE_FISCALE")
public class PeriodeFiscale extends HibernateEntity {

	private Long id;
	private Integer annee;
	private Set<ModeleDocument> modelesDocument;
	private Set<ParametrePeriodeFiscale> parametrePeriodeFiscale;
	private boolean showCodeControleSommationDeclarationPP = false;
	private boolean showCodeControleSommationDeclarationPM = false;
	private boolean showCodeControleRappelQuestionnaireSNC = false;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
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

	// configuration hibernate : la période fiscale possède les paramètres
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "PERIODE_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_PARAM_PF_ID"))
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
	@JoinColumn(name = "PERIODE_ID", foreignKey = @ForeignKey(name = "FK_DOC_PF_ID"))
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
	@Transient
	@Nullable
	public ParametrePeriodeFiscalePP getParametrePeriodeFiscalePP(TypeContribuable typeContribuable) {
		assert typeContribuable != null : "typeContribuable ne peut être null";
		for (ParametrePeriodeFiscale ppf : parametrePeriodeFiscale) {
			if (ppf instanceof ParametrePeriodeFiscalePP && typeContribuable == ((ParametrePeriodeFiscalePP) ppf).getTypeContribuable()) {
				return (ParametrePeriodeFiscalePP) ppf;
			}
		}
		return null;
	}

	/**
	 * @return le {@link ParametrePeriodeFiscalePM} en fonction du {@link TypeContribuable} pour la période
	 */
	@Transient
	@Nullable
	public ParametrePeriodeFiscalePM getParametrePeriodeFiscalePM(TypeContribuable typeContribuable) {
		assert typeContribuable != null : "typeContribuable ne peut être null";
		for (ParametrePeriodeFiscale ppf : parametrePeriodeFiscale) {
			if (ppf instanceof ParametrePeriodeFiscalePM && typeContribuable == ((ParametrePeriodeFiscalePM) ppf).getTypeContribuable()) {
				return (ParametrePeriodeFiscalePM) ppf;
			}
		}
		return null;
	}

	@Transient
	@Nullable
	public ParametrePeriodeFiscaleSNC getParametrePeriodeFiscaleSNC() {
		for (ParametrePeriodeFiscale ppf : parametrePeriodeFiscale) {
			if (ppf instanceof ParametrePeriodeFiscaleSNC) {
				return (ParametrePeriodeFiscaleSNC) ppf;
			}
		}
		return null;
	}

	@Transient
	@Nullable
	public ParametrePeriodeFiscaleEmolument getParametrePeriodeFiscaleEmolument(TypeDocumentEmolument type) {
		for (ParametrePeriodeFiscale ppf : parametrePeriodeFiscale) {
			if (ppf instanceof ParametrePeriodeFiscaleEmolument && type == ((ParametrePeriodeFiscaleEmolument) ppf).getTypeDocument()) {
				return (ParametrePeriodeFiscaleEmolument) ppf;
			}
		}
		return null;
	}

	@Column(name = "CODE_CTRL_SOMM_DI_PP", nullable = false)
	public boolean isShowCodeControleSommationDeclarationPP() {
		return showCodeControleSommationDeclarationPP;
	}

	public void setShowCodeControleSommationDeclarationPP(boolean showCodeControleSommationDeclarationPP) {
		this.showCodeControleSommationDeclarationPP = showCodeControleSommationDeclarationPP;
	}

	@Column(name = "CODE_CTRL_SOMM_DI_PM", nullable = false)
	public boolean isShowCodeControleSommationDeclarationPM() {
		return showCodeControleSommationDeclarationPM;
	}

	public void setShowCodeControleSommationDeclarationPM(boolean showCodeControleSommationDeclarationPM) {
		this.showCodeControleSommationDeclarationPM = showCodeControleSommationDeclarationPM;
	}

	@Column(name = "CODE_CTRL_RAPPEL_Q_SNC", nullable = false)
	public boolean isShowCodeControleRappelQuestionnaireSNC() {
		return showCodeControleRappelQuestionnaireSNC;
	}

	public void setShowCodeControleRappelQuestionnaireSNC(boolean showCodeControleRappelQuestionnaireSNC) {
		this.showCodeControleRappelQuestionnaireSNC = showCodeControleRappelQuestionnaireSNC;
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
		                                 RegDate.get(this.getAnnee() + 1, 6, 30)     // valeur par défaut du terme effectif pour les sommations au 30 juin
		);
		addPeriodeFiscaleParametrePM(6, false, 75, false, ParametrePeriodeFiscalePM.ReferencePourDelai.FIN_PERIODE, TypeContribuable.HORS_CANTON, TypeContribuable.HORS_SUISSE, TypeContribuable.VAUDOIS_ORDINAIRE);
		addPeriodeFiscaleParametrePM(24, false, 15, false, ParametrePeriodeFiscalePM.ReferencePourDelai.EMISSION, TypeContribuable.UTILITE_PUBLIQUE);
		addPeriodeFiscaleParametreSNC(RegDate.get(this.getAnnee() + 1, 3, 15),      // valeur par défaut du terme réglementaire au 15 mars
		                              RegDate.get(this.getAnnee() + 1, 8, 31));     // valeur par défaut du terme effectif au 31 août
		addPeriodeFiscaleParametreEmolument(null);

		addDefaultParametresDemandeDelaisOnlinePP();
		addDefaultParametresDemandeDelaisOnlinePM();
	}

	private void addDefaultParametresDemandeDelaisOnlinePP() {

		// configuration par défaut des délais pour les PP :
		//
		//  Période        | Délais unitaire | Délais groupée
		// -----------------------------------------------------
		//  01.01 - 15.05  | 30.06           | 30.06
		//  16.05 - 15.06  | 30.06, 30.09    | 30.06, 30.09
		//  16.06 - 30.08  | 30.09           | -
		//  01.09 - 31.12  | -               | -
		//
		ParametreDemandeDelaisOnline params = new ParametreDemandeDelaisOnline();
		params.setTypeTiers(ParametreDemandeDelaisOnline.Type.PP);
		{
			params.addPeriodeDelais(new DelaisAccordablesOnlineDIPP(RegDate.get(annee + 1, 1, 1), RegDate.get(annee + 1, 5, 15), DayMonth.get(6, 30), DayMonth.get(6, 30)));
			params.addPeriodeDelais(new DelaisAccordablesOnlineDIPP(RegDate.get(annee + 1, 5, 16), RegDate.get(annee + 1, 6, 15),
			                                                        Arrays.asList(DayMonth.get(6, 30), DayMonth.get(9, 30)),
			                                                        Arrays.asList(DayMonth.get(6, 30), DayMonth.get(9, 30))));
			params.addPeriodeDelais(new DelaisAccordablesOnlineDIPP(RegDate.get(annee + 1, 6, 16), RegDate.get(annee + 1, 8, 31), DayMonth.get(9, 30), null));
			params.addPeriodeDelais(new DelaisAccordablesOnlineDIPP(RegDate.get(annee + 1, 9, 1), RegDate.get(annee + 1, 12, 31)));
		}
		addParametrePeriodeFiscale(params);
	}

	private void addDefaultParametresDemandeDelaisOnlinePM() {

		// configuration par défaut des délais pour les PM :
		//
		//  Période   | Délais unitaire        | Délais groupée
		// ---------------------------------------------------
		//  + 0M      | + 6M + 75D             | + 7M + 75D
		//  + 6M      | + 6M + 75D, + 9M + 75D | + 7M + 75D, + 10M + 75D
		//  + 9M      | -                      | -
		//
		final ParametreDemandeDelaisOnline params = new ParametreDemandeDelaisOnline();
		params.setTypeTiers(ParametreDemandeDelaisOnline.Type.PM);
		{
			params.addPeriodeDelais(new DelaisAccordablesOnlineDIPM(0, Delai.fromString("0D"), Delai.fromString("6M + 75D"), Delai.fromString("7M + 75D")));
			params.addPeriodeDelais(new DelaisAccordablesOnlineDIPM(1, Delai.fromString("6M"),
			                                                        Arrays.asList(Delai.fromString("6M + 75D"), Delai.fromString("9M + 75D")),
			                                                        Arrays.asList(Delai.fromString("7M + 75D"), Delai.fromString("10M + 75D"))));
			params.addPeriodeDelais(new DelaisAccordablesOnlineDIPM(2, Delai.fromString("9M")));
		}
		addParametrePeriodeFiscale(params);
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
	public void addPeriodeFiscaleParametrePM(int delaiImprimeMois, boolean delaiImprimeRepousseFinDeMois,
	                                         int toleranceJours, boolean delaiTolereRepousseFinDeMois,
	                                         ParametrePeriodeFiscalePM.ReferencePourDelai referenceDelaiInitial,
	                                         TypeContribuable... typesContribuable) {
		for (TypeContribuable typeContribuable : typesContribuable) {
			addParametrePeriodeFiscale(new ParametrePeriodeFiscalePM(typeContribuable, delaiImprimeMois, delaiImprimeRepousseFinDeMois, toleranceJours, delaiTolereRepousseFinDeMois, referenceDelaiInitial, this));
		}
	}

	public void addPeriodeFiscaleParametreSNC(RegDate dateRappelReglementaire, RegDate dateRappelEffectif) {
		addParametrePeriodeFiscale(new ParametrePeriodeFiscaleSNC(this, dateRappelReglementaire, dateRappelEffectif));
	}

	public void addPeriodeFiscaleParametreEmolument(@Nullable Integer sommationDIPP) {
		addParametrePeriodeFiscale(new ParametrePeriodeFiscaleEmolument(TypeDocumentEmolument.SOMMATION_DI_PP, sommationDIPP, this));
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
