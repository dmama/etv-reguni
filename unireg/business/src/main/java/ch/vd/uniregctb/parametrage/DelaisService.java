package ch.vd.uniregctb.parametrage;

import ch.vd.registre.base.date.RegDate;

/**
 * Calcul des differents délais paramétrables
 * 
 * @author xsifnr
 *
 */
public interface DelaisService {

	/**
	 * Determine la date d'échéance du délai pour envoyé une DI à une personne décédée.<br>
	 * <br>
	 * Les jours fériés sont comptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */
	RegDate getDateFinDelaiAttenteDeclarationImpotPersonneDecedee(RegDate dateDebut);

	/**
	 * Determine la date d'échéance du délai de retour pour une DI émise manuellement.<br>
	 * <br>
	 * Les jours fériés sont comptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */
	RegDate getDateFinDelaiRetourDeclarationImpotEmiseManuellement(RegDate dateDebut);
	
	/**
	 * Determine la date d'échéance du délai technique d’impression par la CADEV des déclarations d’impôt.<br>
	 * <br>
	 * Les jours fériés sont comptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */
	RegDate getDateFinDelaiCadevImpressionDeclarationImpot(RegDate dateDebut);

	/**
	 * Determine la date d'échéance du délai technique d’impression par la CADEV des listes récapitulatives.<br>
	 * <br>
	 * Les jours fériés sont comptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */
	RegDate getDateFinDelaiCadevImpressionListesRecapitulatives(RegDate dateDebut);

	/**
	 * Determine la date d'échéance du délai d’échéance d’une sommation de déposer la déclaration d’impôt (art. 174 al. 4 LI).<br>
	 * <br>
	 * Les jours fériés sont comptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */	
	RegDate getDateFinDelaiEcheanceSommationDeclarationImpot(RegDate dateDebut);

	/**
	 * Determine la date d'échéance du délai administratif avant l’échéance d’une sommation de déposer la liste récapitulative.<br>
	 * <br>
	 * Les jours fériés sont comptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */	
	RegDate getDateFinDelaiEcheanceSommationListeRecapitualtive(RegDate dateDebut);

	/**
	 * Determine la date d'échéance du délai administratif avant l’envoi d’une sommation de déposer la déclaration d’impôt.<br>
	 * <br>
	 * Les jours fériés sont comptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */	
	RegDate getDateFinDelaiEnvoiSommationDeclarationImpot(RegDate dateDebut);

	/**
	 * Determine la date d'échéance du délai administratif avant l’envoi d’une sommation de déposer la liste récapitulative.<br>
	 * <br>
	 * Les jours fériés sont comptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */	
	RegDate getDateFinDelaiEnvoiSommationListeRecapitulative(RegDate dateDebut);

	/**
	 * Determine la date d'échéance du délai de rétention des rapports de travail inactifs.<br>
	 * <br>
	 * Les jours fériés sont comptés dans le délai.<br>
	 * Le délai <b>n'est pas</b> repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */	
	RegDate getDateFinDelaiRetentionRapportTravailInactif(RegDate dateDebut);

	/**
	 * Determine la date d'échéance du délai de retour des listes récapitulatives.<br>
	 * <br>
	 * Les jours fériés sont décomptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateEmissionLr date d'émission de la LR
	 * @param dateFinPeriodeLr date de fin de la période couverte par la LR
	 * @return la date d'échéance du délai
	 */	
	RegDate getDateFinDelaiRetourListeRecapitulative(RegDate dateEmissionLr, RegDate dateFinPeriodeLr);

	/**
	 * Determine la date d'échéance du délai de retour des sommations de déposer la liste récapitulative.<br>
	 * <br>
	 * Les jours fériés sont décomptés dans le délai.<br>
	 * Le délai est repoussé au premier jour ouvrable s'il tombe sur un jour non-ouvré.<br>
	 * 
	 * @param dateDebut date 
	 * 
	 * @return la date d'échéance du délai
	 */	
	RegDate getDateFinDelaiRetourSommationListeRecapitulative(RegDate dateDebut);
	
	/**
	 * Calcule la date d'écheance d'un délai exprimé en jour. <br> 
	 * <br>
	 * Les jours non-ouvrés sont pris en compte dans le nombre de jours.<br>
	 * <br>
	 * Le délai est repoussé au premier jour ouvré s'il tombe sur un jour non-ouvré. 
	 * 
	 * @param date La {@link RegDate} representant le point de départ du calcul du délai
	 * @param delaiEnJours le délai exprimé en nombre de jours
	 * @return
	 */
	RegDate getFinDelai(RegDate dateDebut, int delaiEnJours);
	
	
	/**
	 * Calcule la date d'écheance d'un délai exprimé en jour. <br> 
	 * <br>
	 * Les week-ends et jours feriés sont pris en compte de telle sorte 
	 * qu'ils n'amputent pas le délai si le flag <code>joursOuvres</code> est à true.<br>
	 * <br>
	 * Le délai est repoussé au prochain jour ouvré si le flag <code>repousseAuProchainJourOuvre</code>
	 * 
	 * @param dateDebut La {@link RegDate} representant le point de départ du calcul du délai
	 * @param delaiEnJours le délai exprimé en nombre de jours
	 * @param joursOuvres true si le délai est exprimé en jours ouvrés
	 * @param repousseAuProchainJourOuvre true si le délai doit être repoussé au prochain jour ouvré si il expire su un jour non-ouvré 
	 * 
	 * @return la Date représentant l'échéance du délai
	 */
	RegDate getFinDelai(RegDate dateDebut, int delaiEnJours, boolean joursOuvres, boolean repousseAuProchainJourOuvre);

}
