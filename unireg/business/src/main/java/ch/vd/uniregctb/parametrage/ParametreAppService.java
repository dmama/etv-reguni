package ch.vd.uniregctb.parametrage;

public interface ParametreAppService {

	public String getDefaut(ParametreEnum param);

	public Integer getDelaiAttenteDeclarationImpotPersonneDecedee();

	public Integer getDelaiRetourDeclarationImpotEmiseManuellement();

	public Integer getDelaiCadevImpressionDeclarationImpot();

	public Integer getDelaiCadevImpressionListesRecapitulatives();

	public Integer getDelaiEcheanceSommationDeclarationImpot();

	public Integer getDelaiEcheanceSommationListeRecapitualtive();

	public Integer getDelaiEnvoiSommationDeclarationImpot();

	public Integer getDelaiEnvoiSommationListeRecapitulative();

	public Integer getDelaiRetentionRapportTravailInactif();

	public Integer getDelaiRetourListeRecapitulative();

	public Integer getDelaiRetourSommationListeRecapitulative();

	public Integer[] getFeteNationale();

	public Integer getJourDuMoisEnvoiListesRecapitulatives();

	public Integer[] getLendemainNouvelAn();

	public Integer getNbMaxParListe();

	public Integer getNbMaxParPage();

	public Integer[] getNoel();

	public String getNom(ParametreEnum param);

	public Integer[] getNouvelAn();

	public Integer getPremierePeriodeFiscale();

	public Integer[] getDateExclusionDecedeEnvoiDI();

	/**
	 * [UNIREG-2507]
	 *
	 * @return l'année minimale acceptée lors de la création de nouveaux fors débiteur.
	 */
	public Integer getAnneeMinimaleForDebiteur();

	public Integer getAgeRentierFemme();

	public Integer getAgeRentierHomme();

	public void reset();

	public void save();

	public void setDelaiAttenteDeclarationImpotPersonneDecedee(Integer val);

	public void setDelaiRetourDeclarationImpotEmiseManuellement(Integer val);

	public void setDelaiCadevImpressionDeclarationImpot(Integer val);

	public void setDelaiCadevImpressionListesRecapitulatives(Integer val);

	public void setDelaiEcheanceSommationDeclarationImpot(Integer val);

	public void setDelaiEcheanceSommationListeRecapitualtive(Integer val);

	public void setDelaiEnvoiSommationDeclarationImpot(Integer val);

	public void setDelaiEnvoiSommationListeRecapitulative(Integer val);

	public void setDelaiRetentionRapportTravailInactif(Integer val);

	public void setDelaiRetourListeRecapitulative(Integer val);

	public void setDelaiRetourSommationListeRecapitulative(Integer val);

	public void setFeteNationale(Integer[] val);

	public void setJourDuMoisEnvoiListesRecapitulatives(Integer val);

	public void setLendemainNouvelAn(Integer[] val);

	public void setNbMaxParListe(Integer val);

	public void setNbMaxParPage(Integer val);

	public void setNoel(Integer[] val);

	public void setNouvelAn(Integer[] val);

	public void setPremierePeriodeFiscale(Integer val);

	public void setAnneeMinimaleForDebiteur(Integer val);

	public void setValeur(ParametreEnum param, String valeur);

	public void setDateExclusionDecedeEnvoiDI(Integer[] val);

	public void setAgeRentierFemme(Integer val);

	public void setAgeRentierHomme(Integer val);

}
