package ch.vd.uniregctb.param.view;

import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.parametrage.ParametreEnum;

/**
 * Classe mappant le formulaire de saisie de /param/application.jsp
 *
 * @author xsifnr
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ParamApplicationView {

//	private static final Logger LOGGER = Logger.getLogger(ParamApplicationView.class);

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
	private String premierePeriodeFiscale;
	private String anneeMinimaleForDebiteur;
	private String delaiAttenteDeclarationImpotPersonneDecedee;
	private String delaiRetourDeclarationImpotEmiseManuellement;
	private String delaiEnvoiSommationDeclarationImpot;
	private String delaiEcheanceSommationDeclarationImpot;
	private String jourDuMoisEnvoiListesRecapitulatives;
	private String delaiCadevImpressionDeclarationImpot;
	private String delaiCadevImpressionListesRecapitulatives;
	private String nbMaxParListe;
	private String nbMaxParPage;
	private String delaiRetourListeRecapitulative;
	private String delaiEnvoiSommationListeRecapitulative;
	private String delaiRetourSommationListeRecapitulative;
	private String delaiEcheanceSommationListeRecapitualtive;
	private String delaiRetentionRapportTravailInactif;
	private String dateExclusionDecedeEnvoiDI;

	public ParamApplicationView(ParametreAppService service) {
		this.noel = ParametreEnum.noel.convertirValeurTypeeVersString(service.getNoel());
		this.nouvelAn = ParametreEnum.nouvelAn.convertirValeurTypeeVersString(service.getNouvelAn());
		this.lendemainNouvelAn = ParametreEnum.lendemainNouvelAn.convertirValeurTypeeVersString(service.getLendemainNouvelAn());
		this.feteNationale = ParametreEnum.feteNationale.convertirValeurTypeeVersString(service.getFeteNationale());
		this.premierePeriodeFiscale = ParametreEnum.premierePeriodeFiscale.convertirValeurTypeeVersString(service.getPremierePeriodeFiscale());
		this.anneeMinimaleForDebiteur = ParametreEnum.anneeMinimaleForDebiteur.convertirValeurTypeeVersString(service.getAnneeMinimaleForDebiteur());
		this.delaiAttenteDeclarationImpotPersonneDecedee =
				ParametreEnum.delaiAttenteDeclarationImpotPersonneDecedee.convertirValeurTypeeVersString(service.getDelaiAttenteDeclarationImpotPersonneDecedee());
		this.delaiRetourDeclarationImpotEmiseManuellement =
				ParametreEnum.delaiRetourDeclarationImpotEmiseManuellement.convertirValeurTypeeVersString(service.getDelaiRetourDeclarationImpotEmiseManuellement());
		this.delaiEnvoiSommationDeclarationImpot = ParametreEnum.delaiEnvoiSommationDeclarationImpot.convertirValeurTypeeVersString(service.getDelaiEnvoiSommationDeclarationImpot());
		this.delaiEcheanceSommationDeclarationImpot = ParametreEnum.delaiEcheanceSommationDeclarationImpot.convertirValeurTypeeVersString(service.getDelaiEcheanceSommationDeclarationImpot());
		this.jourDuMoisEnvoiListesRecapitulatives = ParametreEnum.jourDuMoisEnvoiListesRecapitulatives.convertirValeurTypeeVersString(service.getJourDuMoisEnvoiListesRecapitulatives());
		this.delaiCadevImpressionDeclarationImpot = ParametreEnum.delaiCadevImpressionDeclarationImpot.convertirValeurTypeeVersString(service.getDelaiCadevImpressionDeclarationImpot());
		this.delaiCadevImpressionListesRecapitulatives = ParametreEnum.delaiCadevImpressionListesRecapitulatives.convertirValeurTypeeVersString(service.getDelaiCadevImpressionListesRecapitulatives());
		this.nbMaxParListe = ParametreEnum.nbMaxParListe.convertirValeurTypeeVersString(service.getNbMaxParListe());
		this.nbMaxParPage = ParametreEnum.nbMaxParPage.convertirValeurTypeeVersString(service.getNbMaxParPage());
		this.delaiRetourListeRecapitulative = ParametreEnum.delaiRetourListeRecapitulative.convertirValeurTypeeVersString(service.getDelaiRetourListeRecapitulative());
		this.delaiEnvoiSommationListeRecapitulative = ParametreEnum.delaiEnvoiSommationListeRecapitulative.convertirValeurTypeeVersString(service.getDelaiEnvoiSommationListeRecapitulative());
		this.delaiRetourSommationListeRecapitulative = ParametreEnum.delaiRetourSommationListeRecapitulative.convertirValeurTypeeVersString(service.getDelaiRetourSommationListeRecapitulative());
		this.delaiEcheanceSommationListeRecapitualtive = ParametreEnum.delaiEcheanceSommationListeRecapitualtive.convertirValeurTypeeVersString(service.getDelaiEcheanceSommationListeRecapitualtive());
		this.delaiRetentionRapportTravailInactif = ParametreEnum.delaiRetentionRapportTravailInactif.convertirValeurTypeeVersString(service.getDelaiRetentionRapportTravailInactif());
		this.dateExclusionDecedeEnvoiDI = ParametreEnum.dateExclusionDecedeEnvoiDI.convertirValeurTypeeVersString(service.getDateExclusionDecedeEnvoiDI());
	}

	public void saveTo(ParametreAppService service) {
		service.setNoel((Integer[]) ParametreEnum.noel.convertirStringVersValeurTypee(this.noel));
		service.setNouvelAn((Integer[]) ParametreEnum.nouvelAn.convertirStringVersValeurTypee(this.nouvelAn));
		service.setLendemainNouvelAn((Integer[]) ParametreEnum.lendemainNouvelAn.convertirStringVersValeurTypee(this.lendemainNouvelAn));
		service.setFeteNationale((Integer[]) ParametreEnum.feteNationale.convertirStringVersValeurTypee(this.feteNationale));
		service.setPremierePeriodeFiscale((Integer) ParametreEnum.premierePeriodeFiscale.convertirStringVersValeurTypee(this.premierePeriodeFiscale));
		service.setAnneeMinimaleForDebiteur((Integer) ParametreEnum.anneeMinimaleForDebiteur.convertirStringVersValeurTypee(this.anneeMinimaleForDebiteur));
		service.setDelaiAttenteDeclarationImpotPersonneDecedee(
				(Integer) ParametreEnum.delaiAttenteDeclarationImpotPersonneDecedee.convertirStringVersValeurTypee(this.delaiAttenteDeclarationImpotPersonneDecedee));
		service.setDelaiRetourDeclarationImpotEmiseManuellement(
				(Integer) ParametreEnum.delaiRetourDeclarationImpotEmiseManuellement.convertirStringVersValeurTypee(this.delaiRetourDeclarationImpotEmiseManuellement));
		service.setDelaiEnvoiSommationDeclarationImpot((Integer) ParametreEnum.delaiEnvoiSommationDeclarationImpot.convertirStringVersValeurTypee(this.delaiEnvoiSommationDeclarationImpot));
		service.setDelaiEcheanceSommationDeclarationImpot((Integer) ParametreEnum.delaiEcheanceSommationDeclarationImpot.convertirStringVersValeurTypee(this.delaiEcheanceSommationDeclarationImpot));
		service.setJourDuMoisEnvoiListesRecapitulatives((Integer) ParametreEnum.jourDuMoisEnvoiListesRecapitulatives.convertirStringVersValeurTypee(this.jourDuMoisEnvoiListesRecapitulatives));
		service.setDelaiCadevImpressionDeclarationImpot((Integer) ParametreEnum.delaiCadevImpressionDeclarationImpot.convertirStringVersValeurTypee(this.delaiCadevImpressionDeclarationImpot));
		service.setDelaiCadevImpressionListesRecapitulatives(
				(Integer) ParametreEnum.delaiCadevImpressionListesRecapitulatives.convertirStringVersValeurTypee(this.delaiCadevImpressionListesRecapitulatives));
		service.setNbMaxParListe((Integer) ParametreEnum.nbMaxParListe.convertirStringVersValeurTypee(this.nbMaxParListe));
		service.setNbMaxParPage((Integer) ParametreEnum.nbMaxParPage.convertirStringVersValeurTypee(this.nbMaxParPage));
		service.setDelaiRetourListeRecapitulative((Integer) ParametreEnum.delaiRetourListeRecapitulative.convertirStringVersValeurTypee(this.delaiRetourListeRecapitulative));
		service.setDelaiEnvoiSommationListeRecapitulative((Integer) ParametreEnum.delaiEnvoiSommationListeRecapitulative.convertirStringVersValeurTypee(this.delaiEnvoiSommationListeRecapitulative));
		service.setDelaiRetourSommationListeRecapitulative(
				(Integer) ParametreEnum.delaiRetourSommationListeRecapitulative.convertirStringVersValeurTypee(this.delaiRetourSommationListeRecapitulative));
		service.setDelaiEcheanceSommationListeRecapitualtive(
				(Integer) ParametreEnum.delaiEcheanceSommationListeRecapitualtive.convertirStringVersValeurTypee(this.delaiEcheanceSommationListeRecapitualtive));
		service.setDelaiRetentionRapportTravailInactif((Integer) ParametreEnum.delaiRetentionRapportTravailInactif.convertirStringVersValeurTypee(this.delaiRetentionRapportTravailInactif));
		service.setDateExclusionDecedeEnvoiDI((Integer[])ParametreEnum.dateExclusionDecedeEnvoiDI.convertirStringVersValeurTypee(this.dateExclusionDecedeEnvoiDI));
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

	public String getPremierePeriodeFiscale() {
		return premierePeriodeFiscale;
	}

	public void setPremierePeriodeFiscale(String premierePeriodeFiscale) {
		this.premierePeriodeFiscale = premierePeriodeFiscale;
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

	public String getDelaiRetourDeclarationImpotEmiseManuellement() {
		return delaiRetourDeclarationImpotEmiseManuellement;
	}

	public void setDelaiRetourDeclarationImpotEmiseManuellement(String delaiRetourDeclarationImpotEmiseManuellement) {
		this.delaiRetourDeclarationImpotEmiseManuellement = delaiRetourDeclarationImpotEmiseManuellement;
	}

	public String getDelaiEnvoiSommationDeclarationImpot() {
		return delaiEnvoiSommationDeclarationImpot;
	}

	public void setDelaiEnvoiSommationDeclarationImpot(String delaiEnvoiSommationDeclarationImpot) {
		this.delaiEnvoiSommationDeclarationImpot = delaiEnvoiSommationDeclarationImpot;
	}

	public String getDelaiEcheanceSommationDeclarationImpot() {
		return delaiEcheanceSommationDeclarationImpot;
	}

	public void setDelaiEcheanceSommationDeclarationImpot(String delaiEcheanceSommationDeclarationImpot) {
		this.delaiEcheanceSommationDeclarationImpot = delaiEcheanceSommationDeclarationImpot;
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
}
