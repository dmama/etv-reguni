package ch.vd.unireg.parametrage;

import java.util.HashSet;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;

/**
 * Calculs des délais en nombres de jours ouvrés.
 * 
 * Les calculs peuvent prendre en compte les jours de week-end et jours fériés si souhaité.  
 * 
 * @author xsifnr
 *
 */
public class DelaisServiceImpl implements DelaisService {
	
	private JoursFeriesProvider joursFeriesProvider;
	private ParametreAppService parametreAppService;
	
	/**
	 * {@link Set} des jours fériés
	 */
	private static final Set<RegDate> joursFeries = new HashSet<>();
	
	/**
	 * Set de {@link RegDate} partielles contenant les années pour lesquelles les jours fériés ont
	 * été initialisés
	 */
	private static final Set<RegDate> joursFeriesInitialisesPourAnnees = new HashSet<>();
	

	@Override
	public RegDate getFinDelai(RegDate dateDebut, int delai, boolean joursOuvres, boolean repousseAuProchainJourOuvre) {
		
		assert delai >= 0 : "delai doit être un entier positif";
		assert dateDebut != null : "dateDebut ne doit pas être null";
		
		RegDate dateFinale = dateDebut;
		if (joursOuvres) {
			while (delai > 0 || (delai <= 0 && !isOuvre(dateFinale))) {
				if (isOuvre(dateFinale)) {
					delai--;
				}
				dateFinale = dateFinale.getOneDayAfter();
			}
		} else {
			dateFinale = dateFinale.addDays(delai);
		}
		if (repousseAuProchainJourOuvre) {
			while (!isOuvre(dateFinale)) {
				dateFinale = dateFinale.getOneDayAfter();
			}
		}
		return dateFinale;
		
		
	}
	 

	@Override
	public RegDate getFinDelai(RegDate date, int delaiEnJour) {
		return getFinDelai(date, delaiEnJour, false, true);
	}

	/**
	 * Détermine si un jour est ouvré ou pas
	 * @param date
	 * @return
	 */
	private boolean isOuvre(RegDate date) {
		return date.getWeekDay().isWorkingDay() && !getJoursFeries(date.year()).contains(date);
	}
	
	/**
	 * @param annee permet d'initialiser les dates des jours feriés pour l'année si celà n'est pas déjà fait
	 * @return le Set de date des jours feriés pour toutes les années déjà initialisées.
	 */
	private Set<RegDate> getJoursFeries (int annee) {
		if (!joursFeriesInitialisesPourAnnees.contains(RegDate.get(annee))) {
			synchronized (DelaisServiceImpl.class) {
				if (!joursFeriesInitialisesPourAnnees.contains(RegDate.get(annee))) {
					joursFeries.addAll(joursFeriesProvider.getDatesJoursFeries(annee));
					joursFeriesInitialisesPourAnnees.add(RegDate.get(annee));					
				}
			}
		}
		return joursFeries;
	}
	

	@Override
	public RegDate getDateFinDelaiAttenteDeclarationImpotPersonneDecedee(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiAttenteDeclarationImpotPersonneDecedee());		
	}

	@Override
	public RegDate getDateFinDelaiRetourDeclarationImpotPPEmiseManuellement(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDelaiRetourDeclarationImpotPPEmiseManuellement());
	}

	@Override
	public RegDate getDateFinDelaiRetourDeclarationImpotPMEmiseManuellement(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDelaiRetourDeclarationImpotPMEmiseManuellement());
	}

	@Override
	public RegDate getDateFinDelaiRetourQuestionnaireSNCEmisManuellement(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDelaiRetourQuestionnaireSNCEmisManuellement());
	}

	@Override
	public RegDate getDateFinDelaiDemandeDelai(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDateFinDelaiDemandeDelai());
	}

	@Override
	public RegDate getDateFinDelaiEcheanceRappelQSNC(RegDate dateRappel) {
		return getFinDelai(dateRappel, parametreAppService.getDelaiRetourQuestionnaireSNCRappele());
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionDeclarationImpot(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiCadevImpressionDeclarationImpot());
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionListesRecapitulatives(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiCadevImpressionListesRecapitulatives());
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionQuestionnaireSNC(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDateExpeditionDelaiImpressionCadev());
	}

	@Override
	public RegDate getDateExpeditionDelaiImpressionCadev(RegDate dateExpedition) {
		return getFinDelai(dateExpedition, parametreAppService.getDateExpeditionDelaiImpressionCadev());
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionLettreBienvenue(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDelaiCadevImpressionLettreBienvenue());
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionDemandeDegrevementICI(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDelaiCadevImpressionDemandeDegrevementICI());
	}

	@Override
	public RegDate getDateFinDelaiEcheanceSommationDeclarationImpotPP(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiEcheanceSommationDeclarationImpotPP());
	}

	@Override
	public RegDate getDateFinDelaiEcheanceSommationDeclarationImpotPM(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDelaiEcheanceSommationDeclarationImpotPM());
	}

	@Override
	public RegDate getDateFinDelaiEcheanceSommationListeRecapitualtive(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiEcheanceSommationListeRecapitulative());
	}

	@Override
	public RegDate getDateFinDelaiEnvoiSommationDeclarationImpotPP(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiEnvoiSommationDeclarationImpotPP());
	}

	@Override
	public RegDate getDateFinDelaiEnvoiSommationDeclarationImpotPM(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDelaiEnvoiSommationDeclarationImpotPM());
	}

	@Override
	public RegDate getDateFinDelaiEnvoiSommationListeRecapitulative(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiEnvoiSommationListeRecapitulative());
	}

	@Override
	public RegDate getDateFinDelaiEnvoiRappelQuestionnaireSNC(RegDate dateDebut) {
		return getFinDelai(dateDebut, parametreAppService.getDelaiEnvoiRappelQuestionnaireSNC());
	}

	@Override
	public RegDate getDateFinDelaiRetentionRapportTravailInactif(RegDate dateDebut){
		// ATTENTION : Ce paramètre de délai est exprimé en nombre de mois 
		return dateDebut.addMonths(parametreAppService.getDelaiRetentionRapportTravailInactif());
	}

	@Override
	public RegDate getDateFinDelaiRetourListeRecapitulative(RegDate dateEmissionLr, RegDate dateFinPeriodeLr) {
		final RegDate dateReference = dateEmissionLr.isAfter(dateFinPeriodeLr) ? dateEmissionLr : dateFinPeriodeLr;
		return getFinDelai(dateReference, parametreAppService.getDelaiRetourListeRecapitulative());
	}

	@Override
	public RegDate getDateFinDelaiRetourSommationListeRecapitulative(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiRetourSommationListeRecapitulative());
	}

	public void setJoursFeriesProvider(JoursFeriesProvider joursFeriesProvider) {
		this.joursFeriesProvider = joursFeriesProvider;
	}

	public void setParametreAppService(ParametreAppService parametreAppService) {
		this.parametreAppService = parametreAppService;
	}

}
