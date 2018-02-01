package ch.vd.unireg.parametrage;

public interface ParametreAppAccessor {

	String getDefaut(ParametreEnum param);

	Integer getDelaiAttenteDeclarationImpotPersonneDecedee();

	Integer getDelaiRetourDeclarationImpotPPEmiseManuellement();

	Integer getDelaiRetourDeclarationImpotPMEmiseManuellement();

	Integer getDelaiCadevImpressionDeclarationImpot();

	Integer getDelaiCadevImpressionListesRecapitulatives();

	Integer getDelaiCadevImpressionLettreBienvenue();

	Integer getDelaiCadevImpressionQuestionnaireSNC();

	Integer getDelaiCadevImpressionDemandeDegrevementICI();

	Integer getDelaiEcheanceSommationDeclarationImpotPP();

	Integer getDelaiEcheanceSommationDeclarationImpotPM();

	Integer getDelaiEcheanceSommationListeRecapitulative();

	Integer getDelaiEnvoiSommationDeclarationImpotPP();

	Integer getDelaiEnvoiSommationDeclarationImpotPM();

	Integer[] getDateLimiteEnvoiMasseDeclarationsUtilitePublique();

	Integer getDelaiRetourQuestionnaireSNCEmisManuellement();

	Integer getDelaiEnvoiRappelQuestionnaireSNC();

	Integer getDelaiEnvoiSommationListeRecapitulative();

	Integer getDelaiRetentionRapportTravailInactif();

	Integer getDelaiRetourListeRecapitulative();

	Integer getDelaiRetourSommationListeRecapitulative();

	Integer[] getDateDebutEnvoiLettresBienvenue();

	Integer getDelaiRetourLettreBienvenue();

	Integer getTailleTrouAssujettissementPourNouvelleLettreBienvenue();

	Integer getDelaiEnvoiRappelLettreBienvenue();

	Integer[] getDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI();

	Integer getDelaiRetourDemandeDegrevementICI();

	Integer getDelaiEnvoiRappelDemandeDegrevementICI();

	Integer[] getFeteNationale();

	Integer getJourDuMoisEnvoiListesRecapitulatives();

	Integer[] getLendemainNouvelAn();

	Integer getNbMaxParListe();

	Integer getNbMaxParPage();

	Integer[] getNoel();

	String getNom(ParametreEnum param);

	Integer[] getNouvelAn();

	Integer getPremierePeriodeFiscalePersonnesPhysiques();

	Integer getPremierePeriodeFiscalePersonnesMorales();

	Integer getPremierePeriodeFiscaleDeclarationsPersonnesMorales();

	Integer[] getDateExclusionDecedeEnvoiDI();

	Integer getDelaiMinimalRetourDeclarationImpotPM();

	/**
	 * [UNIREG-2507]
	 * @return l'année minimale acceptée lors de la création de nouveaux fors débiteur.
	 */
	Integer getAnneeMinimaleForDebiteur();

	Integer getAgeRentierFemme();

	Integer getAgeRentierHomme();

	void setDelaiAttenteDeclarationImpotPersonneDecedee(Integer val);

	void setDelaiRetourDeclarationImpotPPEmiseManuellement(Integer val);

	void setDelaiRetourDeclarationImpotPMEmiseManuellement(Integer val);

	void setDelaiCadevImpressionDeclarationImpot(Integer val);

	void setDelaiCadevImpressionListesRecapitulatives(Integer val);

	void setDelaiCadevImpressionLettreBienvenue(Integer val);

	void setDelaiCadevImpressionQuestionnaireSNC(Integer val);

	void setDelaiCadevImpressionDemandeDegrevementICI(Integer val);

	void setDelaiEcheanceSommationDeclarationImpotPP(Integer val);

	void setDelaiEcheanceSommationDeclarationImpotPM(Integer val);

	void setDelaiEcheanceSommationListeRecapitualtive(Integer val);

	void setDelaiEnvoiSommationDeclarationImpotPP(Integer val);

	void setDelaiEnvoiSommationDeclarationImpotPM(Integer val);

	void setDateLimiteEnvoiMasseDeclarationsUtilitePublique(Integer[] val);

	void setDelaiRetourQuestionnaireSNCEmisManuellement(Integer val);

	void setDelaiEnvoiRappelQuestionnaireSNC(Integer val);

	void setDelaiEnvoiSommationListeRecapitulative(Integer val);

	void setDelaiRetentionRapportTravailInactif(Integer val);

	void setDelaiRetourListeRecapitulative(Integer val);

	void setDelaiRetourSommationListeRecapitulative(Integer val);

	void setDateDebutEnvoiLettresBienvenue(Integer[] val);

	void setDelaiRetourLettreBienvenue(Integer val);

	void setTailleTrouAssujettissementPourNouvelleLettreBienvenue(Integer val);

	void setDelaiEnvoiRappelLettreBienvenue(Integer val);

	void setDateDebutPriseEnCompteModificationPourNouvelleDemandeDegrevementICI(Integer[] val);

	void setDelaiRetourDemandeDegrevementICI(Integer val);

	void setDelaiEnvoiRappelDemandeDegrevementICI(Integer val);

	void setFeteNationale(Integer[] val);

	void setJourDuMoisEnvoiListesRecapitulatives(Integer val);

	void setLendemainNouvelAn(Integer[] val);

	void setNbMaxParListe(Integer val);

	void setNbMaxParPage(Integer val);

	void setNoel(Integer[] val);

	void setNouvelAn(Integer[] val);

	void setPremierePeriodeFiscalePersonnesPhysiques(Integer val);

	void setPremierePeriodeFiscalePersonnesMorales(Integer val);

	void setPremierePeriodeFiscaleDeclarationsPersonnesMorales(Integer val);

	void setAnneeMinimaleForDebiteur(Integer val);

	void setValeur(ParametreEnum param, String valeur);

	void setDateExclusionDecedeEnvoiDI(Integer[] val);

	void setAgeRentierFemme(Integer val);

	void setAgeRentierHomme(Integer val);

	void setDelaiMinimalRetourDeclarationImpotPM(Integer val);

}
