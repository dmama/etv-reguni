package ch.vd.uniregctb.parametrage;

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
	private static Set<RegDate> joursFeries = new HashSet<RegDate>();
	
	/**
	 * Set de {@link RegDate} partielles contenant les années pour lesquelles les jours fériés ont
	 * été initialisés
	 *   
	 */
	private static Set<RegDate> joursFeriesInitialisesPourAnnees = new HashSet<RegDate>();
	

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getFinDelai(ch.vd.registre.base.date.RegDate, int, boolean, boolean)
	 */
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
	 

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getFinDelai(ch.vd.registre.base.date.RegDate, int)
	 */
	public RegDate getFinDelai(RegDate date, int delaiEnJour) {
		return getFinDelai(date, delaiEnJour, false, true);
	}

	/**
	 * Détermine si un jour est ouvré ou pas
	 * @param date
	 * @return
	 */
	private boolean isOuvre(RegDate date) {
		return 
			date.getWeekDay().isWorkingDay() &&
			!getJoursFeries(date.year()).contains(date);
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
	

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiAttenteDeclarationImpotPersonneDecedee(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiAttenteDeclarationImpotPersonneDecedee(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiAttenteDeclarationImpotPersonneDecedee());		
	}
	
	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiAttenteDeclarationImpotPersonneDecedee(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiRetourDeclarationImpotEmiseManuellement(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiRetourDeclarationImpotEmiseManuellement());		
	}	

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiCadevImpressionDeclarationImpot(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiCadevImpressionDeclarationImpot(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiCadevImpressionDeclarationImpot());
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiCadevImpressionListesRecapitulatives(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiCadevImpressionListesRecapitulatives(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiCadevImpressionListesRecapitulatives());
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiEcheanceSommationDeclarationImpot(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiEcheanceSommationDeclarationImpot(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiEcheanceSommationDeclarationImpot());
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiEcheanceSommationListeRecapitualtive(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiEcheanceSommationListeRecapitualtive(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiEcheanceSommationListeRecapitualtive());
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiEnvoiSommationDeclarationImpot(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiEnvoiSommationDeclarationImpot(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiEnvoiSommationDeclarationImpot());
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiEnvoiSommationListeRecapitulative(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiEnvoiSommationListeRecapitulative(RegDate dateDebut){
		return getFinDelai(dateDebut, parametreAppService.getDelaiEnvoiSommationListeRecapitulative());
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiRetentionRapportTravailInactif(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiRetentionRapportTravailInactif(RegDate dateDebut){
		// ATTENTION : Ce paramètre de délai est exprimé en nombre de mois 
		RegDate dateFin = dateDebut.addMonths(parametreAppService.getDelaiRetentionRapportTravailInactif());
		return dateFin;
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiRetourListeRecapitulative(ch.vd.registre.base.date.RegDate)
	 */
	public RegDate getDateFinDelaiRetourListeRecapitulative(RegDate dateEmissionLr, RegDate dateFinPeriodeLr) {
		final RegDate dateReference = dateEmissionLr.isAfter(dateFinPeriodeLr) ? dateEmissionLr : dateFinPeriodeLr;
		return getFinDelai(dateReference, parametreAppService.getDelaiRetourListeRecapitulative());
	}

	/* (non-Javadoc)
	 * @see ch.vd.uniregctb.parametrage.DelaisService#getDateFinDelaiRetourSommationListeRecapitulative(ch.vd.registre.base.date.RegDate)
	 */
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
