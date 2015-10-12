package ch.vd.uniregctb.evenement.fiscal;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Interface du service des événement fiscaux
 */
public interface EvenementFiscalService {

	/**
	 * @param tiers un tiers
	 * @return la liste des événements fiscaux pour le tiers fourni
	 */
	Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers);

	/**
	 * Publie un événement fiscal de type 'Ouverture de for'
	 * @param forFiscal le for fiscal ouvert pour lequel on publie l'événement
	 */
	void publierEvenementFiscalOuvertureFor(ForFiscal forFiscal);

	/**
	 * Publie un événement fiscal de type 'Fermeture de for'
	 * @param forFiscal le for fermé ouvert pour lequel on publie l'événement
	 */
	void publierEvenementFiscalFermetureFor(ForFiscal forFiscal);

	/**
	 * Publie un événement fiscal de type 'Annulation de for'
	 * @param forFiscal      le for fiscal qui vient d'être annulé
	 */
	void publierEvenementFiscalAnnulationFor(ForFiscal forFiscal);

	/**
	 * Publie un événement fiscal de type 'Changement de mode d'imposition'
	 * @param forFiscal le nouveau for fiscal avec le nouveau mode d'imposition
	 */
	void publierEvenementFiscalChangementModeImposition(ForFiscal forFiscal);

	/**
	 * [UNIREG-3244] Publie un événement de fin d'autorité parentale sur un contribuable parent suite à la majorité d'un enfant.
	 *
	 * @param contribuableEnfant le contribuable qui acquiert la majorité
	 * @param contribuableParent le contribuable dont l'autorité parentale prend fin
	 * @param dateMajorite       la date d'acquisition de la majorité
	 */
	void publierEvenementFiscalFinAutoriteParentale(PersonnePhysique contribuableEnfant, ContribuableImpositionPersonnesPhysiques contribuableParent, RegDate dateMajorite);

	/**
	 * [UNIREG-3244] Publie un événement de naissance d'un contribuable enfant
	 *
	 * @param contribuableEnfant le contribuable nouveau né
	 * @param contribuableParent le contribuable qui possède l'autorité parentale du nouveau né
	 * @param dateNaissance      la date de naissance
	 */
	void publierEvenementFiscalNaissance(PersonnePhysique contribuableEnfant, ContribuableImpositionPersonnesPhysiques contribuableParent, RegDate dateNaissance);

	/**
	 * Publie un événement fiscal de type 'Changement de situation de famille'
	 * @param date la date de valeur du changement
	 * @param ctb le contribuable ciblé par le changement
	 */
	void publierEvenementFiscalChangementSituationFamille(RegDate date, ContribuableImpositionPersonnesPhysiques ctb);

	/**
	 * Publie un événement fiscal de type 'émission de LR'
	 * @param lr la LR juste émise
	 * @param dateEmission la date d'émission (en général la date du jour)
	 */
	void publierEvenementFiscalEmissionListeRecapitulative(DeclarationImpotSource lr, RegDate dateEmission);

	/**
	 * Publie un événement fiscal de type 'quittancement de LR'
	 * @param lr la LR quittancée
	 * @param dateQuittancement la date de quittancement (qui peut ne pas être la date du jour car le quittancement nous vient d'une source externe qui indique sa propre date)
	 */
	void publierEvenementFiscalQuittancementListeRecapitulative(DeclarationImpotSource lr, RegDate dateQuittancement);

	/**
	 * Publie un événement fiscal de type 'sommation de LR'
	 * @param lr la LR sommée
	 * @param dateSommation la date de sommation (en général la date du jour)
	 */
	void publierEvenementFiscalSommationListeRecapitulative(DeclarationImpotSource lr, RegDate dateSommation);

	/**
	 * Publie un événement fiscal de type 'échéance de LR'
	 * @param lr la LR échue
	 * @param dateEcheance la date d'échéance (en général la date du jour)
	 */
	void publierEvenementFiscalEcheanceListeRecapitulative(DeclarationImpotSource lr, RegDate dateEcheance);

	/**
	 * Publie un événement fiscal de type 'annulation de LR'
	 * @param lr la LR annulée
	 */
	void publierEvenementFiscalAnnulationListeRecapitulative(DeclarationImpotSource lr);

	/**
	 * Publie un événement fiscal de type 'émission de DI'
	 * @param di la déclaration émise
	 * @param dateEmission la date d'émission (en général la date du jour)
	 */
	void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission);

	/**
	 * Publie un événement fiscal de type 'quittancement de DI'
	 * @param di la déclaration quittancée
	 * @param dateQuittance la date de quittance (qui peut ne pas être la date du jour car le quittancement nous vient d'une source externe qui indique sa propre date)
	 */
	void publierEvenementFiscalQuittancementDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateQuittance);

	/**
	 * Publie un événement fiscal de type 'sommation de DI'
	 * @param di la déclaration sommée
	 * @param dateSommation la date de sommation (en général la date du jour)
	 */
	void publierEvenementFiscalSommationDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateSommation);

	/**
	 * Publie un événement fiscal de type 'échéance de DI'
	 * @param di la déclaration échue
	 * @param dateEcheance la date d'échéance (en général la date du jour)
	 */
	void publierEvenementFiscalEcheanceDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEcheance);

	/**
	 * Publie un événement fiscal de type 'annulation de DI'
	 * @param di la déclaration annulée
	 */
	void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di);

	/**
	 * Publie un événement fiscal de type 'ouverture de régime fiscal'
	 * @param rf le régime fiscal nouvellement ouvert
	 */
	void publierEvenementFiscalOuvertureRegimeFiscal(RegimeFiscal rf);

	/**
	 * Publie un événement fiscal de type 'fermeture de régime fiscal'
	 * @param rf le régime fiscal nouvellement fermé (= auquel on vient d'assigner une date de fin de validité)
	 */
	void publierEvenementFiscalFermetureRegimeFiscal(RegimeFiscal rf);

	/**
	 * Publie un événement fiscal de type 'annulation de régime fiscal'
	 * @param rf le régime fiscal nouvellement annulé
	 */
	void publierEvenementFiscalAnnulationRegimeFiscal(RegimeFiscal rf);

	/**
	 * Publie un événement fiscal de type 'ouverture d'allègement fiscal'
	 * @param af l'allègement fiscal nouvellement ouvert
	 */
	void publierEvenementFiscalOuvertureAllegementFiscal(AllegementFiscal af);

	/**
	 * Publie un événement fiscal de type 'fermeture d'allègement fiscal'
	 * @param af l'allègement fiscal nouvellement fermé (= auquel on vient d'assigner une date de fin de validité)
	 */
	void publierEvenementFiscalFermetureAllegementFiscal(AllegementFiscal af);

	/**
	 * Publie un événement fiscal de type 'annulation d'allègement fiscal'
	 * @param af l'allègement fiscal nouvellement annulé
	 */
	void publierEvenementFiscalAnnulationAllegementFiscal(AllegementFiscal af);

	/**
	 * Publie un événement fiscal de type 'information complémentaire'
	 * @param entreprise entreprise concernée par l'événement
	 * @param type type d'information complémentaire
	 * @param dateEvenement date de valeur de l'événement
	 */
	void publierEvenementFiscalInformationComplementaire(Entreprise entreprise, EvenementFiscalInformationComplementaire.TypeInformationComplementaire type, RegDate dateEvenement);

}
