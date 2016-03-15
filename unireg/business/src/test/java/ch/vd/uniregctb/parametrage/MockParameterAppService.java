package ch.vd.uniregctb.parametrage;

/**
 * Classe de mock pour le service des paramètres applicatifs
 */
public final class MockParameterAppService implements ParametreAppService {

	private final ParametreAppContainer container = new ParametreAppContainer();

	public MockParameterAppService() {
		container.initDefaults();
	}

	@Override

	public String getDefaut(ParametreEnum param) {
		return container.getDefaut(param);
	}

	@Override
	public Integer getDelaiAttenteDeclarationImpotPersonneDecedee() {
		return container.getDelaiAttenteDeclarationImpotPersonneDecedee();
	}

	@Override
	public Integer getDelaiRetourDeclarationImpotPPEmiseManuellement() {
		return container.getDelaiRetourDeclarationImpotPPEmiseManuellement();
	}

	@Override
	public Integer getDelaiRetourDeclarationImpotPMEmiseManuellement() {
		return container.getDelaiRetourDeclarationImpotPMEmiseManuellement();
	}

	@Override
	public Integer getDelaiCadevImpressionDeclarationImpot() {
		return container.getDelaiCadevImpressionDeclarationImpot();
	}

	@Override
	public Integer getDelaiCadevImpressionListesRecapitulatives() {
		return container.getDelaiCadevImpressionListesRecapitulatives();
	}

	@Override
	public Integer getDelaiCadevImpressionLettreBienvenue() {
		return container.getDelaiCadevImpressionLettreBienvenue();
	}

	@Override
	public Integer getDelaiEcheanceSommationDeclarationImpotPP() {
		return container.getDelaiEcheanceSommationDeclarationImpotPP();
	}

	@Override
	public Integer getDelaiEcheanceSommationDeclarationImpotPM() {
		return container.getDelaiEcheanceSommationDeclarationImpotPM();
	}

	@Override
	public Integer getDelaiEcheanceSommationListeRecapitualtive() {
		return container.getDelaiEcheanceSommationListeRecapitualtive();
	}

	@Override
	public Integer getDelaiEnvoiSommationDeclarationImpotPP() {
		return container.getDelaiEnvoiSommationDeclarationImpotPP();
	}

	@Override
	public Integer getDelaiEnvoiSommationDeclarationImpotPM() {
		return container.getDelaiEnvoiSommationDeclarationImpotPM();
	}

	@Override
	public Integer getDelaiEnvoiSommationListeRecapitulative() {
		return container.getDelaiEnvoiSommationListeRecapitulative();
	}

	@Override
	public Integer getDelaiRetentionRapportTravailInactif() {
		return container.getDelaiRetentionRapportTravailInactif();
	}

	@Override
	public Integer getDelaiRetourListeRecapitulative() {
		return container.getDelaiRetourListeRecapitulative();
	}

	@Override
	public Integer getDelaiRetourSommationListeRecapitulative() {
		return container.getDelaiRetourSommationListeRecapitulative();
	}

	@Override
	public Integer getTailleTrouAssujettissementPourNouvelleLettreBienvenue() {
		return container.getTailleTrouAssujettissementPourNouvelleLettreBienvenue();
	}

	@Override
	public Integer getDelaiEnvoiRappelLettreBienvenue() {
		return container.getDelaiEnvoiRappelLettreBienvenue();
	}

	@Override
	public Integer getDelaiRetourLettreBienvenue() {
		return container.getDelaiRetourLettreBienvenue();
	}

	@Override
	public Integer[] getDateDebutEnvoiLettresBienvenue() {
		return container.getDateDebutEnvoiLettresBienvenue();
	}

	@Override
	public Integer[] getFeteNationale() {
		return container.getFeteNationale();
	}

	@Override
	public Integer getJourDuMoisEnvoiListesRecapitulatives() {
		return container.getJourDuMoisEnvoiListesRecapitulatives();
	}

	@Override
	public Integer[] getLendemainNouvelAn() {
		return container.getLendemainNouvelAn();
	}

	@Override
	public Integer getNbMaxParListe() {
		return container.getNbMaxParListe();
	}

	@Override
	public Integer getNbMaxParPage() {
		return container.getNbMaxParPage();
	}

	@Override
	public Integer[] getNoel() {
		return container.getNoel();
	}

	@Override
	public String getNom(ParametreEnum param) {
		return container.getNom(param);
	}

	@Override
	public Integer[] getNouvelAn() {
		return container.getNouvelAn();
	}

	@Override
	public Integer getPremierePeriodeFiscalePersonnesPhysiques() {
		return container.getPremierePeriodeFiscalePersonnesPhysiques();
	}

	@Override
	public Integer getPremierePeriodeFiscalePersonnesMorales() {
		return container.getPremierePeriodeFiscalePersonnesMorales();
	}

	@Override
	public Integer getPremierePeriodeFiscaleDeclarationsPersonnesMorales() {
		return container.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
	}

	@Override
	public Integer[] getDateExclusionDecedeEnvoiDI() {
		return container.getDateExclusionDecedeEnvoiDI();
	}

	@Override
	public Integer getAnneeMinimaleForDebiteur() {
		return container.getAnneeMinimaleForDebiteur();
	}

	@Override
	public Integer getAgeRentierFemme() {
		return container.getAgeRentierFemme();
	}

	@Override
	public Integer getAgeRentierHomme() {
		return container.getAgeRentierHomme();
	}

	@Override
	public Integer getDelaiMinimalRetourDeclarationImpotPM() {
		return container.getDelaiMinimalRetourDeclarationImpotPM();
	}

	@Override
	public Integer[] getDateLimiteEnvoiMasseDeclarationsUtilitePublique() {
		return container.getDateLimiteEnvoiMasseDeclarationsUtilitePublique();
	}

	@Override
	public void reset() {
		container.reset();
	}

	@Override
	public void save() {
		// on ne fait rien...
	}

	@Override
	public void setDelaiAttenteDeclarationImpotPersonneDecedee(Integer val) {
		container.setDelaiAttenteDeclarationImpotPersonneDecedee(val);
	}

	@Override
	public void setDelaiRetourDeclarationImpotPPEmiseManuellement(Integer val) {
		container.setDelaiRetourDeclarationImpotPPEmiseManuellement(val);
	}

	@Override
	public void setDelaiRetourDeclarationImpotPMEmiseManuellement(Integer val) {
		container.setDelaiRetourDeclarationImpotPMEmiseManuellement(val);
	}

	@Override
	public void setDelaiCadevImpressionDeclarationImpot(Integer val) {
		container.setDelaiCadevImpressionDeclarationImpot(val);
	}

	@Override
	public void setDelaiCadevImpressionListesRecapitulatives(Integer val) {
		container.setDelaiCadevImpressionListesRecapitulatives(val);
	}

	@Override
	public void setDelaiCadevImpressionLettreBienvenue(Integer val) {
		container.setDelaiCadevImpressionLettreBienvenue(val);
	}

	@Override
	public void setDelaiEcheanceSommationDeclarationImpotPP(Integer val) {
		container.setDelaiEcheanceSommationDeclarationImpotPP(val);
	}

	@Override
	public void setDelaiEcheanceSommationDeclarationImpotPM(Integer val) {
		container.setDelaiEnvoiSommationDeclarationImpotPM(val);
	}

	@Override
	public void setDelaiEcheanceSommationListeRecapitualtive(Integer val) {
		container.setDelaiEcheanceSommationListeRecapitualtive(val);
	}

	@Override
	public void setDelaiEnvoiSommationDeclarationImpotPP(Integer val) {
		container.setDelaiEnvoiSommationDeclarationImpotPP(val);
	}

	@Override
	public void setDelaiEnvoiSommationListeRecapitulative(Integer val) {
		container.setDelaiEnvoiSommationListeRecapitulative(val);
	}

	@Override
	public void setDelaiRetentionRapportTravailInactif(Integer val) {
		container.setDelaiRetentionRapportTravailInactif(val);
	}

	@Override
	public void setDelaiRetourListeRecapitulative(Integer val) {
		container.setDelaiRetourListeRecapitulative(val);
	}

	@Override
	public void setDelaiRetourSommationListeRecapitulative(Integer val) {
		container.setDelaiRetourSommationListeRecapitulative(val);
	}

	@Override
	public void setDateDebutEnvoiLettresBienvenue(Integer[] val) {
		container.setDateDebutEnvoiLettresBienvenue(val);
	}

	@Override
	public void setDelaiRetourLettreBienvenue(Integer val) {
		container.setDelaiRetourLettreBienvenue(val);
	}

	@Override
	public void setTailleTrouAssujettissementPourNouvelleLettreBienvenue(Integer val) {
		container.setTailleTrouAssujettissementPourNouvelleLettreBienvenue(val);
	}

	@Override
	public void setDelaiEnvoiRappelLettreBienvenue(Integer val) {
		container.setDelaiEnvoiRappelLettreBienvenue(val);
	}

	@Override
	public void setFeteNationale(Integer[] val) {
		container.setFeteNationale(val);
	}

	@Override
	public void setJourDuMoisEnvoiListesRecapitulatives(Integer val) {
		container.setJourDuMoisEnvoiListesRecapitulatives(val);
	}

	@Override
	public void setLendemainNouvelAn(Integer[] val) {
		container.setLendemainNouvelAn(val);
	}

	@Override
	public void setNbMaxParListe(Integer val) {
		container.setNbMaxParListe(val);
	}

	@Override
	public void setNbMaxParPage(Integer val) {
		container.setNbMaxParPage(val);
	}

	@Override
	public void setNoel(Integer[] val) {
		container.setNoel(val);
	}

	@Override
	public void setNouvelAn(Integer[] val) {
		container.setNouvelAn(val);
	}

	@Override
	public void setPremierePeriodeFiscalePersonnesPhysiques(Integer val) {
		container.setPremierePeriodeFiscalePersonnesPhysiques(val);
	}

	@Override
	public void setPremierePeriodeFiscalePersonnesMorales(Integer val) {
		container.setPremierePeriodeFiscalePersonnesMorales(val);
	}

	@Override
	public void setPremierePeriodeFiscaleDeclarationsPersonnesMorales(Integer val) {
		container.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(val);
	}

	@Override
	public void setAnneeMinimaleForDebiteur(Integer val) {
		container.setAnneeMinimaleForDebiteur(val);
	}

	@Override
	public void setValeur(ParametreEnum param, String valeur) {
		container.setValeur(param, valeur);
	}

	@Override
	public void setDateExclusionDecedeEnvoiDI(Integer[] val) {
		container.setDateExclusionDecedeEnvoiDI(val);
	}

	@Override
	public void setAgeRentierFemme(Integer val) {
		container.setAgeRentierFemme(val);
	}

	@Override
	public void setAgeRentierHomme(Integer val) {
		container.setAgeRentierHomme(val);
	}

	@Override
	public void setDelaiMinimalRetourDeclarationImpotPM(Integer val) {
		container.setDelaiMinimalRetourDeclarationImpotPM(val);
	}

	@Override
	public void setDelaiEnvoiSommationDeclarationImpotPM(Integer val) {
		container.setDelaiEnvoiSommationDeclarationImpotPM(val);
	}

	@Override
	public void setDateLimiteEnvoiMasseDeclarationsUtilitePublique(Integer[] val) {
		container.setDateLimiteEnvoiMasseDeclarationsUtilitePublique(val);
	}
}
