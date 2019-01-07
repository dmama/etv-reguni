package ch.vd.unireg.param.view;

import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.parametrage.ParametreEnum;

/**
 * Classe mappant le formulaire de saisie de /param/application.jsp
 *
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ParamApplicationView {
//	private static final Logger LOGGER = LoggerFactory.getLogger(ParamApplicationView.class);

	public enum Action {
		save,
		reset
	}
	private Action action;

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	private String noel;
	private String nouvelAn;
	private String lendemainNouvelAn;
	private String feteNationale;
	private String premierePeriodeFiscalePersonnesPhysiques;
	private String premierePeriodeFiscalePersonnesMorales;
	private String premierePeriodeFiscaleDeclarationPersonnesMorales;
	private String anneeMinimaleForDebiteur;
	private String delaiAttenteDeclarationImpotPersonneDecedee;
	private String delaiRetourDeclarationImpotPPEmiseManuellement;
	private String delaiRetourDeclarationImpotPMEmiseManuellement;
	private String delaiEnvoiSommationDeclarationImpotPP;
	private String delaiEnvoiSommationDeclarationImpotPM;
	private String delaiEcheanceSommationDeclarationImpotPP;
	private String delaiEcheanceSommationDeclarationImpotPM;
	private String jourDuMoisEnvoiListesRecapitulatives;
	private String delaiCadevImpressionDeclarationImpot;
	private String delaiCadevImpressionListesRecapitulatives;
	private String delaiCadevImpressionLettreBienvenue;
	private String delaiCadevImpressionQuestionnaireSNC;
	private String delaiCadevImpressionDemandeDegrevementICI;
	private String nbMaxParListe;
	private String nbMaxParPage;
	private String delaiRetourListeRecapitulative;
	private String delaiEnvoiSommationListeRecapitulative;
	private String delaiRetourSommationListeRecapitulative;
	private String delaiEcheanceSommationListeRecapitualtive;
	private String delaiRetentionRapportTravailInactif;
	private String dateExclusionDecedeEnvoiDI;
	private String ageRentierFemme;
	private String ageRentierHomme;
	private String delaiMinimalRetourDeclarationImpotPM;
	private String tailleTrouAssujettissementPourNouvelleLettreBienvenue;
	private String delaiEnvoiRappelLettreBienvenue;
	private String delaiRetourLettreBienvenue;
	private String dateDebutEnvoiLettresBienvenue;
	private String dateLimiteEnvoiMasseDeclarationsUtilitePublique;
	private String delaiRetourQuestionnaireSNCEmisManuellement;
	private String delaiEnvoiRappelQuestionnaireSNC;
	private String dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI;
	private String delaiRetourDemandeDegrevementICI;
	private String delaiEnvoiRappelDemandeDegrevementICI;

	public ParamApplicationView() {}

	public ParamApplicationView(ParametreAppService service) {
		this.noel = ParametreEnum.noel.convertirValeurTypeeVersString(service.getNoel());
		this.nouvelAn = ParametreEnum.nouvelAn.convertirValeurTypeeVersString(service.getNouvelAn());
		this.lendemainNouvelAn = ParametreEnum.lendemainNouvelAn.convertirValeurTypeeVersString(service.getLendemainNouvelAn());
		this.feteNationale = ParametreEnum.feteNationale.convertirValeurTypeeVersString(service.getFeteNationale());
		this.premierePeriodeFiscalePersonnesPhysiques = ParametreEnum.premierePeriodeFiscalePersonnesPhysiques.convertirValeurTypeeVersString(service.getPremierePeriodeFiscalePersonnesPhysiques());
		this.premierePeriodeFiscalePersonnesMorales = ParametreEnum.premierePeriodeFiscalePersonnesMorales.convertirValeurTypeeVersString(service.getPremierePeriodeFiscalePersonnesMorales());
		this.premierePeriodeFiscaleDeclarationPersonnesMorales = ParametreEnum.premierePeriodeFiscaleDeclarationPersonnesMorales.convertirValeurTypeeVersString(service.getPremierePeriodeFiscaleDeclarationsPersonnesMorales());
		this.anneeMinimaleForDebiteur = ParametreEnum.anneeMinimaleForDebiteur.convertirValeurTypeeVersString(service.getAnneeMinimaleForDebiteur());
		this.delaiAttenteDeclarationImpotPersonneDecedee = ParametreEnum.delaiAttenteDeclarationImpotPersonneDecedee.convertirValeurTypeeVersString(service.getDelaiAttenteDeclarationImpotPersonneDecedee());
		this.delaiRetourDeclarationImpotPPEmiseManuellement = ParametreEnum.delaiRetourDeclarationImpotPPEmiseManuellement.convertirValeurTypeeVersString(service.getDelaiRetourDeclarationImpotPPEmiseManuellement());
		this.delaiRetourDeclarationImpotPMEmiseManuellement = ParametreEnum.delaiRetourDeclarationImpotPMEmiseManuellement.convertirValeurTypeeVersString(service.getDelaiRetourDeclarationImpotPMEmiseManuellement());
		this.delaiEnvoiSommationDeclarationImpotPP = ParametreEnum.delaiEnvoiSommationDeclarationImpotPP.convertirValeurTypeeVersString(service.getDelaiEnvoiSommationDeclarationImpotPP());
		this.delaiEcheanceSommationDeclarationImpotPP = ParametreEnum.delaiEcheanceSommationDeclarationImpotPP.convertirValeurTypeeVersString(service.getDelaiEcheanceSommationDeclarationImpotPP());
		this.jourDuMoisEnvoiListesRecapitulatives = ParametreEnum.jourDuMoisEnvoiListesRecapitulatives.convertirValeurTypeeVersString(service.getJourDuMoisEnvoiListesRecapitulatives());
		this.delaiCadevImpressionDeclarationImpot = ParametreEnum.delaiCadevImpressionDeclarationImpot.convertirValeurTypeeVersString(service.getDelaiCadevImpressionDeclarationImpot());
		this.delaiCadevImpressionListesRecapitulatives = ParametreEnum.delaiCadevImpressionListesRecapitulatives.convertirValeurTypeeVersString(service.getDelaiCadevImpressionListesRecapitulatives());
		this.delaiCadevImpressionLettreBienvenue = ParametreEnum.delaiCadevImpressionLettreBienvenue.convertirValeurTypeeVersString(service.getDelaiCadevImpressionLettreBienvenue());
		this.delaiCadevImpressionQuestionnaireSNC = ParametreEnum.delaiImpressionCadev.convertirValeurTypeeVersString(service.getDateExpeditionDelaiImpressionCadev());
		this.delaiCadevImpressionDemandeDegrevementICI = ParametreEnum.delaiCadevImpressionDemandeDegrevementICI.convertirValeurTypeeVersString(service.getDelaiCadevImpressionDemandeDegrevementICI());
		this.nbMaxParListe = ParametreEnum.nbMaxParListe.convertirValeurTypeeVersString(service.getNbMaxParListe());
		this.nbMaxParPage = ParametreEnum.nbMaxParPage.convertirValeurTypeeVersString(service.getNbMaxParPage());
		this.delaiRetourListeRecapitulative = ParametreEnum.delaiRetourListeRecapitulative.convertirValeurTypeeVersString(service.getDelaiRetourListeRecapitulative());
		this.delaiEnvoiSommationListeRecapitulative = ParametreEnum.delaiEnvoiSommationListeRecapitulative.convertirValeurTypeeVersString(service.getDelaiEnvoiSommationListeRecapitulative());
		this.delaiRetourSommationListeRecapitulative = ParametreEnum.delaiRetourSommationListeRecapitulative.convertirValeurTypeeVersString(service.getDelaiRetourSommationListeRecapitulative());
		this.delaiEcheanceSommationListeRecapitualtive = ParametreEnum.delaiEcheanceSommationListeRecapitualtive.convertirValeurTypeeVersString(service.getDelaiEcheanceSommationListeRecapitulative());
		this.delaiRetentionRapportTravailInactif = ParametreEnum.delaiRetentionRapportTravailInactif.convertirValeurTypeeVersString(service.getDelaiRetentionRapportTravailInactif());
		this.dateExclusionDecedeEnvoiDI = ParametreEnum.dateExclusionDecedeEnvoiDI.convertirValeurTypeeVersString(service.getDateExclusionDecedeEnvoiDI());
		this.ageRentierFemme = ParametreEnum.ageRentierFemme.convertirValeurTypeeVersString(service.getAgeRentierFemme());
		this.ageRentierHomme = ParametreEnum.ageRentierHomme.convertirValeurTypeeVersString(service.getAgeRentierHomme());
		this.delaiMinimalRetourDeclarationImpotPM = ParametreEnum.delaiMinimalRetourDeclarationImpotPM.convertirValeurTypeeVersString(service.getDelaiMinimalRetourDeclarationImpotPM());
		this.delaiEnvoiSommationDeclarationImpotPM = ParametreEnum.delaiEnvoiSommationDeclarationImpotPM.convertirValeurTypeeVersString(service.getDelaiEnvoiSommationDeclarationImpotPM());
		this.delaiEcheanceSommationDeclarationImpotPM = ParametreEnum.delaiEcheanceSommationDeclarationImpotPM.convertirValeurTypeeVersString(service.getDelaiEcheanceSommationDeclarationImpotPM());
		this.tailleTrouAssujettissementPourNouvelleLettreBienvenue = ParametreEnum.tailleTrouAssujettissementPourNouvelleLettreBienvenue.convertirValeurTypeeVersString(service.getTailleTrouAssujettissementPourNouvelleLettreBienvenue());
		this.delaiEnvoiRappelLettreBienvenue = ParametreEnum.delaiEnvoiRappelLettreBienvenue.convertirValeurTypeeVersString(service.getDelaiEnvoiRappelLettreBienvenue());
		this.delaiRetourLettreBienvenue = ParametreEnum.delaiRetourLettreBienvenue.convertirValeurTypeeVersString(service.getDelaiRetourLettreBienvenue());
		this.dateDebutEnvoiLettresBienvenue = ParametreEnum.dateDebutEnvoiLettresBienvenue.convertirValeurTypeeVersString(service.getDateDebutEnvoiLettresBienvenue());
		this.dateLimiteEnvoiMasseDeclarationsUtilitePublique = ParametreEnum.dateLimiteEnvoiMasseDeclarationsUtilitePublique.convertirValeurTypeeVersString(service.getDateLimiteEnvoiMasseDeclarationsUtilitePublique());
		this.delaiRetourQuestionnaireSNCEmisManuellement = ParametreEnum.delaiRetourQuestionnaireSNCEmisManuellement.convertirValeurTypeeVersString(service.getDelaiRetourQuestionnaireSNCEmisManuellement());
		this.delaiEnvoiRappelQuestionnaireSNC = ParametreEnum.delaiEnvoiRappelQuestionnaireSNC.convertirValeurTypeeVersString(service.getDelaiEnvoiRappelQuestionnaireSNC());
		this.dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI = ParametreEnum.dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI.convertirValeurTypeeVersString(service.getDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI());
		this.delaiRetourDemandeDegrevementICI = ParametreEnum.delaiRetourDemandeDegrevementICI.convertirValeurTypeeVersString(service.getDelaiRetourDemandeDegrevementICI());
		this.delaiEnvoiRappelDemandeDegrevementICI = ParametreEnum.delaiEnvoiRappelDemandeDegrevementICI.convertirValeurTypeeVersString(service.getDelaiEnvoiRappelDemandeDegrevementICI());
	}

	public void saveTo(ParametreAppService service) {
		service.setNoel((Integer[]) ParametreEnum.noel.convertirStringVersValeurTypee(this.noel));
		service.setNouvelAn((Integer[]) ParametreEnum.nouvelAn.convertirStringVersValeurTypee(this.nouvelAn));
		service.setLendemainNouvelAn((Integer[]) ParametreEnum.lendemainNouvelAn.convertirStringVersValeurTypee(this.lendemainNouvelAn));
		service.setFeteNationale((Integer[]) ParametreEnum.feteNationale.convertirStringVersValeurTypee(this.feteNationale));
		service.setPremierePeriodeFiscalePersonnesPhysiques((Integer) ParametreEnum.premierePeriodeFiscalePersonnesPhysiques.convertirStringVersValeurTypee(this.premierePeriodeFiscalePersonnesPhysiques));
		service.setPremierePeriodeFiscalePersonnesMorales((Integer) ParametreEnum.premierePeriodeFiscalePersonnesMorales.convertirStringVersValeurTypee(this.premierePeriodeFiscalePersonnesMorales));
		service.setPremierePeriodeFiscaleDeclarationsPersonnesMorales((Integer) ParametreEnum.premierePeriodeFiscaleDeclarationPersonnesMorales.convertirStringVersValeurTypee(this.premierePeriodeFiscaleDeclarationPersonnesMorales));
		service.setAnneeMinimaleForDebiteur((Integer) ParametreEnum.anneeMinimaleForDebiteur.convertirStringVersValeurTypee(this.anneeMinimaleForDebiteur));
		service.setDelaiAttenteDeclarationImpotPersonneDecedee((Integer) ParametreEnum.delaiAttenteDeclarationImpotPersonneDecedee.convertirStringVersValeurTypee(this.delaiAttenteDeclarationImpotPersonneDecedee));
		service.setDelaiRetourDeclarationImpotPPEmiseManuellement((Integer) ParametreEnum.delaiRetourDeclarationImpotPPEmiseManuellement.convertirStringVersValeurTypee(this.delaiRetourDeclarationImpotPPEmiseManuellement));
		service.setDelaiRetourDeclarationImpotPMEmiseManuellement((Integer) ParametreEnum.delaiRetourDeclarationImpotPMEmiseManuellement.convertirStringVersValeurTypee(this.delaiRetourDeclarationImpotPMEmiseManuellement));
		service.setDelaiEnvoiSommationDeclarationImpotPP((Integer) ParametreEnum.delaiEnvoiSommationDeclarationImpotPP.convertirStringVersValeurTypee(this.delaiEnvoiSommationDeclarationImpotPP));
		service.setDelaiEcheanceSommationDeclarationImpotPP((Integer) ParametreEnum.delaiEcheanceSommationDeclarationImpotPP.convertirStringVersValeurTypee(this.delaiEcheanceSommationDeclarationImpotPP));
		service.setJourDuMoisEnvoiListesRecapitulatives((Integer) ParametreEnum.jourDuMoisEnvoiListesRecapitulatives.convertirStringVersValeurTypee(this.jourDuMoisEnvoiListesRecapitulatives));
		service.setDelaiCadevImpressionDeclarationImpot((Integer) ParametreEnum.delaiCadevImpressionDeclarationImpot.convertirStringVersValeurTypee(this.delaiCadevImpressionDeclarationImpot));
		service.setDelaiCadevImpressionListesRecapitulatives((Integer) ParametreEnum.delaiCadevImpressionListesRecapitulatives.convertirStringVersValeurTypee(this.delaiCadevImpressionListesRecapitulatives));
		service.setDelaiCadevImpressionLettreBienvenue((Integer) ParametreEnum.delaiCadevImpressionLettreBienvenue.convertirStringVersValeurTypee(this.delaiCadevImpressionLettreBienvenue));
		service.setDelaiCadevImpressionQuestionnaireSNC((Integer) ParametreEnum.delaiImpressionCadev.convertirStringVersValeurTypee(this.delaiCadevImpressionQuestionnaireSNC));
		service.setDelaiCadevImpressionDemandeDegrevementICI((Integer) ParametreEnum.delaiCadevImpressionDemandeDegrevementICI.convertirStringVersValeurTypee(this.delaiCadevImpressionDemandeDegrevementICI));
		service.setNbMaxParListe((Integer) ParametreEnum.nbMaxParListe.convertirStringVersValeurTypee(this.nbMaxParListe));
		service.setNbMaxParPage((Integer) ParametreEnum.nbMaxParPage.convertirStringVersValeurTypee(this.nbMaxParPage));
		service.setDelaiRetourListeRecapitulative((Integer) ParametreEnum.delaiRetourListeRecapitulative.convertirStringVersValeurTypee(this.delaiRetourListeRecapitulative));
		service.setDelaiEnvoiSommationListeRecapitulative((Integer) ParametreEnum.delaiEnvoiSommationListeRecapitulative.convertirStringVersValeurTypee(this.delaiEnvoiSommationListeRecapitulative));
		service.setDelaiRetourSommationListeRecapitulative((Integer) ParametreEnum.delaiRetourSommationListeRecapitulative.convertirStringVersValeurTypee(this.delaiRetourSommationListeRecapitulative));
		service.setDelaiEcheanceSommationListeRecapitualtive((Integer) ParametreEnum.delaiEcheanceSommationListeRecapitualtive.convertirStringVersValeurTypee(this.delaiEcheanceSommationListeRecapitualtive));
		service.setDelaiRetentionRapportTravailInactif((Integer) ParametreEnum.delaiRetentionRapportTravailInactif.convertirStringVersValeurTypee(this.delaiRetentionRapportTravailInactif));
		service.setDateExclusionDecedeEnvoiDI((Integer[])ParametreEnum.dateExclusionDecedeEnvoiDI.convertirStringVersValeurTypee(this.dateExclusionDecedeEnvoiDI));
		service.setAgeRentierFemme((Integer) ParametreEnum.ageRentierFemme.convertirStringVersValeurTypee(this.ageRentierFemme));
		service.setAgeRentierHomme((Integer) ParametreEnum.ageRentierHomme.convertirStringVersValeurTypee(this.ageRentierHomme));
		service.setDelaiMinimalRetourDeclarationImpotPM((Integer) ParametreEnum.delaiMinimalRetourDeclarationImpotPM.convertirStringVersValeurTypee(this.delaiMinimalRetourDeclarationImpotPM));
		service.setDelaiEnvoiSommationDeclarationImpotPM((Integer) ParametreEnum.delaiEnvoiSommationDeclarationImpotPM.convertirStringVersValeurTypee(this.delaiEnvoiSommationDeclarationImpotPM));
		service.setDelaiEcheanceSommationDeclarationImpotPM((Integer) ParametreEnum.delaiEcheanceSommationDeclarationImpotPM.convertirStringVersValeurTypee(this.delaiEcheanceSommationDeclarationImpotPM));
		service.setTailleTrouAssujettissementPourNouvelleLettreBienvenue((Integer) ParametreEnum.tailleTrouAssujettissementPourNouvelleLettreBienvenue.convertirStringVersValeurTypee(this.tailleTrouAssujettissementPourNouvelleLettreBienvenue));
		service.setDelaiEnvoiRappelLettreBienvenue((Integer) ParametreEnum.delaiEnvoiRappelLettreBienvenue.convertirStringVersValeurTypee(this.delaiEnvoiRappelLettreBienvenue));
		service.setDelaiRetourLettreBienvenue((Integer) ParametreEnum.delaiRetourLettreBienvenue.convertirStringVersValeurTypee(this.delaiRetourLettreBienvenue));
		service.setDateDebutEnvoiLettresBienvenue((Integer[]) ParametreEnum.dateDebutEnvoiLettresBienvenue.convertirStringVersValeurTypee(this.dateDebutEnvoiLettresBienvenue));
		service.setDateLimiteEnvoiMasseDeclarationsUtilitePublique((Integer[]) ParametreEnum.dateLimiteEnvoiMasseDeclarationsUtilitePublique.convertirStringVersValeurTypee(this.dateLimiteEnvoiMasseDeclarationsUtilitePublique));
		service.setDelaiRetourQuestionnaireSNCEmisManuellement((Integer) ParametreEnum.delaiRetourQuestionnaireSNCEmisManuellement.convertirStringVersValeurTypee(this.delaiRetourQuestionnaireSNCEmisManuellement));
		service.setDelaiEnvoiRappelQuestionnaireSNC((Integer) ParametreEnum.delaiEnvoiRappelQuestionnaireSNC.convertirStringVersValeurTypee(this.delaiEnvoiRappelQuestionnaireSNC));
		service.setDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI((Integer[]) ParametreEnum.dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI.convertirStringVersValeurTypee(this.dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI));
		service.setDelaiRetourDemandeDegrevementICI((Integer) ParametreEnum.delaiRetourDemandeDegrevementICI.convertirStringVersValeurTypee(this.delaiRetourDemandeDegrevementICI));
		service.setDelaiEnvoiRappelDemandeDegrevementICI((Integer) ParametreEnum.delaiEnvoiRappelDemandeDegrevementICI.convertirStringVersValeurTypee(this.delaiEnvoiRappelDemandeDegrevementICI));
	}

	public String getNoel() {
		return noel;
	}

	public void setNoel(String noel) {
		this.noel = noel;
	}

	public String getNouvelAn() {
		return nouvelAn;
	}

	public void setNouvelAn(String nouvelAn) {
		this.nouvelAn = nouvelAn;
	}

	public String getLendemainNouvelAn() {
		return lendemainNouvelAn;
	}

	public void setLendemainNouvelAn(String lendemainNouvelAn) {
		this.lendemainNouvelAn = lendemainNouvelAn;
	}

	public String getFeteNationale() {
		return feteNationale;
	}

	public void setFeteNationale(String feteNationale) {
		this.feteNationale = feteNationale;
	}

	public String getPremierePeriodeFiscalePersonnesPhysiques() {
		return premierePeriodeFiscalePersonnesPhysiques;
	}

	public void setPremierePeriodeFiscalePersonnesPhysiques(String premierePeriodeFiscalePersonnesPhysiques) {
		this.premierePeriodeFiscalePersonnesPhysiques = premierePeriodeFiscalePersonnesPhysiques;
	}

	public String getPremierePeriodeFiscalePersonnesMorales() {
		return premierePeriodeFiscalePersonnesMorales;
	}

	public void setPremierePeriodeFiscalePersonnesMorales(String premierePeriodeFiscalePersonnesMorales) {
		this.premierePeriodeFiscalePersonnesMorales = premierePeriodeFiscalePersonnesMorales;
	}

	public String getPremierePeriodeFiscaleDeclarationPersonnesMorales() {
		return premierePeriodeFiscaleDeclarationPersonnesMorales;
	}

	public void setPremierePeriodeFiscaleDeclarationPersonnesMorales(String premierePeriodeFiscaleDeclarationPersonnesMorales) {
		this.premierePeriodeFiscaleDeclarationPersonnesMorales = premierePeriodeFiscaleDeclarationPersonnesMorales;
	}

	public String getAnneeMinimaleForDebiteur() {
		return anneeMinimaleForDebiteur;
	}

	public void setAnneeMinimaleForDebiteur(String anneeMinimaleForDebiteur) {
		this.anneeMinimaleForDebiteur = anneeMinimaleForDebiteur;
	}

	public String getDelaiAttenteDeclarationImpotPersonneDecedee() {
		return delaiAttenteDeclarationImpotPersonneDecedee;
	}

	public void setDelaiAttenteDeclarationImpotPersonneDecedee(String delaiAttenteDeclarationImpotPersonneDecedee) {
		this.delaiAttenteDeclarationImpotPersonneDecedee = delaiAttenteDeclarationImpotPersonneDecedee;
	}

	public String getDelaiRetourDeclarationImpotPPEmiseManuellement() {
		return delaiRetourDeclarationImpotPPEmiseManuellement;
	}

	public void setDelaiRetourDeclarationImpotPPEmiseManuellement(String delaiRetourDeclarationImpotPPEmiseManuellement) {
		this.delaiRetourDeclarationImpotPPEmiseManuellement = delaiRetourDeclarationImpotPPEmiseManuellement;
	}

	public String getDelaiRetourDeclarationImpotPMEmiseManuellement() {
		return delaiRetourDeclarationImpotPMEmiseManuellement;
	}

	public void setDelaiRetourDeclarationImpotPMEmiseManuellement(String delaiRetourDeclarationImpotPMEmiseManuellement) {
		this.delaiRetourDeclarationImpotPMEmiseManuellement = delaiRetourDeclarationImpotPMEmiseManuellement;
	}

	public String getDelaiEnvoiSommationDeclarationImpotPP() {
		return delaiEnvoiSommationDeclarationImpotPP;
	}

	public void setDelaiEnvoiSommationDeclarationImpotPP(String delaiEnvoiSommationDeclarationImpotPP) {
		this.delaiEnvoiSommationDeclarationImpotPP = delaiEnvoiSommationDeclarationImpotPP;
	}

	public String getDelaiEcheanceSommationDeclarationImpotPP() {
		return delaiEcheanceSommationDeclarationImpotPP;
	}

	public void setDelaiEcheanceSommationDeclarationImpotPP(String delaiEcheanceSommationDeclarationImpotPP) {
		this.delaiEcheanceSommationDeclarationImpotPP = delaiEcheanceSommationDeclarationImpotPP;
	}

	public String getJourDuMoisEnvoiListesRecapitulatives() {
		return jourDuMoisEnvoiListesRecapitulatives;
	}

	public void setJourDuMoisEnvoiListesRecapitulatives(String jourDuMoisEnvoiListesRecapitulatives) {
		this.jourDuMoisEnvoiListesRecapitulatives = jourDuMoisEnvoiListesRecapitulatives;
	}

	public String getDelaiCadevImpressionDeclarationImpot() {
		return delaiCadevImpressionDeclarationImpot;
	}

	public void setDelaiCadevImpressionDeclarationImpot(String delaiCadevImpressionDeclarationImpot) {
		this.delaiCadevImpressionDeclarationImpot = delaiCadevImpressionDeclarationImpot;
	}

	public String getDelaiCadevImpressionListesRecapitulatives() {
		return delaiCadevImpressionListesRecapitulatives;
	}

	public void setDelaiCadevImpressionListesRecapitulatives(String delaiCadevImpressionListesRecapitulatives) {
		this.delaiCadevImpressionListesRecapitulatives = delaiCadevImpressionListesRecapitulatives;
	}

	public String getDelaiCadevImpressionLettreBienvenue() {
		return delaiCadevImpressionLettreBienvenue;
	}

	public void setDelaiCadevImpressionLettreBienvenue(String delaiCadevImpressionLettreBienvenue) {
		this.delaiCadevImpressionLettreBienvenue = delaiCadevImpressionLettreBienvenue;
	}

	public String getDelaiCadevImpressionQuestionnaireSNC() {
		return delaiCadevImpressionQuestionnaireSNC;
	}

	public void setDelaiCadevImpressionQuestionnaireSNC(String delaiCadevImpressionQuestionnaireSNC) {
		this.delaiCadevImpressionQuestionnaireSNC = delaiCadevImpressionQuestionnaireSNC;
	}

	public String getDelaiCadevImpressionDemandeDegrevementICI() {
		return delaiCadevImpressionDemandeDegrevementICI;
	}

	public void setDelaiCadevImpressionDemandeDegrevementICI(String delaiCadevImpressionDemandeDegrevementICI) {
		this.delaiCadevImpressionDemandeDegrevementICI = delaiCadevImpressionDemandeDegrevementICI;
	}

	public String getNbMaxParListe() {
		return nbMaxParListe;
	}

	public void setNbMaxParListe(String nbMaxParListe) {
		this.nbMaxParListe = nbMaxParListe;
	}

	public String getNbMaxParPage() {
		return nbMaxParPage;
	}

	public void setNbMaxParPage(String nbMaxParPage) {
		this.nbMaxParPage = nbMaxParPage;
	}

	public String getDelaiRetourListeRecapitulative() {
		return delaiRetourListeRecapitulative;
	}

	public void setDelaiRetourListeRecapitulative(String delaiRetourListeRecapitulative) {
		this.delaiRetourListeRecapitulative = delaiRetourListeRecapitulative;
	}

	public String getDelaiEnvoiSommationListeRecapitulative() {
		return delaiEnvoiSommationListeRecapitulative;
	}

	public void setDelaiEnvoiSommationListeRecapitulative(String delaiEnvoiSommationListeRecapitulative) {
		this.delaiEnvoiSommationListeRecapitulative = delaiEnvoiSommationListeRecapitulative;
	}

	public String getDelaiRetourSommationListeRecapitulative() {
		return delaiRetourSommationListeRecapitulative;
	}

	public void setDelaiRetourSommationListeRecapitulative(String delaiRetourSommationListeRecapitulative) {
		this.delaiRetourSommationListeRecapitulative = delaiRetourSommationListeRecapitulative;
	}

	public String getDelaiEcheanceSommationListeRecapitualtive() {
		return delaiEcheanceSommationListeRecapitualtive;
	}

	public void setDelaiEcheanceSommationListeRecapitualtive(String delaiEcheanceSommationListeRecapitualtive) {
		this.delaiEcheanceSommationListeRecapitualtive = delaiEcheanceSommationListeRecapitualtive;
	}

	public String getDelaiRetentionRapportTravailInactif() {
		return delaiRetentionRapportTravailInactif;
	}

	public void setDelaiRetentionRapportTravailInactif(String delaiRetentionRapportTravailInactif) {
		this.delaiRetentionRapportTravailInactif = delaiRetentionRapportTravailInactif;
	}

	public String getDateExclusionDecedeEnvoiDI() {
		return dateExclusionDecedeEnvoiDI;
	}

	public void setDateExclusionDecedeEnvoiDI(String dateExclusionDecedeEnvoiDI) {
		this.dateExclusionDecedeEnvoiDI = dateExclusionDecedeEnvoiDI;
	}

	public String getAgeRentierFemme() {
		return ageRentierFemme;
	}

	public void setAgeRentierFemme(String ageRentierFemme) {
		this.ageRentierFemme = ageRentierFemme;
	}

	public String getAgeRentierHomme() {
		return ageRentierHomme;
	}

	public void setAgeRentierHomme(String ageRentierHomme) {
		this.ageRentierHomme = ageRentierHomme;
	}

	public String getDelaiMinimalRetourDeclarationImpotPM() {
		return delaiMinimalRetourDeclarationImpotPM;
	}

	public void setDelaiMinimalRetourDeclarationImpotPM(String delaiMinimalRetourDeclarationImpotPM) {
		this.delaiMinimalRetourDeclarationImpotPM = delaiMinimalRetourDeclarationImpotPM;
	}

	public String getDelaiEnvoiSommationDeclarationImpotPM() {
		return delaiEnvoiSommationDeclarationImpotPM;
	}

	public void setDelaiEnvoiSommationDeclarationImpotPM(String delaiEnvoiSommationDeclarationImpotPM) {
		this.delaiEnvoiSommationDeclarationImpotPM = delaiEnvoiSommationDeclarationImpotPM;
	}

	public String getDelaiEcheanceSommationDeclarationImpotPM() {
		return delaiEcheanceSommationDeclarationImpotPM;
	}

	public void setDelaiEcheanceSommationDeclarationImpotPM(String delaiEcheanceSommationDeclarationImpotPM) {
		this.delaiEcheanceSommationDeclarationImpotPM = delaiEcheanceSommationDeclarationImpotPM;
	}

	public String getTailleTrouAssujettissementPourNouvelleLettreBienvenue() {
		return tailleTrouAssujettissementPourNouvelleLettreBienvenue;
	}

	public void setTailleTrouAssujettissementPourNouvelleLettreBienvenue(String tailleTrouAssujettissementPourNouvelleLettreBienvenue) {
		this.tailleTrouAssujettissementPourNouvelleLettreBienvenue = tailleTrouAssujettissementPourNouvelleLettreBienvenue;
	}

	public String getDelaiRetourLettreBienvenue() {
		return delaiRetourLettreBienvenue;
	}

	public void setDelaiRetourLettreBienvenue(String delaiRetourLettreBienvenue) {
		this.delaiRetourLettreBienvenue = delaiRetourLettreBienvenue;
	}

	public String getDateDebutEnvoiLettresBienvenue() {
		return dateDebutEnvoiLettresBienvenue;
	}

	public void setDateDebutEnvoiLettresBienvenue(String dateDebutEnvoiLettresBienvenue) {
		this.dateDebutEnvoiLettresBienvenue = dateDebutEnvoiLettresBienvenue;
	}

	public String getDelaiEnvoiRappelLettreBienvenue() {
		return delaiEnvoiRappelLettreBienvenue;
	}

	public void setDelaiEnvoiRappelLettreBienvenue(String delaiEnvoiRappelLettreBienvenue) {
		this.delaiEnvoiRappelLettreBienvenue = delaiEnvoiRappelLettreBienvenue;
	}

	public String getDateLimiteEnvoiMasseDeclarationsUtilitePublique() {
		return dateLimiteEnvoiMasseDeclarationsUtilitePublique;
	}

	public void setDateLimiteEnvoiMasseDeclarationsUtilitePublique(String dateLimiteEnvoiMasseDeclarationsUtilitePublique) {
		this.dateLimiteEnvoiMasseDeclarationsUtilitePublique = dateLimiteEnvoiMasseDeclarationsUtilitePublique;
	}

	public String getDelaiRetourQuestionnaireSNCEmisManuellement() {
		return delaiRetourQuestionnaireSNCEmisManuellement;
	}

	public void setDelaiRetourQuestionnaireSNCEmisManuellement(String delaiRetourQuestionnaireSNCEmisManuellement) {
		this.delaiRetourQuestionnaireSNCEmisManuellement = delaiRetourQuestionnaireSNCEmisManuellement;
	}

	public String getDelaiEnvoiRappelQuestionnaireSNC() {
		return delaiEnvoiRappelQuestionnaireSNC;
	}

	public void setDelaiEnvoiRappelQuestionnaireSNC(String delaiEnvoiRappelQuestionnaireSNC) {
		this.delaiEnvoiRappelQuestionnaireSNC = delaiEnvoiRappelQuestionnaireSNC;
	}

	public String getDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI() {
		return dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI;
	}

	public void setDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI(String dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI) {
		this.dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI = dateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI;
	}

	public String getDelaiRetourDemandeDegrevementICI() {
		return delaiRetourDemandeDegrevementICI;
	}

	public void setDelaiRetourDemandeDegrevementICI(String delaiRetourDemandeDegrevementICI) {
		this.delaiRetourDemandeDegrevementICI = delaiRetourDemandeDegrevementICI;
	}

	public String getDelaiEnvoiRappelDemandeDegrevementICI() {
		return delaiEnvoiRappelDemandeDegrevementICI;
	}

	public void setDelaiEnvoiRappelDemandeDegrevementICI(String delaiEnvoiRappelDemandeDegrevementICI) {
		this.delaiEnvoiRappelDemandeDegrevementICI = delaiEnvoiRappelDemandeDegrevementICI;
	}
}
