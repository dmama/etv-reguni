package ch.vd.unireg.evenement.fiscal;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.documentfiscal.LettreBienvenue;
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

public class MockEvenementFiscalService implements EvenementFiscalService {

	@Override
	public Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers) {
		return null;
	}

	@Override
	public void publierEvenementFiscalOuvertureFor(ForFiscal forFiscal) {
	}

	@Override
	public void publierEvenementFiscalFermetureFor(ForFiscal forFiscal) {
	}

	@Override
	public void publierEvenementFiscalAnnulationFor(ForFiscal forFiscal) {
	}

	@Override
	public void publierEvenementFiscalChangementModeImposition(ForFiscal forFiscal) {
	}

	@Override
	public void publierEvenementFiscalFinAutoriteParentale(PersonnePhysique contribuableEnfant, ContribuableImpositionPersonnesPhysiques contribuableParent, RegDate dateMajorite) {
	}

	@Override
	public void publierEvenementFiscalNaissance(PersonnePhysique contribuableEnfant, ContribuableImpositionPersonnesPhysiques contribuableParent, RegDate dateNaissance) {
	}

	@Override
	public void publierEvenementFiscalChangementSituationFamille(RegDate date, ContribuableImpositionPersonnesPhysiques ctb) {
	}

	@Override
	public void publierEvenementFiscalEmissionListeRecapitulative(DeclarationImpotSource lr, RegDate dateEmission) {
	}

	@Override
	public void publierEvenementFiscalQuittancementListeRecapitulative(DeclarationImpotSource lr, RegDate dateQuittancement) {
	}

	@Override
	public void publierEvenementFiscalSommationListeRecapitulative(DeclarationImpotSource lr, RegDate dateSommation) {
	}

	@Override
	public void publierEvenementFiscalEcheanceListeRecapitulative(DeclarationImpotSource lr, RegDate dateEcheance) {
	}

	@Override
	public void publierEvenementFiscalAnnulationListeRecapitulative(DeclarationImpotSource lr) {
	}

	@Override
	public void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission) {
	}

	@Override
	public void publierEvenementFiscalQuittancementDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateQuittance) {
	}

	@Override
	public void publierEvenementFiscalSommationDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateSommation) {
	}

	@Override
	public void publierEvenementFiscalEcheanceDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEcheance) {
	}

	@Override
	public void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di) {
	}

	@Override
	public void publierEvenementFiscalEmissionQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateEmission) {
	}

	@Override
	public void publierEvenementFiscalQuittancementQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateQuittance) {
	}

	@Override
	public void publierEvenementFiscalRappelQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateRappel) {
	}

	@Override
	public void publierEvenementFiscalEcheanceQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateEcheance) {
	}

	@Override
	public void publierEvenementFiscalAnnulationQuestionnaireSNC(QuestionnaireSNC qsnc) {
	}

	@Override
	public void publierEvenementFiscalOuvertureRegimeFiscal(RegimeFiscal rf) {
	}

	@Override
	public void publierEvenementFiscalFermetureRegimeFiscal(RegimeFiscal rf) {
	}

	@Override
	public void publierEvenementFiscalAnnulationRegimeFiscal(RegimeFiscal rf) {
	}

	@Override
	public void publierEvenementFiscalOuvertureAllegementFiscal(AllegementFiscal af) {
	}

	@Override
	public void publierEvenementFiscalFermetureAllegementFiscal(AllegementFiscal af) {
	}

	@Override
	public void publierEvenementFiscalAnnulationAllegementFiscal(AllegementFiscal af) {
	}

	@Override
	public void publierEvenementFiscalOuvertureFlagEntreprise(FlagEntreprise flag) {
	}

	@Override
	public void publierEvenementFiscalFermetureFlagEntreprise(FlagEntreprise flag) {
	}

	@Override
	public void publierEvenementFiscalAnnulationFlagEntreprise(FlagEntreprise flag) {
	}

	@Override
	public void publierEvenementFiscalInformationComplementaire(Entreprise entreprise, EvenementFiscalInformationComplementaire.TypeInformationComplementaire type, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalEmissionLettreBienvenue(LettreBienvenue lettre) {
	}

	@Override
	public void publierEvenementFiscalImpressionFourreNeutre(FourreNeutre fourreNeutre, RegDate dateTraitement) {

	}

	@Override
	public void publierCreationBatiment(RegDate dateCreation, BatimentRF batiment) {

	}

	@Override
	public void publierRadiationBatiment(RegDate dateRadiation, BatimentRF batiment) {

	}

	@Override
	public void publierModificationDescriptionBatiment(RegDate dateModification, BatimentRF batiment) {

	}

	@Override
	public void publierOuvertureDroitPropriete(RegDate dateDebutMetier, DroitProprieteRF droit) {

	}

	@Override
	public void publierFermetureDroitPropriete(RegDate dateFinMetier, DroitProprieteRF droit) {

	}

	@Override
	public void publierModificationDroitPropriete(RegDate dateModification, DroitProprieteRF droit) {

	}

	@Override
	public void publierOuvertureServitude(RegDate dateDebut, ServitudeRF servitude) {

	}

	@Override
	public void publierFermetureServitude(RegDate dateFin, ServitudeRF servitude) {

	}

	@Override
	public void publierModificationServitude(RegDate dateModification, ServitudeRF servitude) {

	}

	@Override
	public void publierCreationImmeuble(RegDate dateCreation, ImmeubleRF immeuble) {

	}

	@Override
	public void publierRadiationImmeuble(RegDate dateRadiation, ImmeubleRF immeuble) {

	}

	@Override
	public void publierReactivationImmeuble(RegDate dateReactivation, ImmeubleRF immeuble) {

	}

	@Override
	public void publierModificationEgridImmeuble(RegDate dateModification, ImmeubleRF immeuble) {

	}

	@Override
	public void publierModificationSituationImmeuble(RegDate dateModification, ImmeubleRF immeuble) {

	}

	@Override
	public void publierModificationSurfaceTotaleImmeuble(RegDate dateModification, ImmeubleRF immeuble) {

	}

	@Override
	public void publierModificationSurfaceAuSolImmeuble(RegDate dateModification, ImmeubleRF immeuble) {

	}

	@Override
	public void publierModificationQuotePartImmeuble(RegDate dateModification, ImmeubleRF immeuble) {

	}

	@Override
	public void publierDebutEstimationFiscalImmeuble(RegDate dateDebutMetier, EstimationRF estimation) {

	}

	@Override
	public void publierChangementEnRevisionEstimationFiscalImmeuble(RegDate dateChangement, EstimationRF estimation) {

	}

	@Override
	public void publierFinEstimationFiscalImmeuble(RegDate dateFinMetier, EstimationRF estimation) {

	}

	@Override
	public void publierAnnulationEstimationFiscalImmeuble(RegDate dateAnnulation, EstimationRF estimation) {

	}

	@Override
	public void publierDebutImplantationBatiment(RegDate dateDebut, ImplantationRF implantation) {

	}

	@Override
	public void publierFinImplantationBatiment(RegDate dateFin, ImplantationRF implantation) {

	}

	@Override
	public void publierModificationPrincipalCommunaute(RegDate dateDebut, CommunauteRF communaute) {

	}

	@Override
	public void publierModificationHeritageCommunaute(RegDate dateDebut, CommunauteRF communaute) {

	}

	@Override
	public void publierDebutRapprochementTiersRF(RegDate dateDebut, RapprochementRF rapprochement) {

	}

	@Override
	public void publierFinRapprochementTiersRF(RegDate dateFin, RapprochementRF rapprochement) {

	}

	@Override
	public void publierAnnulationRapprochementTiersRF(RegDate dateAnnulation, RapprochementRF rapprochement) {

	}
}
