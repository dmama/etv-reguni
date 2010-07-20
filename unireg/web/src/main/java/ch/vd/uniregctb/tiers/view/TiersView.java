package ch.vd.uniregctb.tiers.view;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.uniregctb.di.view.DeclarationImpotDetailView;
import ch.vd.uniregctb.entreprise.EntrepriseView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.individu.IndividuView;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.rt.view.RapportPrestationView;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Structure model commun a l'ecran de visualisation et
 * l'ecran d'edition des Tiers
 *
 * @author xcifde
 *
 */
public class TiersView {

	private TiersGeneralView tiersGeneral;

	private Tiers tiers;

	private Tiers tiersPrincipal;

	private Tiers tiersConjoint;

	private String nomPrenomPrincipal;

	private String nomPrenomConjoint;

	private IndividuView individu;

	private IndividuView individuConjoint;

	private List<AdresseView> historiqueAdresses;

	private List<AdresseView> historiqueAdressesCiviles;

	private List<AdresseView> historiqueAdressesCivilesConjoint;

	private List<AdresseView> adressesEnErreur;

	private String adressesEnErreurMessage;

	private List<RapportView> dossiersApparentes;

	private List<RapportPrestationView> rapportsPrestation;

	private boolean rapportsPrestationHisto;

	private Set<DebiteurView> debiteurs;

	private List<RapportView> contribuablesAssocies;

	private List<ListeRecapDetailView> lrs;

	private List<DeclarationImpotDetailView> dis;

	private ForFiscalView forsPrincipalActif;

	private List<ForFiscalView> forsFiscaux;

	private List<PeriodiciteView> periodicites;

	private PeriodiciteView periodicite;

	private List<SituationFamilleView> situationsFamille;

	private List<MouvementDetailView> mouvements;

	private EntrepriseView entreprise;

	private String urlSipf;

	private String urlTaoPP;

	private String urlTaoBA;

	private String urlTaoIS;

	private String urlCAT;

	private String urlRegView;

	private boolean isAllowed;

	private boolean addContactISAllowed;

	private Map<String, Boolean> allowedOnglet;

	private boolean ibanValide;

	/**
	 * @return the tiers
	 */
	public Tiers getTiers() {
		return tiers;
	}

	/**
	 * @param tiers the tiers to set
	 */
	public void setTiers(Tiers tiers) {
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
	 * @return la nature du tiers (	Entreprise / Etablissement
	 * 								/ Habitant / Non Habitant
	 * 								/ AutreCommunaute / MenageCommun)
	 */
	public String getNatureTiers() {
		if(tiers != null){
			return tiers.getNatureTiers();
		}
		return "";
	}

	/**
	 * @return si le tiers est inactif (ancien I107)
	 */
	public boolean isDebiteurInactif() {
		if(tiers != null)
			return tiers.isDebiteurInactif();
		return true;
	}

	/**
	 * @return le type d'autorité fiscale du for principal actif
	 */
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		TypeAutoriteFiscale result = null;
		if(tiers instanceof Contribuable){
			ForFiscalPrincipal forFiscal = tiers.getForFiscalPrincipalAt(null);
			if(forFiscal != null){
				result = forFiscal.getTypeAutoriteFiscale();
			}
		}
		return result;
	}

	public Tiers getTiersConjoint() {
		return tiersConjoint;
	}

	public void setTiersConjoint(Tiers tiersConjoint) {
		this.tiersConjoint = tiersConjoint;
	}

	public IndividuView getIndividuConjoint() {
		return individuConjoint;
	}

	public void setIndividuConjoint(IndividuView individuConjoint) {
		this.individuConjoint = individuConjoint;
	}

	public List<AdresseView> getHistoriqueAdresses() {
		return historiqueAdresses;
	}

	public void setHistoriqueAdresses(List<AdresseView> historiqueAdresses) {
		this.historiqueAdresses = historiqueAdresses;
	}

	public String getNatureMembrePrincipal() {
		if(tiersPrincipal != null)
			return tiersPrincipal.getNatureTiers();
		return "";
	}

	public String getNatureMembreConjoint() {
		if(tiersConjoint != null)
			return tiersConjoint.getNatureTiers();
		return "";
	}

	public Tiers getTiersPrincipal() {
		return tiersPrincipal;
	}

	public void setTiersPrincipal(Tiers tiersPrincipal) {
		this.tiersPrincipal = tiersPrincipal;
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

	public String getUrlSipf() {
		return urlSipf;
	}

	public void setUrlSipf(String urlSipf) {
		this.urlSipf = urlSipf;
	}

	public String getUrlTaoPP() {
		return urlTaoPP;
	}

	public void setUrlTaoPP(String urlTao) {
		this.urlTaoPP = urlTao;
	}

	public String getUrlTaoBA() {
		return urlTaoBA;
	}

	public void setUrlTaoBA(String urlTao) {
		this.urlTaoBA = urlTao;
	}

	public List<RapportPrestationView> getRapportsPrestation() {
		return rapportsPrestation;
	}

	public void setRapportsPrestation(List<RapportPrestationView> rapportsPrestation) {
		this.rapportsPrestation = rapportsPrestation;
	}

	public boolean isRapportsPrestationHisto() {
		return rapportsPrestationHisto;
	}

	public void setRapportsPrestationHisto(boolean rapportsPrestationHisto) {
		this.rapportsPrestationHisto = rapportsPrestationHisto;
	}

	public List<ListeRecapDetailView> getLrs() {
		return lrs;
	}

	public void setLrs(List<ListeRecapDetailView> lrs) {
		this.lrs = lrs;
	}

	public String getUrlCAT() {
		return urlCAT;
	}

	public void setUrlCAT(String urlCAT) {
		this.urlCAT = urlCAT;
	}

	public String getUrlRegView() {
		return urlRegView;
	}

	public void setUrlRegView(String urlRegView) {
		this.urlRegView = urlRegView;
	}


	public EntrepriseView getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(EntrepriseView entreprise) {
		this.entreprise = entreprise;
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

	public List<SituationFamilleView> getSituationsFamille() {
		return situationsFamille;
	}

	public void setSituationsFamille(List<SituationFamilleView> situationsFamille) {
		this.situationsFamille = situationsFamille;
	}

	/**
	 * @return the urlTaoIS
	 */
	public String getUrlTaoIS() {
		return urlTaoIS;
	}

	/**
	 * @param urlTaoIS the urlTaoIS to set
	 */
	public void setUrlTaoIS(String urlTaoIS) {
		this.urlTaoIS = urlTaoIS;
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

	public void setAdressesEnErreur(List<AdresseView> adressesEnErreur) {
		this.adressesEnErreur = adressesEnErreur;
	}

	public String getAdressesEnErreurMessage() {
		return adressesEnErreurMessage;
	}

	public void setAdressesEnErreurMessage(String adressesEnErreurMessage) {
		this.adressesEnErreurMessage = adressesEnErreurMessage;
	}

	public List<DeclarationImpotDetailView> getDis() {
		return dis;
	}

	public void setDis(List<DeclarationImpotDetailView> dis) {
		this.dis = dis;
	}

	public List<MouvementDetailView> getMouvements() {
		return mouvements;
	}

	public void setMouvements(List<MouvementDetailView> mouvements) {
		this.mouvements = mouvements;
	}

	public Map<String, Boolean> getAllowedOnglet() {
		return allowedOnglet;
	}


	public void setAllowedOnglet(Map<String, Boolean> allowedOnglet) {
		this.allowedOnglet = allowedOnglet;
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

	public void setHistoriqueAdressesCiviles(List<AdresseView> historiqueAdressesCiviles) {
		this.historiqueAdressesCiviles = historiqueAdressesCiviles;
	}

	public List<AdresseView> getHistoriqueAdressesCiviles() {
		return historiqueAdressesCiviles;
	}

	public void setHistoriqueAdressesCivilesConjoint(List<AdresseView> historiqueAdressesCivilesConjoint) {
		this.historiqueAdressesCivilesConjoint = historiqueAdressesCivilesConjoint;
	}

	public List<AdresseView> getHistoriqueAdressesCivilesConjoint() {
		return historiqueAdressesCivilesConjoint;	}


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

	public boolean isIbanValide() {
		return ibanValide;
	}

	public void setIbanValide(boolean ibanValide) {
		this.ibanValide = ibanValide;
	}
}