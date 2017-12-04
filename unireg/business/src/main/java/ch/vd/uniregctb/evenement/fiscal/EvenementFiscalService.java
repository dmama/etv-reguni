package ch.vd.uniregctb.evenement.fiscal;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.fourreNeutre.FourreNeutre;
import ch.vd.uniregctb.registrefoncier.BatimentRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ImplantationRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
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
	 *
	 * @param forFiscal le for fiscal ouvert pour lequel on publie l'événement
	 */
	void publierEvenementFiscalOuvertureFor(ForFiscal forFiscal);

	/**
	 * Publie un événement fiscal de type 'Fermeture de for'
	 *
	 * @param forFiscal le for fermé ouvert pour lequel on publie l'événement
	 */
	void publierEvenementFiscalFermetureFor(ForFiscal forFiscal);

	/**
	 * Publie un événement fiscal de type 'Annulation de for'
	 *
	 * @param forFiscal le for fiscal qui vient d'être annulé
	 */
	void publierEvenementFiscalAnnulationFor(ForFiscal forFiscal);

	/**
	 * Publie un événement fiscal de type 'Changement de mode d'imposition'
	 *
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
	 *
	 * @param date la date de valeur du changement
	 * @param ctb  le contribuable ciblé par le changement
	 */
	void publierEvenementFiscalChangementSituationFamille(RegDate date, ContribuableImpositionPersonnesPhysiques ctb);

	/**
	 * Publie un événement fiscal de type 'émission de LR'
	 *
	 * @param lr           la LR juste émise
	 * @param dateEmission la date d'émission (en général la date du jour)
	 */
	void publierEvenementFiscalEmissionListeRecapitulative(DeclarationImpotSource lr, RegDate dateEmission);

	/**
	 * Publie un événement fiscal de type 'quittancement de LR'
	 *
	 * @param lr                la LR quittancée
	 * @param dateQuittancement la date de quittancement (qui peut ne pas être la date du jour car le quittancement nous vient d'une source externe qui indique sa propre date)
	 */
	void publierEvenementFiscalQuittancementListeRecapitulative(DeclarationImpotSource lr, RegDate dateQuittancement);

	/**
	 * Publie un événement fiscal de type 'sommation de LR'
	 *
	 * @param lr            la LR sommée
	 * @param dateSommation la date de sommation (en général la date du jour)
	 */
	void publierEvenementFiscalSommationListeRecapitulative(DeclarationImpotSource lr, RegDate dateSommation);

	/**
	 * Publie un événement fiscal de type 'échéance de LR'
	 *
	 * @param lr           la LR échue
	 * @param dateEcheance la date d'échéance (en général la date du jour)
	 */
	void publierEvenementFiscalEcheanceListeRecapitulative(DeclarationImpotSource lr, RegDate dateEcheance);

	/**
	 * Publie un événement fiscal de type 'annulation de LR'
	 *
	 * @param lr la LR annulée
	 */
	void publierEvenementFiscalAnnulationListeRecapitulative(DeclarationImpotSource lr);

	/**
	 * Publie un événement fiscal de type 'émission de DI'
	 *
	 * @param di           la déclaration émise
	 * @param dateEmission la date d'émission (en général la date du jour)
	 */
	void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission);

	/**
	 * Publie un événement fiscal de type 'quittancement de DI'
	 *
	 * @param di            la déclaration quittancée
	 * @param dateQuittance la date de quittance (qui peut ne pas être la date du jour car le quittancement nous vient d'une source externe qui indique sa propre date)
	 */
	void publierEvenementFiscalQuittancementDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateQuittance);

	/**
	 * Publie un événement fiscal de type 'sommation de DI'
	 *
	 * @param di            la déclaration sommée
	 * @param dateSommation la date de sommation (en général la date du jour)
	 */
	void publierEvenementFiscalSommationDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateSommation);

	/**
	 * Publie un événement fiscal de type 'échéance de DI'
	 *
	 * @param di           la déclaration échue
	 * @param dateEcheance la date d'échéance (en général la date du jour)
	 */
	void publierEvenementFiscalEcheanceDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEcheance);

	/**
	 * Publie un événement fiscal de type 'annulation de DI'
	 *
	 * @param di la déclaration annulée
	 */
	void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di);

	/**
	 * Publie un événement fiscal de type 'émission de questionnaire SNC'
	 *
	 * @param qsnc         le questionnaire SNC émis
	 * @param dateEmission la date d'émission (en général la date du jour)
	 */
	void publierEvenementFiscalEmissionQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateEmission);

	/**
	 * Publie un événement fiscal de type 'quittancement de questionnaire SNC'
	 *
	 * @param qsnc          le questionnaire quittancé
	 * @param dateQuittance la date de quittance (qui peut ne pas être la date du jour car le quittancement nous vient d'une source externe qui indique sa propre date)
	 */
	void publierEvenementFiscalQuittancementQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateQuittance);

	/**
	 * Publie un événement fiscal de type 'rappel de questionnaire SNC'
	 *
	 * @param qsnc       le questionnaire rappelé
	 * @param dateRappel la date de rappel (en général la date du jour)
	 */
	void publierEvenementFiscalRappelQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateRappel);

	/**
	 * Publie un événement fiscal de type 'annulation de questionnaire SNC'
	 *
	 * @param qsnc le questionnaire SNC
	 */
	void publierEvenementFiscalAnnulationQuestionnaireSNC(QuestionnaireSNC qsnc);

	/**
	 * Publie un événement fiscal de type 'ouverture de régime fiscal'
	 *
	 * @param rf le régime fiscal nouvellement ouvert
	 */
	void publierEvenementFiscalOuvertureRegimeFiscal(RegimeFiscal rf);

	/**
	 * Publie un événement fiscal de type 'fermeture de régime fiscal'
	 *
	 * @param rf le régime fiscal nouvellement fermé (= auquel on vient d'assigner une date de fin de validité)
	 */
	void publierEvenementFiscalFermetureRegimeFiscal(RegimeFiscal rf);

	/**
	 * Publie un événement fiscal de type 'annulation de régime fiscal'
	 *
	 * @param rf le régime fiscal nouvellement annulé
	 */
	void publierEvenementFiscalAnnulationRegimeFiscal(RegimeFiscal rf);

	/**
	 * Publie un événement fiscal de type 'ouverture d'allègement fiscal'
	 *
	 * @param af l'allègement fiscal nouvellement ouvert
	 */
	void publierEvenementFiscalOuvertureAllegementFiscal(AllegementFiscal af);

	/**
	 * Publie un événement fiscal de type 'fermeture d'allègement fiscal'
	 *
	 * @param af l'allègement fiscal nouvellement fermé (= auquel on vient d'assigner une date de fin de validité)
	 */
	void publierEvenementFiscalFermetureAllegementFiscal(AllegementFiscal af);

	/**
	 * Publie un événement fiscal de type 'annulation d'allègement fiscal'
	 *
	 * @param af l'allègement fiscal nouvellement annulé
	 */
	void publierEvenementFiscalAnnulationAllegementFiscal(AllegementFiscal af);

	/**
	 * Publie un événement fiscal de type 'ouverture de flag entreprise'
	 *
	 * @param flag le flag entreprise nouvellement ouvert
	 */
	void publierEvenementFiscalOuvertureFlagEntreprise(FlagEntreprise flag);

	/**
	 * Publie un événement fiscal de type 'fermeture de flag entreprise'
	 *
	 * @param flag le flag entreprise nouvellement fermé (= auquel on vient d'assigner une date de fin de validité)
	 */
	void publierEvenementFiscalFermetureFlagEntreprise(FlagEntreprise flag);

	/**
	 * Publie un événement fiscal de type 'annulation de flag entreprise'
	 *
	 * @param flag le flag entreprise nouvellement annulé
	 */
	void publierEvenementFiscalAnnulationFlagEntreprise(FlagEntreprise flag);

	/**
	 * Publie un événement fiscal de type 'information complémentaire'
	 *
	 * @param entreprise    entreprise concernée par l'événement
	 * @param type          type d'information complémentaire
	 * @param dateEvenement date de valeur de l'événement
	 */
	void publierEvenementFiscalInformationComplementaire(Entreprise entreprise, EvenementFiscalInformationComplementaire.TypeInformationComplementaire type, RegDate dateEvenement);

	/**
	 * Publie un événement fiscal de type 'émission de lettre de bienvenue'
	 *
	 * @param lettre la lettre émise
	 */
	void publierEvenementFiscalEmissionLettreBienvenue(LettreBienvenue lettre);

	/**
	 * Publie un evenement fiscal de type 'Impression d'une fourre neutre'
	 *
	 * @param fourreNeutre   concernée pat l'event
	 * @param dateTraitement la date valeur de l'impression
	 */
	void publierEvenementFiscalImpressionFourreNeutre(FourreNeutre fourreNeutre, RegDate dateTraitement);

	/**
	 * Publie un événement de création d'un bâtiment.
	 *
	 * @param dateCreation la date de création (ou d'apparition) du bâtiment.
	 * @param batiment     le bâtiment en question.
	 */
	void publierCreationBatiment(RegDate dateCreation, BatimentRF batiment);

	/**
	 * Publie un événement de radiation d'un bâtiment.
	 *
	 * @param dateRadiation la date de radiation (ou de disparition) du bâtiment.
	 * @param batiment      le bâtiment en question.
	 */
	void publierRadiationBatiment(RegDate dateRadiation, BatimentRF batiment);

	/**
	 * Publie un événement de modification de la description d'un bâtiment.
	 *
	 * @param dateModification la date de modification de la description.
	 * @param batiment         le bâtiment en question.
	 */
	void publierModificationDescriptionBatiment(RegDate dateModification, BatimentRF batiment);

	/**
	 * Publie un événement d'ouverture d'un droit de propriété sur un immeuble.
	 *
	 * @param dateDebutMetier la date de début métier du droit de propriété.
	 * @param droit           le droit en question.
	 */
	void publierOuvertureDroitPropriete(RegDate dateDebutMetier, DroitProprieteRF droit);

	/**
	 * Publie un événement de fermeture d'un droit de propriété sur un immeuble.
	 *
	 * @param dateFinMetier la date de fin métier du droit de propriété.
	 * @param droit         le droit en question.
	 */
	void publierFermetureDroitPropriete(RegDate dateFinMetier, DroitProprieteRF droit);

	/**
	 * Publie un événement de modification d'un droit de propriété sur un immeuble.
	 *
	 * @param dateModification la date de modification du droit de propriété.
	 * @param droit            le droit en qestion.
	 */
	void publierModificationDroitPropriete(RegDate dateModification, DroitProprieteRF droit);

	/**
	 * Publie un événement d'ouverture d'une servitude sur un immeuble.
	 *
	 * @param dateDebut la date de début métier de la servitude.
	 * @param servitude la servitude en question.
	 */
	void publierOuvertureServitude(RegDate dateDebut, ServitudeRF servitude);

	/**
	 * Publie un événement de fermeture d'une servitude sur un immeuble.
	 *
	 * @param dateFin   la date de fin métier de la servitude.
	 * @param servitude la servitude en question.
	 */
	void publierFermetureServitude(RegDate dateFin, ServitudeRF servitude);

	/**
	 * Publie un événement de modification d'une servitude sur un immeuble.
	 *
	 * @param dateModification la date de modificaiton de la servitude.
	 * @param servitude        la servitude en question.
	 */
	void publierModificationServitude(RegDate dateModification, ServitudeRF servitude);

	/**
	 * Publie un événement de création d'un immeuble.
	 *
	 * @param dateCreation la date de création (ou d'apparition) de l'immeuble.
	 * @param immeuble     l'immeuble en question.
	 */
	void publierCreationImmeuble(RegDate dateCreation, ImmeubleRF immeuble);

	/**
	 * Publie un événement de radiation d'un immeuble.
	 *
	 * @param dateRadiation la date de radiation (ou de disparition) de l'immeuble.
	 * @param immeuble      l'immeuble en question.
	 */
	void publierRadiationImmeuble(RegDate dateRadiation, ImmeubleRF immeuble);

	/**
	 * Publie un événement de réactivation d'un immeuble.
	 *
	 * @param dateReactivation la date de réactivation de l'immeuble.
	 * @param immeuble         l'immeuble en question.
	 */
	void publierReactivationImmeuble(RegDate dateReactivation, ImmeubleRF immeuble);

	/**
	 * Publie un événement de modification de l'egrid d'un immeuble.
	 *
	 * @param dateModification la date de modification de l'egrid de l'immeuble.
	 * @param immeuble         l'immeuble en question.
	 */
	void publierModificationEgridImmeuble(RegDate dateModification, ImmeubleRF immeuble);

	/**
	 * Publie un événement de modification de la situation d'un immeuble.
	 *
	 * @param dateModification la date de modification de la situation de l'immeuble.
	 * @param immeuble         l'immeuble en question.
	 */
	void publierModificationSituationImmeuble(RegDate dateModification, ImmeubleRF immeuble);

	/**
	 * Publie un événement de modification de la surface totale d'un immeuble.
	 *
	 * @param dateModification la date de modification de la surface totale de l'immeuble.
	 * @param immeuble         l'immeuble en question.
	 */
	void publierModificationSurfaceTotaleImmeuble(RegDate dateModification, ImmeubleRF immeuble);

	/**
	 * Publie un événement de modification d'une surface au sol d'un immeuble.
	 *
	 * @param dateModification la date de modification d'une surface au sol de l'immeuble.
	 * @param immeuble         l'immeuble en question.
	 */
	void publierModificationSurfaceAuSolImmeuble(RegDate dateModification, ImmeubleRF immeuble);

	/**
	 * Publie un événement de modification d'une quote-part d'un immeuble.
	 *
	 * @param dateModification la date de modification d'une quote-part de l'immeuble.
	 * @param immeuble         l'immeuble en question.
	 */
	void publierModificationQuotePartImmeuble(RegDate dateModification, ImmeubleRF immeuble);

	/**
	 * Publie un événement de début d'estimation fiscale d'un immeuble.
	 *
	 * @param dateDebutMetier la date de début métier d'une estimation fiscale d'un immeuble.
	 * @param estimation      l'estimation en question.
	 */
	void publierDebutEstimationFiscalImmeuble(RegDate dateDebutMetier, EstimationRF estimation);

	/**
	 * Publie un événement de changement du flag <i>en révision</i> d'une estimation fiscale d'un immeuble.
	 *
	 * @param dateChangement la date de changement du flag.
	 * @param estimation     l'estimation en question.
	 */
	void publierChangementEnRevisionEstimationFiscalImmeuble(RegDate dateChangement, EstimationRF estimation);

	/**
	 * Publie un événement de fin d'estimation fiscale d'un immeuble.
	 *
	 * @param dateFinMetier la date de fin métier d'une estimation fiscale d'un immeuble.
	 * @param estimation    l'estimation en question.
	 */
	void publierFinEstimationFiscalImmeuble(RegDate dateFinMetier, EstimationRF estimation);

	/**
	 * Publie un événement d'annulation d'estimation fiscale d'un immeuble.
	 *
	 * @param dateAnnulation la date d'annulation d'une estimation fiscale d'un immeuble.
	 * @param estimation     l'estimation en question.
	 */
	void publierAnnulationEstimationFiscalImmeuble(RegDate dateAnnulation, EstimationRF estimation);

	/**
	 * Publie un événement de début d'implantation d'un bâtiment.
	 *
	 * @param dateDebut    la date de début de l'implantation d'un bâtiment.
	 * @param implantation l'implantation en question.
	 */
	void publierDebutImplantationBatiment(RegDate dateDebut, ImplantationRF implantation);

	/**
	 * Publie un événement de fin d'implantation d'un bâtiment.
	 *
	 * @param dateFin      la date de fin de l'implantation d'un bâtiment.
	 * @param implantation l'implantation en question.
	 */
	void publierFinImplantationBatiment(RegDate dateFin, ImplantationRF implantation);

	/**
	 * Publie un événement de modification du principal d'une communauté.
	 *  @param dateDebut la date de début de validité du principal
	 * @param communaute       la communauté en question
	 */
	void publierModificationPrincipalCommunaute(RegDate dateDebut, CommunauteRF communaute);

	/**
	 * Publie un événement de modification d'une communauté suite au décès et à l'ouverture de l'héritage d'un membre de la communauté.
	 *
	 * @param dateDebut  la date de début de l'héritage (ou du changement de l'héritage)
	 * @param communaute la communauté en question
	 */
	void publierModificationHeritageCommunaute(RegDate dateDebut, CommunauteRF communaute);

	/**
	 * Publie un événement de début de rapprochement entre un tiers Unireg et un tiers RF.
	 *
	 * @param dateDebut     la date de début du rapprochement
	 * @param rapprochement le rapprochement en question.
	 */
	void publierDebutRapprochementTiersRF(RegDate dateDebut, RapprochementRF rapprochement);

	/**
	 * Publie un événement de fin de rapprochement entre un tiers Unireg et un tiers RF.
	 *
	 * @param dateFin       la date de fin du rapprochement
	 * @param rapprochement le rapprochement en question.
	 */
	void publierFinRapprochementTiersRF(RegDate dateFin, RapprochementRF rapprochement);

	/**
	 * Publie un événement d'annulation du rapprochement entre un tiers Unireg et un tiers RF.
	 *
	 * @param dateAnnulation la date d'annulation du rapprochement
	 * @param rapprochement  le rapprochement en question.
	 */
	void publierAnnulationRapprochementTiersRF(RegDate dateAnnulation, RapprochementRF rapprochement);
}
