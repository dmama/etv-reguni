package ch.vd.uniregctb.param.view;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.parametrage.ParametreAppServiceImpl;
import ch.vd.uniregctb.parametrage.ParametreEnum;

/**
 * 
 * Classe mappant le formulaire de saisie de /param/application.jsp
 * 
 * @author xsifnr
 *
 */
public class ParamApplicationView {

	/**
	 * Un logger pour {@link ParamApplicationView}
	 */
	private static final Logger LOGGER = Logger.getLogger(ParamApplicationView.class);
	
	static {
		/*
		 * Verification de l'adéquation de la classe avec ParametreEnum. Cette classe doit définir une propriété JavaBean pour chaque valeur
		 * possible de ParametreEnum
		 */
		assert
			ParametreEnum.isClassCompatible(ParamApplicationView.class) : 
			ParametreEnum.getMissingPropertiesMessage(ParamApplicationView.class)
		;
		LOGGER.debug(ParamApplicationView.class.getName() + " est en adequation avec " + ParametreEnum.class.getName());
	}
	
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
	public String getDelaiAttenteDeclarationImpotPersonneDecedee() {
		return delaiAttenteDeclarationImpotPersonneDecedee;
	}
	public void setDelaiAttenteDeclarationImpotPersonneDecedee(String delaiAttenteDeclarationImpotPersonneDecedee
			) {
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
	
	public static void main(String[] args) {
		System.out.println(ParametreEnum.getMissingPropertiesMessage(ParametreAppServiceImpl.class));
		System.out.println(ParametreEnum.getMissingPropertiesMessage(ParamApplicationView.class));
		System.out.println("OK");
	}
}
