package ch.vd.uniregctb.parametrage;

public interface ParametreAppAccessor {

	String getDefaut(ParametreEnum param);

	Integer getDelaiAttenteDeclarationImpotPersonneDecedee();

	Integer getDelaiRetourDeclarationImpotEmiseManuellement();

	Integer getDelaiCadevImpressionDeclarationImpot();

	Integer getDelaiCadevImpressionListesRecapitulatives();

	Integer getDelaiEcheanceSommationDeclarationImpotPP();

	Integer getDelaiEcheanceSommationDeclarationImpotPM();

	Integer getDelaiEcheanceSommationListeRecapitualtive();

	Integer getDelaiEnvoiSommationDeclarationImpotPP();

	Integer getDelaiEnvoiSommationDeclarationImpotPM();

	Integer getDelaiEnvoiSommationListeRecapitulative();

	Integer getDelaiRetentionRapportTravailInactif();

	Integer getDelaiRetourListeRecapitulative();

	Integer getDelaiRetourSommationListeRecapitulative();

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
	 *
	 * @return l'année minimale acceptée lors de la création de nouveaux fors débiteur.
	 */
	Integer getAnneeMinimaleForDebiteur();

	Integer getAgeRentierFemme();

	Integer getAgeRentierHomme();

	void setDelaiAttenteDeclarationImpotPersonneDecedee(Integer val);

	void setDelaiRetourDeclarationImpotEmiseManuellement(Integer val);

	void setDelaiCadevImpressionDeclarationImpot(Integer val);

	void setDelaiCadevImpressionListesRecapitulatives(Integer val);

	void setDelaiEcheanceSommationDeclarationImpotPP(Integer val);

	void setDelaiEcheanceSommationDeclarationImpotPM(Integer val);

	void setDelaiEcheanceSommationListeRecapitualtive(Integer val);

	void setDelaiEnvoiSommationDeclarationImpotPP(Integer val);

	void setDelaiEnvoiSommationDeclarationImpotPM(Integer val);

	void setDelaiEnvoiSommationListeRecapitulative(Integer val);

	void setDelaiRetentionRapportTravailInactif(Integer val);

	void setDelaiRetourListeRecapitulative(Integer val);

	void setDelaiRetourSommationListeRecapitulative(Integer val);

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
