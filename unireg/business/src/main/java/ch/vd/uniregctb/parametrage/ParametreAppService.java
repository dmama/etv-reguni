package ch.vd.uniregctb.parametrage;

import ch.vd.uniregctb.parametrage.ParametreEnum;


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

	public String getValeur(ParametreEnum param);

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

	public void setValeur(ParametreEnum param, String valeur);

}
