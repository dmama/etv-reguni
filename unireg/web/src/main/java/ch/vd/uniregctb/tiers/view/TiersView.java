package ch.vd.uniregctb.tiers.view;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.di.view.DeclarationView;
import ch.vd.uniregctb.entreprise.EntrepriseView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.individu.IndividuView;
import ch.vd.uniregctb.lr.view.ListeRecapDetailView;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.mouvement.view.MouvementDetailView;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.rt.view.RapportPrestationView;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DecisionAciView;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.NatureTiers;
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

	private ComplementView complement = new ComplementView();

	private Tiers tiers;

	private Tiers tiersPrincipal;

	private Tiers tiersConjoint;

	private String nomPrenomPrincipal;

	private String nomPrenomConjoint;

	private IndividuView individu;

	private IndividuView individuConjoint;

	private List<AdresseView> historiqueAdresses;

	private List<AdresseCivilView> historiqueAdressesCiviles;
	private String exceptionAdresseCiviles;

	private List<AdresseCivilView> historiqueAdressesCivilesConjoint;
	private String exceptionAdresseCivilesConjoint;

	private List<AdresseView> adressesEnErreur;

	private String adressesEnErreurMessage;

	private List<RapportView> dossiersApparentes;

	private List<RapportPrestationView> rapportsPrestation;

	private boolean rapportsPrestationHisto;

	private boolean ctbAssocieHisto;

	private Set<DebiteurView> debiteurs;

	private List<RapportView> contribuablesAssocies;

	private List<ListeRecapDetailView> lrs;

	private List<DeclarationView> dis;

	private ForFiscalView forsPrincipalActif;

	private List<ForFiscalView> forsFiscaux;

	private List<PeriodiciteView> periodicites;

	private PeriodiciteView periodicite;

	private List<SituationFamilleView> situationsFamille;

	private String situationsFamilleEnErreurMessage;

	private List<MouvementDetailView> mouvements;

	@Deprecated
	private EntrepriseView entreprise;

	private List<RegimeFiscalView> regimesFiscauxVD;
	private List<RegimeFiscalView> regimesFiscauxCH;

	private List<AllegementFiscalView> allegementsFiscaux;
	private List<ExerciceCommercial> exercicesCommerciaux;
	private RegDate dateBouclementFutur;

	private boolean isAllowed;

	private boolean addContactISAllowed;

	private LogicielView logiciel;

	private List<DecisionAciView> decisionsAci;

	private boolean decisionRecente;

	//10 éléments à afficher par défaut
	private int nombreElementsTable = 10;

	public ComplementView getComplement() {
		return complement;
	}

	public void setComplement(ComplementView complement) {
		this.complement = complement;
	}

	/**
	 * @return the tiers
	 */
	public Tiers getTiers() {
		return tiers;
	}

	/**
	 * @param tiers the tiers to set
	 */
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
	 * @return la nature du tiers (	Entreprise / Etablissement
	 * 								/ Habitant / Non Habitant
	 * 								/ AutreCommunaute / MenageCommun)
	 */
	public NatureTiers getNatureTiers() {
		if(tiers != null){
			return tiers.getNatureTiers();
		}
		return null;
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
		if(tiersPrincipal != null)
			return tiersPrincipal.getNatureTiers();
		return null;
	}

	public NatureTiers getNatureMembreConjoint() {
		if(tiersConjoint != null)
			return tiersConjoint.getNatureTiers();
		return null;
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

	public EntrepriseView getEntreprise() {
		return entreprise;
	}

	@Deprecated
	public void setEntreprise(EntrepriseView entreprise) {
		this.entreprise = entreprise;
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

	public RegDate getDateBouclementFutur() {
		return dateBouclementFutur;
	}

	public void setDateBouclementFutur(RegDate dateBouclementFutur) {
		this.dateBouclementFutur = dateBouclementFutur;
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

	public List<DeclarationView> getDis() {
		return dis;
	}

	public void setDis(List<DeclarationView> dis) {
		this.dis = dis;
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

	public boolean isCtbAssocieHisto() {
		return ctbAssocieHisto;
	}

	public void setCtbAssocieHisto(boolean ctbAssocieHisto) {
		this.ctbAssocieHisto = ctbAssocieHisto;
	}

	public int getNombreElementsTable() {
		return nombreElementsTable;
	}

	public void setNombreElementsTable(int nombreElementsTable) {
		this.nombreElementsTable = nombreElementsTable;
	}
}