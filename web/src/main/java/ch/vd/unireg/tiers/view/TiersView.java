package ch.vd.unireg.tiers.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.view.QuestionnaireSNCView;
import ch.vd.unireg.di.view.DeclarationImpotView;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscalView;
import ch.vd.unireg.entreprise.EntrepriseView;
import ch.vd.unireg.entreprise.EtablissementView;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.individu.IndividuView;
import ch.vd.unireg.lr.view.ListeRecapitulativeView;
import ch.vd.unireg.mandataire.AccesMandatairesView;
import ch.vd.unireg.mandataire.MandataireCourrierView;
import ch.vd.unireg.mandataire.MandatairePerceptionView;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.mouvement.view.MouvementDetailView;
import ch.vd.unireg.rapport.view.RapportView;
import ch.vd.unireg.rt.view.RapportPrestationView;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.DecisionAciView;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.GroupeFlagsEntreprise;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeFlagEntreprise;

/**
 * Structure model commun a l'ecran de visualisation et
 * l'ecran d'edition des Tiers
 *
 * @author xcifde
 *
 */
public class TiersView {

	private TiersGeneralView tiersGeneral;

	private ComplementView complement;

	// deprecated : car il ne s'agit pas d'une vue (risque de lazy init exception)
	@Deprecated
	private Tiers tiers;
	@Deprecated
	private PersonnePhysique tiersPrincipal;
	@Deprecated
	private PersonnePhysique tiersConjoint;

	private String nomPrenomPrincipal;
	private String nomPrenomConjoint;

	private IndividuView individu;
	private IndividuView individuConjoint;

	private List<EtiquetteTiersView> etiquettes;
	private List<EtiquetteTiersView> etiquettesConjoint;

	private List<AdresseView> historiqueAdresses;

	private List<AdresseCivilView> historiqueAdressesCiviles;
	private String exceptionAdresseCiviles;

	private List<AdresseCivilView> historiqueAdressesCivilesConjoint;
	private String exceptionAdresseCivilesConjoint;

	private List<AdresseView> adressesEnErreur;

	private String adressesEnErreurMessage;

	private List<RapportView> rapportsEtablissements;
	private List<RapportView> dossiersApparentes;
	private List<RapportPrestationView> rapportsPrestation;

	private Set<DebiteurView> debiteurs;

	private List<RapportView> contribuablesAssocies;

	private List<ListeRecapitulativeView> lrs;
	private List<DeclarationImpotView> dis;
	private List<QuestionnaireSNCView> questionnairesSNC;

	private ForFiscalView forsPrincipalActif;

	private List<ForFiscalView> forsFiscaux;

	private List<PeriodiciteView> periodicites;

	private PeriodiciteView periodicite;

	private boolean withSituationsFamille;
	private List<SituationFamilleView> situationsFamille;
	private String situationsFamilleEnErreurMessage;

	private List<MouvementDetailView> mouvements;

	private EntrepriseView entreprise;
	private EtablissementView etablissement;
	private String exceptionDonneesCiviles;

	private RegDate dateDebutPremierExerciceCommercial;
	private List<RegimeFiscalView> regimesFiscauxVD;
	private List<RegimeFiscalView> regimesFiscauxCH;
	private List<AllegementFiscalView> allegementsFiscaux;
	private List<ExerciceCommercial> exercicesCommerciaux;
	private List<FlagEntrepriseView> flags;

	private List<AutreDocumentFiscalView> autresDocumentsFiscauxSuivis;
	private List<AutreDocumentFiscalView> autresDocumentsFiscauxNonSuivis;

	private List<DomicileEtablissementView> domicilesEtablissement;

	private List<CommuneView> communesImmeubles;

	private boolean isAllowed;

	private boolean addContactISAllowed;

	private LogicielView logiciel;

	private List<DecisionAciView> decisionsAci;

	private boolean decisionRecente;

	//10 éléments à afficher par défaut
	private int nombreElementsTable = 10;

	private boolean forsPrincipauxPagines = true;
	private boolean forsSecondairesPagines = true;
	private boolean autresForsPagines = true;

	private AccesMandatairesView accesMandataire;
	private List<MandataireCourrierView> mandatairesCourrier;
	private List<MandatairePerceptionView> mandatairesPerception;

	private boolean civilSousControleACI;

	private boolean bouclementsRenseignes;

	public boolean isPmOuEtablissement() {
		return tiers instanceof ContribuableImpositionPersonnesMorales || tiers instanceof Etablissement;
	}


	public ComplementView getComplement() {
		return complement;
	}

	public void setComplement(ComplementView complement) {
		this.complement = complement;
	}

	/**
	 * @return the tiers
	 */
	@Deprecated
	public Tiers getTiers() {
		return tiers;
	}

	/**
	 * @param tiers the tiers to set
	 */
	@Deprecated
	public void setTiers(@Nullable Tiers tiers) {
		this.tiers = tiers;
	}

	/**
	 * @return the individu
	 */
	public IndividuView getIndividu() {
		return individu;
	}

	/**
	 * @param individu the individu to set
	 */
	public void setIndividu(IndividuView individu) {
		this.individu = individu;
	}


	/**
	 * @return la nature du tiers (	Entreprise / Etablissement / Habitant / Non Habitant / AutreCommunaute / MenageCommun)
	 */
	public NatureTiers getNatureTiers() {
		return tiers != null ? tiers.getNatureTiers() : null;
	}

	/**
	 * @return si le tiers est inactif (ancien I107)
	 */
	public boolean isDebiteurInactif() {
		return tiers == null || tiers.isDebiteurInactif();
	}

	private <T extends ForFiscalPrincipal> Optional<T> getForFiscalPrincipalCourant(Class<T> clazz) {
		return Stream.of(tiers)
				.filter(Objects::nonNull)
				.map(t -> t.getForsFiscauxValidAt(null))
				.flatMap(List::stream)
				.filter(clazz::isInstance)
				.map(clazz::cast)
				.findFirst();
	}

	/**
	 * @return le type d'autorité fiscale du for principal actif
	 */
	@Nullable
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return getForFiscalPrincipalCourant(ForFiscalPrincipal.class)
				.map(ForFiscalPrincipal::getTypeAutoriteFiscale)
				.orElse(null);
	}

	/**
	 * @return le mode d'imposition du for principal PP actif
	 */
	@Nullable
	public ModeImposition getModeImposition() {
		return getForFiscalPrincipalCourant(ForFiscalPrincipalPP.class)
				.map(ForFiscalPrincipalPP::getModeImposition)
				.orElse(null);
	}

	@Deprecated
	public PersonnePhysique getTiersConjoint() {
		return tiersConjoint;
	}

	@Deprecated
	public void setTiersConjoint(PersonnePhysique tiersConjoint) {
		this.tiersConjoint = tiersConjoint;
	}

	public IndividuView getIndividuConjoint() {
		return individuConjoint;
	}

	public void setIndividuConjoint(IndividuView individuConjoint) {
		this.individuConjoint = individuConjoint;
	}

	public boolean isWithCanceledIndividu() {
		return isCanceledIndividu(individu) || isCanceledIndividu(individuConjoint);
	}

	private static boolean isCanceledIndividu(IndividuView view) {
		return view != null && view.isCanceled();
	}

	public List<AdresseView> getHistoriqueAdresses() {
		return historiqueAdresses;
	}

	public void setHistoriqueAdresses(List<AdresseView> historiqueAdresses) {
		this.historiqueAdresses = historiqueAdresses;
	}

	public NatureTiers getNatureMembrePrincipal() {
		if (tiersPrincipal != null)
			return tiersPrincipal.getNatureTiers();
		return null;
	}

	public NatureTiers getNatureMembreConjoint() {
		if (tiersConjoint != null)
			return tiersConjoint.getNatureTiers();
		return null;
	}

	@Deprecated
	public PersonnePhysique getTiersPrincipal() {
		return tiersPrincipal;
	}

	@Deprecated
	public void setTiersPrincipal(PersonnePhysique tiersPrincipal) {
		this.tiersPrincipal = tiersPrincipal;
	}

	public List<RapportView> getRapportsEtablissements() {
		return rapportsEtablissements;
	}

	public void setRapportsEtablissements(List<RapportView> rapportsEtablissements) {
		this.rapportsEtablissements = rapportsEtablissements;
	}

	public List<RapportView> getDossiersApparentes() {
		return dossiersApparentes;
	}

	public void setDossiersApparentes(List<RapportView> dossiersApparentes) {
		this.dossiersApparentes = dossiersApparentes;
	}

	public Set<DebiteurView> getDebiteurs() {
		return debiteurs;
	}

	public void setDebiteurs(Set<DebiteurView> debiteurs) {
		this.debiteurs = debiteurs;
	}

	public List<RapportPrestationView> getRapportsPrestation() {
		return rapportsPrestation;
	}

	public void setRapportsPrestation(List<RapportPrestationView> rapportsPrestation) {
		this.rapportsPrestation = rapportsPrestation;
	}

	public List<ListeRecapitulativeView> getLrs() {
		return lrs;
	}

	public void setLrs(List<ListeRecapitulativeView> lrs) {
		this.lrs = lrs;
	}

	public EntrepriseView getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(EntrepriseView entreprise) {
		this.entreprise = entreprise;
	}

	public EtablissementView getEtablissement() {
		return etablissement;
	}

	public void setEtablissement(EtablissementView etablissement) {
		this.etablissement = etablissement;
	}

	public RegDate getDateDebutPremierExerciceCommercial() {
		return dateDebutPremierExerciceCommercial;
	}

	public void setDateDebutPremierExerciceCommercial(RegDate dateDebutPremierExerciceCommercial) {
		this.dateDebutPremierExerciceCommercial = dateDebutPremierExerciceCommercial;
	}

	public List<RegimeFiscalView> getRegimesFiscauxVD() {
		return regimesFiscauxVD;
	}

	public void setRegimesFiscauxVD(List<RegimeFiscalView> regimesFiscauxVD) {
		this.regimesFiscauxVD = regimesFiscauxVD;
	}

	public List<RegimeFiscalView> getRegimesFiscauxCH() {
		return regimesFiscauxCH;
	}

	public void setRegimesFiscauxCH(List<RegimeFiscalView> regimesFiscauxCH) {
		this.regimesFiscauxCH = regimesFiscauxCH;
	}

	public List<AllegementFiscalView> getAllegementsFiscaux() {
		return allegementsFiscaux;
	}

	public void setAllegementsFiscaux(List<AllegementFiscalView> allegementsFiscaux) {
		this.allegementsFiscaux = allegementsFiscaux;
	}

	public List<ExerciceCommercial> getExercicesCommerciaux() {
		return exercicesCommerciaux;
	}

	public void setExercicesCommerciaux(List<ExerciceCommercial> exercicesCommerciaux) {
		this.exercicesCommerciaux = exercicesCommerciaux;
	}

	public ExerciceCommercial getExerciceCommercialCourant() {
		return DateRangeHelper.rangeAt(exercicesCommerciaux, RegDate.get());
	}


	public List<FlagEntrepriseView> getFlags() {
		return flags;
	}

	@SuppressWarnings("unused")
	public List<FlagEntrepriseView> getFlags(GroupeFlagsEntreprise groupe) {
		final List<FlagEntrepriseView> filtered = new ArrayList<>(flags.size());
		final Set<TypeFlagEntreprise> types = TypeFlagEntreprise.ofGroupe(groupe);
		for (FlagEntrepriseView flag : flags) {
			if (types.contains(flag.getType())) {
				filtered.add(flag);
			}
		}
		return filtered;
	}

	public void setFlags(List<FlagEntrepriseView> flags) {
		this.flags = flags;
	}

	public List<DomicileEtablissementView> getDomicilesEtablissement() {
		return domicilesEtablissement;
	}

	public void setDomicilesEtablissement(List<DomicileEtablissementView> domicilesEtablissement) {
		this.domicilesEtablissement = domicilesEtablissement;
	}

	public List<AutreDocumentFiscalView> getAutresDocumentsFiscauxSuivis() {
		return autresDocumentsFiscauxSuivis;
	}

	public void setAutresDocumentsFiscauxSuivis(List<AutreDocumentFiscalView> autresDocumentsFiscauxSuivis) {
		this.autresDocumentsFiscauxSuivis = autresDocumentsFiscauxSuivis;
	}

	public List<AutreDocumentFiscalView> getAutresDocumentsFiscauxNonSuivis() {
		return autresDocumentsFiscauxNonSuivis;
	}

	public void setAutresDocumentsFiscauxNonSuivis(List<AutreDocumentFiscalView> autresDocumentsFiscauxNonSuivis) {
		this.autresDocumentsFiscauxNonSuivis = autresDocumentsFiscauxNonSuivis;
	}

	public ForFiscalView getForsPrincipalActif() {
		return forsPrincipalActif;
	}

	public void setForsPrincipalActif(ForFiscalView forsPrincipalActif) {
		this.forsPrincipalActif = forsPrincipalActif;
	}

	public List<ForFiscalView> getForsFiscaux() {
		return forsFiscaux;
	}

	public void setForsFiscaux(List<ForFiscalView> forsFiscaux) {
		this.forsFiscaux = forsFiscaux;
	}

	@NotNull
	public List<ForFiscalView> getForsFiscauxPrincipaux() {
		return extract(forsFiscaux, ForFiscalView::isPrincipal);
	}

	@NotNull
	public List<ForFiscalView> getForsFiscauxSecondaires() {
		return extract(forsFiscaux, ForFiscalView::isSecondaire);
	}

	@NotNull
	public List<ForFiscalView> getAutresForsFiscaux() {
		return extract(forsFiscaux, ff -> !ff.isPrincipal() && !ff.isSecondaire());
	}

	@NotNull
	private static <T> List<T> extract(List<T> source, Predicate<? super T> predicate) {
		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}
		return source.stream()
				.filter(predicate)
				.collect(Collectors.toList());
	}

	public boolean isWithForIBC() {
		final List<ForFiscalView> forsIBC = extract(forsFiscaux, ff -> !ff.isAnnule() && ff.getGenreImpot() == GenreImpot.BENEFICE_CAPITAL);
		return !forsIBC.isEmpty();
	}

	public boolean isWithSituationsFamille() {
		return withSituationsFamille;
	}

	public void setWithSituationsFamille(boolean withSituationsFamille) {
		this.withSituationsFamille = withSituationsFamille;
	}

	public List<SituationFamilleView> getSituationsFamille() {
		return situationsFamille;
	}

	public void setSituationsFamille(List<SituationFamilleView> situationsFamille) {
		this.situationsFamille = situationsFamille;
	}

	public String getSituationsFamilleEnErreurMessage() {
		return situationsFamilleEnErreurMessage;
	}

	public void setSituationsFamilleEnErreurMessage(String situationsFamilleEnErreurMessage) {
		this.situationsFamilleEnErreurMessage = situationsFamilleEnErreurMessage;
	}

	public TiersGeneralView getTiersGeneral() {
		return tiersGeneral;
	}

	public void setTiersGeneral(TiersGeneralView tiersGeneral) {
		this.tiersGeneral = tiersGeneral;
	}

	public List<AdresseView> getAdressesEnErreur() {
		return adressesEnErreur;
	}

	public void setAdressesEnErreur(@Nullable List<AdresseView> adressesEnErreur) {
		this.adressesEnErreur = adressesEnErreur;
	}

	public String getAdressesEnErreurMessage() {
		return adressesEnErreurMessage;
	}

	public void setAdressesEnErreurMessage(@Nullable String adressesEnErreurMessage) {
		this.adressesEnErreurMessage = adressesEnErreurMessage;
	}

	public List<DeclarationImpotView> getDis() {
		return dis;
	}

	public void setDis(List<DeclarationImpotView> dis) {
		this.dis = dis;
	}

	public List<QuestionnaireSNCView> getQuestionnairesSNC() {
		return questionnairesSNC;
	}

	public void setQuestionnairesSNC(List<QuestionnaireSNCView> questionnairesSNC) {
		this.questionnairesSNC = questionnairesSNC;
	}

	public List<MouvementDetailView> getMouvements() {
		return mouvements;
	}

	public void setMouvements(List<MouvementDetailView> mouvements) {
		this.mouvements = mouvements;
	}

	public boolean isAllowed() {
		return isAllowed;
	}

	public void setAllowed(boolean isAllowed) {
		this.isAllowed = isAllowed;
	}

	public List<RapportView> getContribuablesAssocies() {
		return contribuablesAssocies;
	}

	public void setContribuablesAssocies(List<RapportView> contribuablesAssocies) {
		this.contribuablesAssocies = contribuablesAssocies;
	}

	public boolean isAddContactISAllowed() {
		return addContactISAllowed;
	}

	public void setAddContactISAllowed(boolean addContactISAllowed) {
		this.addContactISAllowed = addContactISAllowed;
	}

	public void setHistoriqueAdressesCiviles(List<AdresseCivilView> historiqueAdressesCiviles) {
		this.historiqueAdressesCiviles = historiqueAdressesCiviles;
	}

	public List<AdresseCivilView> getHistoriqueAdressesCiviles() {
		return historiqueAdressesCiviles;
	}

	public String getExceptionAdresseCiviles() {
		return exceptionAdresseCiviles;
	}

	public void setExceptionAdresseCiviles(String exceptionAdresseCiviles) {
		this.exceptionAdresseCiviles = exceptionAdresseCiviles;
	}

	public void setHistoriqueAdressesCivilesConjoint(List<AdresseCivilView> historiqueAdressesCivilesConjoint) {
		this.historiqueAdressesCivilesConjoint = historiqueAdressesCivilesConjoint;
	}

	public List<AdresseCivilView> getHistoriqueAdressesCivilesConjoint() {
		return historiqueAdressesCivilesConjoint;
	}

	public String getExceptionAdresseCivilesConjoint() {
		return exceptionAdresseCivilesConjoint;
	}

	public void setExceptionAdresseCivilesConjoint(String exceptionAdresseCivilesConjoint) {
		this.exceptionAdresseCivilesConjoint = exceptionAdresseCivilesConjoint;
	}

	public String getNomPrenomPrincipal() {
		return nomPrenomPrincipal;
	}

	public void setNomPrenomPrincipal(String nomPrenomPrincipal) {
		this.nomPrenomPrincipal = nomPrenomPrincipal;
	}

	public String getNomPrenomConjoint() {
		return nomPrenomConjoint;
	}

	public void setNomPrenomConjoint(String nomPrenomConjoint) {
		this.nomPrenomConjoint = nomPrenomConjoint;
	}

	public List<PeriodiciteView> getPeriodicites() {
		return periodicites;
	}

	public void setPeriodicites(List<PeriodiciteView> periodicites) {
		this.periodicites = periodicites;
	}

	public PeriodiciteView getPeriodicite() {
		return periodicite;
	}

	public void setPeriodicite(PeriodiciteView periodicite) {
		this.periodicite = periodicite;
	}

	public LogicielView getLogiciel() {
		return logiciel;
	}

	public void setLogiciel(LogicielView logiciel) {
		this.logiciel = logiciel;
	}

	public List<DecisionAciView> getDecisionsAci() {
		return decisionsAci;
	}

	public void setDecisionsAci(List<DecisionAciView> decisionsAci) {
		this.decisionsAci = decisionsAci;
	}

	public boolean isDecisionRecente() {
		return decisionRecente;
	}

	public void setDecisionRecente(boolean decisionRecente) {
		this.decisionRecente = decisionRecente;
	}

	public int getNombreElementsTable() {
		return nombreElementsTable;
	}

	public void setNombreElementsTable(int nombreElementsTable) {
		this.nombreElementsTable = nombreElementsTable;
	}

	public boolean isForsPrincipauxPagines() {
		return forsPrincipauxPagines;
	}

	public void setForsPrincipauxPagines(boolean forsPrincipauxPagines) {
		this.forsPrincipauxPagines = forsPrincipauxPagines;
	}

	public boolean isForsSecondairesPagines() {
		return forsSecondairesPagines;
	}

	public void setForsSecondairesPagines(boolean forsSecondairesPagines) {
		this.forsSecondairesPagines = forsSecondairesPagines;
	}

	public boolean isAutresForsPagines() {
		return autresForsPagines;
	}

	public void setAutresForsPagines(boolean autresForsPagines) {
		this.autresForsPagines = autresForsPagines;
	}

	public List<MandataireCourrierView> getMandatairesCourrier() {
		return mandatairesCourrier;
	}

	public void setMandatairesCourrier(List<MandataireCourrierView> mandatairesCourrier) {
		this.mandatairesCourrier = mandatairesCourrier;
	}

	public List<MandatairePerceptionView> getMandatairesPerception() {
		return mandatairesPerception;
	}

	public void setMandatairesPerception(List<MandatairePerceptionView> mandatairesPerception) {
		this.mandatairesPerception = mandatairesPerception;
	}

	public AccesMandatairesView getAccesMandataire() {
		return accesMandataire;
	}

	public void setAccesMandataire(AccesMandatairesView accesMandataire) {
		this.accesMandataire = accesMandataire;
	}

	public boolean isShowOngletMandataire() {
		return accesMandataire != null && accesMandataire.hasAnything();
	}

	public List<EtiquetteTiersView> getEtiquettes() {
		return etiquettes;
	}

	public void setEtiquettes(List<EtiquetteTiersView> etiquettes) {
		this.etiquettes = etiquettes;
	}

	public List<EtiquetteTiersView> getEtiquettesConjoint() {
		return etiquettesConjoint;
	}

	public void setEtiquettesConjoint(List<EtiquetteTiersView> etiquettesConjoint) {
		this.etiquettesConjoint = etiquettesConjoint;
	}

	public boolean isCivilSousControleACI() {
		return civilSousControleACI;
	}

	public void setCivilSousControleACI(boolean civilSousControleACI) {
		this.civilSousControleACI = civilSousControleACI;
	}

	public String getExceptionDonneesCiviles() {
		return exceptionDonneesCiviles;
	}

	public void setExceptionDonneesCiviles(String exceptionDonneesCiviles) {
		this.exceptionDonneesCiviles = exceptionDonneesCiviles;
	}

	public List<CommuneView> getCommunesImmeubles() {
		return communesImmeubles;
	}

	public void setCommunesImmeubles(List<CommuneView> communesImmeubles) {
		this.communesImmeubles = communesImmeubles;
	}

	public void setBouclementsRenseignes(boolean bouclementsRenseignes) {
		this.bouclementsRenseignes = bouclementsRenseignes;
	}

	public boolean isBouclementsRenseignes() {
		return bouclementsRenseignes;
	}
}