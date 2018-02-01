package ch.vd.unireg.evenement.fiscal;

import java.util.Collection;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.documentfiscal.LettreBienvenue;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalBatiment;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalCommunaute;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalDroit;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalDroitPropriete;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalImmeuble;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalImplantationBatiment;
import ch.vd.unireg.evenement.fiscal.registrefoncier.EvenementFiscalServitude;
import ch.vd.unireg.fourreNeutre.FourreNeutre;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.ImplantationRF;
import ch.vd.unireg.registrefoncier.RapprochementRF;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.Tiers;

/**
 * Service des événement fiscaux
 */
public class EvenementFiscalServiceImpl implements EvenementFiscalService {

	private EvenementFiscalDAO evenementFiscalDAO;
	private EvenementFiscalSender evenementFiscalSender;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalDAO(EvenementFiscalDAO evenementFiscalDAO) {
		this.evenementFiscalDAO = evenementFiscalDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementFiscalSender(EvenementFiscalSender evenementFiscalSender) {
		this.evenementFiscalSender = evenementFiscalSender;
	}

	@Override
	public Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers) {
		return evenementFiscalDAO.getEvenementsFiscaux(tiers);
	}

	private void saveAndPublish(EvenementFiscal evenementFiscal) {
		Assert.notNull(evenementFiscal, "evenementFiscal ne peut être null.");

		// sauve evenementFiscal
		evenementFiscal = evenementFiscalDAO.save(evenementFiscal);

		// publication de l'evenementFiscal
		try {
			evenementFiscalSender.sendEvent(evenementFiscal);
		}
		catch (EvenementFiscalException e) {
			throw new RuntimeException("Erreur survenu lors de la publication de l'evenement fiscal [" + evenementFiscal.getId() + "].", e);
		}
	}

	private void publierEvenementFiscalFor(RegDate date, ForFiscal forFiscal, EvenementFiscalFor.TypeEvenementFiscalFor type) {
		saveAndPublish(new EvenementFiscalFor(date, forFiscal, type));
	}

	@Override
	public void publierEvenementFiscalOuvertureFor(ForFiscal forFiscal) {
		publierEvenementFiscalFor(forFiscal.getDateDebut(), forFiscal, EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
	}

	@Override
	public void publierEvenementFiscalFermetureFor(ForFiscal forFiscal) {
		publierEvenementFiscalFor(forFiscal.getDateFin(), forFiscal, EvenementFiscalFor.TypeEvenementFiscalFor.FERMETURE);
	}

	@Override
	public void publierEvenementFiscalAnnulationFor(ForFiscal forFiscal) {
		publierEvenementFiscalFor(forFiscal.getDateDebut(), forFiscal, EvenementFiscalFor.TypeEvenementFiscalFor.ANNULATION);
	}

	@Override
	public void publierEvenementFiscalChangementModeImposition(ForFiscal forFiscal) {
		publierEvenementFiscalFor(forFiscal.getDateDebut(), forFiscal, EvenementFiscalFor.TypeEvenementFiscalFor.CHGT_MODE_IMPOSITION);
	}

	private void publierEvenementFiscalParente(RegDate date, PersonnePhysique enfant, ContribuableImpositionPersonnesPhysiques parent, EvenementFiscalParente.TypeEvenementFiscalParente type) {
		saveAndPublish(new EvenementFiscalParente(parent, date, enfant, type));
	}

	@Override
	public void publierEvenementFiscalFinAutoriteParentale(PersonnePhysique contribuableEnfant, ContribuableImpositionPersonnesPhysiques contribuableParent, RegDate dateMajorite) {
		publierEvenementFiscalParente(dateMajorite, contribuableEnfant, contribuableParent, EvenementFiscalParente.TypeEvenementFiscalParente.FIN_AUTORITE_PARENTALE);
	}

	@Override
	public void publierEvenementFiscalNaissance(PersonnePhysique contribuableEnfant, ContribuableImpositionPersonnesPhysiques contribuableParent, RegDate dateNaissance) {
		publierEvenementFiscalParente(dateNaissance, contribuableEnfant, contribuableParent, EvenementFiscalParente.TypeEvenementFiscalParente.NAISSANCE);
	}

	@Override
	public void publierEvenementFiscalChangementSituationFamille(RegDate date, ContribuableImpositionPersonnesPhysiques ctb) {
		saveAndPublish(new EvenementFiscalSituationFamille(date, ctb));
	}

	@Override
	public void publierEvenementFiscalEmissionListeRecapitulative(DeclarationImpotSource lr, RegDate dateEmission) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(dateEmission, lr, EvenementFiscalDeclarationSommable.TypeAction.EMISSION));
	}

	@Override
	public void publierEvenementFiscalQuittancementListeRecapitulative(DeclarationImpotSource lr, RegDate dateQuittancement) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(dateQuittancement, lr, EvenementFiscalDeclarationSommable.TypeAction.QUITTANCEMENT));
	}

	@Override
	public void publierEvenementFiscalAnnulationListeRecapitulative(DeclarationImpotSource lr) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(RegDateHelper.get(lr.getAnnulationDate()), lr, EvenementFiscalDeclarationSommable.TypeAction.ANNULATION));
	}

	@Override
	public void publierEvenementFiscalSommationListeRecapitulative(DeclarationImpotSource lr, RegDate dateSommation) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(dateSommation, lr, EvenementFiscalDeclarationSommable.TypeAction.SOMMATION));
	}

	@Override
	public void publierEvenementFiscalEcheanceListeRecapitulative(DeclarationImpotSource lr, RegDate dateEcheance) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(dateEcheance, lr, EvenementFiscalDeclarationSommable.TypeAction.ECHEANCE));
	}

	@Override
	public void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(dateEmission, di, EvenementFiscalDeclarationSommable.TypeAction.EMISSION));
	}

	@Override
	public void publierEvenementFiscalQuittancementDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateQuittance) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(dateQuittance, di, EvenementFiscalDeclarationSommable.TypeAction.QUITTANCEMENT));
	}

	@Override
	public void publierEvenementFiscalSommationDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateSommation) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(dateSommation, di, EvenementFiscalDeclarationSommable.TypeAction.SOMMATION));
	}

	@Override
	public void publierEvenementFiscalEcheanceDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEcheance) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(dateEcheance, di, EvenementFiscalDeclarationSommable.TypeAction.ECHEANCE));
	}

	@Override
	public void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di) {
		saveAndPublish(new EvenementFiscalDeclarationSommable(RegDateHelper.get(di.getAnnulationDate()), di, EvenementFiscalDeclarationSommable.TypeAction.ANNULATION));
	}

	@Override
	public void publierEvenementFiscalEmissionQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateEmission) {
		saveAndPublish(new EvenementFiscalDeclarationRappelable(dateEmission, qsnc, EvenementFiscalDeclarationRappelable.TypeAction.EMISSION));
	}

	@Override
	public void publierEvenementFiscalQuittancementQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateQuittance) {
		saveAndPublish(new EvenementFiscalDeclarationRappelable(dateQuittance, qsnc, EvenementFiscalDeclarationRappelable.TypeAction.QUITTANCEMENT));
	}

	@Override
	public void publierEvenementFiscalRappelQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateRappel) {
		saveAndPublish(new EvenementFiscalDeclarationRappelable(dateRappel, qsnc, EvenementFiscalDeclarationRappelable.TypeAction.RAPPEL));
	}

	@Override
	public void publierEvenementFiscalAnnulationQuestionnaireSNC(QuestionnaireSNC qsnc) {
		saveAndPublish(new EvenementFiscalDeclarationRappelable(RegDateHelper.get(qsnc.getAnnulationDate()), qsnc, EvenementFiscalDeclarationRappelable.TypeAction.ANNULATION));
	}

	private void publierEvenementFiscalRegimeFiscal(RegDate date, RegimeFiscal rf, EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime type) {
		saveAndPublish(new EvenementFiscalRegimeFiscal(date, rf, type));
	}

	@Override
	public void publierEvenementFiscalOuvertureRegimeFiscal(RegimeFiscal rf) {
		publierEvenementFiscalRegimeFiscal(rf.getDateDebut(), rf, EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.OUVERTURE);
	}

	@Override
	public void publierEvenementFiscalFermetureRegimeFiscal(RegimeFiscal rf) {
		publierEvenementFiscalRegimeFiscal(rf.getDateFin(), rf, EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.FERMETURE);
	}

	@Override
	public void publierEvenementFiscalAnnulationRegimeFiscal(RegimeFiscal rf) {
		publierEvenementFiscalRegimeFiscal(rf.getDateDebut(), rf, EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.ANNULATION);
	}

	private void publierEvenementFiscalAllegementFiscal(RegDate date, AllegementFiscal af, EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement type) {
		saveAndPublish(new EvenementFiscalAllegementFiscal(date, af, type));
	}

	@Override
	public void publierEvenementFiscalOuvertureAllegementFiscal(AllegementFiscal af) {
		publierEvenementFiscalAllegementFiscal(af.getDateDebut(), af, EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement.OUVERTURE);
	}

	@Override
	public void publierEvenementFiscalFermetureAllegementFiscal(AllegementFiscal af) {
		publierEvenementFiscalAllegementFiscal(af.getDateFin(), af, EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement.FERMETURE);
	}

	@Override
	public void publierEvenementFiscalAnnulationAllegementFiscal(AllegementFiscal af) {
		publierEvenementFiscalAllegementFiscal(af.getDateDebut(), af, EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement.ANNULATION);
	}

	private void publierEvenementFiscalFlagEntreprise(RegDate date, FlagEntreprise flag, EvenementFiscalFlagEntreprise.TypeEvenementFiscalFlagEntreprise type) {
		saveAndPublish(new EvenementFiscalFlagEntreprise(date, flag, type));
	}

	@Override
	public void publierEvenementFiscalOuvertureFlagEntreprise(FlagEntreprise flag) {
		publierEvenementFiscalFlagEntreprise(flag.getDateDebut(), flag, EvenementFiscalFlagEntreprise.TypeEvenementFiscalFlagEntreprise.OUVERTURE);
	}

	@Override
	public void publierEvenementFiscalFermetureFlagEntreprise(FlagEntreprise flag) {
		publierEvenementFiscalFlagEntreprise(flag.getDateFin(), flag, EvenementFiscalFlagEntreprise.TypeEvenementFiscalFlagEntreprise.FERMETURE);
	}

	@Override
	public void publierEvenementFiscalAnnulationFlagEntreprise(FlagEntreprise flag) {
		publierEvenementFiscalFlagEntreprise(flag.getDateDebut(), flag, EvenementFiscalFlagEntreprise.TypeEvenementFiscalFlagEntreprise.ANNULATION);
	}

	@Override
	public void publierEvenementFiscalInformationComplementaire(Entreprise entreprise, EvenementFiscalInformationComplementaire.TypeInformationComplementaire type, RegDate dateEvenement) {
		saveAndPublish(new EvenementFiscalInformationComplementaire(entreprise, dateEvenement, type));
	}

	@Override
	public void publierEvenementFiscalEmissionLettreBienvenue(LettreBienvenue lettre) {
		saveAndPublish(new EvenementFiscalEnvoiLettreBienvenue(lettre.getEntreprise(), lettre.getDateEnvoi()));
	}

	@Override
	public void publierEvenementFiscalImpressionFourreNeutre(FourreNeutre fourreNeutre, RegDate dateTraitement) {
		saveAndPublish(new EvenementFiscalImpressionFourreNeutre(fourreNeutre.getTiers(), fourreNeutre.getPeriodeFiscale(), dateTraitement));
	}

	@Override
	public void publierCreationBatiment(RegDate dateCreation, BatimentRF batiment) {
		saveAndPublish(new EvenementFiscalBatiment(dateCreation, batiment, EvenementFiscalBatiment.TypeEvenementFiscalBatiment.CREATION));
	}

	@Override
	public void publierRadiationBatiment(RegDate dateRadiation, BatimentRF batiment) {
		saveAndPublish(new EvenementFiscalBatiment(dateRadiation, batiment, EvenementFiscalBatiment.TypeEvenementFiscalBatiment.RADIATION));
	}

	@Override
	public void publierModificationDescriptionBatiment(RegDate dateModification, BatimentRF batiment) {
		saveAndPublish(new EvenementFiscalBatiment(dateModification, batiment, EvenementFiscalBatiment.TypeEvenementFiscalBatiment.MODIFICATION_DESCRIPTION));
	}

	@Override
	public void publierOuvertureDroitPropriete(RegDate dateDebutMetier, DroitProprieteRF droit) {
		saveAndPublish(new EvenementFiscalDroitPropriete(dateDebutMetier, droit, EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE));
	}

	@Override
	public void publierFermetureDroitPropriete(RegDate dateFinMetier, DroitProprieteRF droit) {
		saveAndPublish(new EvenementFiscalDroitPropriete(dateFinMetier, droit, EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE));
	}

	@Override
	public void publierModificationDroitPropriete(RegDate dateModification, DroitProprieteRF droit) {
		saveAndPublish(new EvenementFiscalDroitPropriete(dateModification, droit, EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION));
	}

	@Override
	public void publierOuvertureServitude(RegDate dateDebut, ServitudeRF servitude) {
		saveAndPublish(new EvenementFiscalServitude(dateDebut, servitude, EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE));
	}

	@Override
	public void publierFermetureServitude(RegDate dateFin, ServitudeRF servitude) {
		saveAndPublish(new EvenementFiscalServitude(dateFin, servitude, EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.FERMETURE));
	}

	@Override
	public void publierModificationServitude(RegDate dateModification, ServitudeRF servitude) {
		saveAndPublish(new EvenementFiscalServitude(dateModification, servitude, EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.MODIFICATION));
	}

	@Override
	public void publierCreationImmeuble(RegDate dateCreation, ImmeubleRF immeuble) {
		saveAndPublish(new EvenementFiscalImmeuble(dateCreation, immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.CREATION));
	}

	@Override
	public void publierRadiationImmeuble(RegDate dateRadiation, ImmeubleRF immeuble) {
		saveAndPublish(new EvenementFiscalImmeuble(dateRadiation, immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.RADIATION));
	}

	@Override
	public void publierReactivationImmeuble(RegDate dateReactivation, ImmeubleRF immeuble) {
		saveAndPublish(new EvenementFiscalImmeuble(dateReactivation, immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.REACTIVATION));
	}

	@Override
	public void publierModificationEgridImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		saveAndPublish(new EvenementFiscalImmeuble(dateModification, immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_EGRID));
	}

	@Override
	public void publierModificationSituationImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		saveAndPublish(new EvenementFiscalImmeuble(dateModification, immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_SITUATION));
	}

	@Override
	public void publierModificationSurfaceTotaleImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		saveAndPublish(new EvenementFiscalImmeuble(dateModification, immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_SURFACE_TOTALE));
	}

	@Override
	public void publierModificationSurfaceAuSolImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		saveAndPublish(new EvenementFiscalImmeuble(dateModification, immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_SURFACE_AU_SOL));
	}

	@Override
	public void publierModificationQuotePartImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		saveAndPublish(new EvenementFiscalImmeuble(dateModification, immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_QUOTE_PART));
	}

	@Override
	public void publierDebutEstimationFiscalImmeuble(RegDate dateDebutMetier, EstimationRF estimation) {
		saveAndPublish(new EvenementFiscalImmeuble(dateDebutMetier, estimation.getImmeuble(), EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.DEBUT_ESTIMATION));
	}

	@Override
	public void publierChangementEnRevisionEstimationFiscalImmeuble(RegDate dateChangement, EstimationRF estimation) {
		saveAndPublish(new EvenementFiscalImmeuble(dateChangement, estimation.getImmeuble(), EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_STATUT_REVISION_ESTIMATION));
	}

	@Override
	public void publierFinEstimationFiscalImmeuble(RegDate dateFinMetier, EstimationRF estimation) {
		saveAndPublish(new EvenementFiscalImmeuble(dateFinMetier, estimation.getImmeuble(), EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.FIN_ESTIMATION));
	}

	@Override
	public void publierAnnulationEstimationFiscalImmeuble(RegDate dateAnnulation, EstimationRF estimation) {
		saveAndPublish(new EvenementFiscalImmeuble(dateAnnulation, estimation.getImmeuble(), EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.ANNULATION_ESTIMATION));
	}

	@Override
	public void publierDebutImplantationBatiment(RegDate dateDebut, ImplantationRF implantation) {
		saveAndPublish(new EvenementFiscalImplantationBatiment(dateDebut, implantation, EvenementFiscalImplantationBatiment.TypeEvenementFiscalImplantation.CREATION));
	}

	@Override
	public void publierFinImplantationBatiment(RegDate dateFin, ImplantationRF implantation) {
		saveAndPublish(new EvenementFiscalImplantationBatiment(dateFin, implantation, EvenementFiscalImplantationBatiment.TypeEvenementFiscalImplantation.RADIATION));
	}

	@Override
	public void publierModificationPrincipalCommunaute(RegDate dateDebut, CommunauteRF communaute) {
		saveAndPublish(new EvenementFiscalCommunaute(dateDebut, communaute, EvenementFiscalCommunaute.TypeEvenementFiscalCommunaute.CHANGEMENT_PRINCIPAL));
	}

	@Override
	public void publierModificationHeritageCommunaute(RegDate dateDebut, CommunauteRF communaute) {
		saveAndPublish(new EvenementFiscalCommunaute(dateDebut, communaute, EvenementFiscalCommunaute.TypeEvenementFiscalCommunaute.HERITAGE));
	}

	@Override
	public void publierDebutRapprochementTiersRF(RegDate dateDebut, RapprochementRF rapprochement) {
		saveAndPublish(new EvenementFiscalRapprochementTiersRF(dateDebut, rapprochement, EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE));
	}

	@Override
	public void publierFinRapprochementTiersRF(RegDate dateFin, RapprochementRF rapprochement) {
		saveAndPublish(new EvenementFiscalRapprochementTiersRF(dateFin, rapprochement, EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.FERMETURE));
	}

	@Override
	public void publierAnnulationRapprochementTiersRF(RegDate dateAnnulation, RapprochementRF rapprochement) {
		saveAndPublish(new EvenementFiscalRapprochementTiersRF(dateAnnulation, rapprochement, EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.ANNULATION));
	}
}
