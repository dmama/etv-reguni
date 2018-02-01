package ch.vd.unireg.tiers.etats;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.documentfiscal.LettreBienvenue;
import ch.vd.unireg.evenement.fiscal.EvenementFiscal;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
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
 * @author Raphaël Marmier, 2016-06-28, <raphael.marmier@vd.ch>
 */
public class EvenementFiscalMockService implements EvenementFiscalService {
	private boolean calledOnce = false;
	private Entreprise entreprise;
	private EvenementFiscalInformationComplementaire.TypeInformationComplementaire type;
	private RegDate dateEvenement;

	@Override
	public Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalOuvertureFor(ForFiscal forFiscal) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalFermetureFor(ForFiscal forFiscal) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalAnnulationFor(ForFiscal forFiscal) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalChangementModeImposition(ForFiscal forFiscal) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalFinAutoriteParentale(PersonnePhysique contribuableEnfant, ContribuableImpositionPersonnesPhysiques contribuableParent, RegDate dateMajorite) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalNaissance(PersonnePhysique contribuableEnfant, ContribuableImpositionPersonnesPhysiques contribuableParent, RegDate dateNaissance) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalChangementSituationFamille(RegDate date, ContribuableImpositionPersonnesPhysiques ctb) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalEmissionListeRecapitulative(DeclarationImpotSource lr, RegDate dateEmission) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalQuittancementListeRecapitulative(DeclarationImpotSource lr, RegDate dateQuittancement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalSommationListeRecapitulative(DeclarationImpotSource lr, RegDate dateSommation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalEcheanceListeRecapitulative(DeclarationImpotSource lr, RegDate dateEcheance) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalAnnulationListeRecapitulative(DeclarationImpotSource lr) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalQuittancementDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateQuittance) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalSommationDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateSommation) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void publierEvenementFiscalEcheanceDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEcheance) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void publierEvenementFiscalEmissionQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateEmission) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void publierEvenementFiscalQuittancementQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateQuittance) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalRappelQuestionnaireSNC(QuestionnaireSNC qsnc, RegDate dateRappel) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalAnnulationQuestionnaireSNC(QuestionnaireSNC qsnc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalOuvertureRegimeFiscal(RegimeFiscal rf) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalFermetureRegimeFiscal(RegimeFiscal rf) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalAnnulationRegimeFiscal(RegimeFiscal rf) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void publierEvenementFiscalOuvertureAllegementFiscal(AllegementFiscal af) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void publierEvenementFiscalFermetureAllegementFiscal(AllegementFiscal af) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalAnnulationAllegementFiscal(AllegementFiscal af) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalOuvertureFlagEntreprise(FlagEntreprise flag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalFermetureFlagEntreprise(FlagEntreprise flag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalAnnulationFlagEntreprise(FlagEntreprise flag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalInformationComplementaire(Entreprise entreprise, EvenementFiscalInformationComplementaire.TypeInformationComplementaire type, RegDate dateEvenement) {
		if (calledOnce) {
			throw new IllegalStateException("publierEvenementFiscalInformationComplementaire() déjà appelé une fois dans le mock!");
		}
		this.calledOnce = true;
		this.entreprise = entreprise;
		this.type = type;
		this.dateEvenement = dateEvenement;
	}

	@Override
	public void publierEvenementFiscalEmissionLettreBienvenue(LettreBienvenue lettre) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierEvenementFiscalImpressionFourreNeutre(FourreNeutre fourreNeutre, RegDate dateTraitement) {

	}

	@Override
	public void publierCreationBatiment(RegDate dateCreation, BatimentRF batiment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierRadiationBatiment(RegDate dateRadiation, BatimentRF batiment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationDescriptionBatiment(RegDate dateModification, BatimentRF batiment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierOuvertureDroitPropriete(RegDate dateDebutMetier, DroitProprieteRF droit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierFermetureDroitPropriete(RegDate dateFinMetier, DroitProprieteRF droit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationDroitPropriete(RegDate dateModification, DroitProprieteRF droit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierOuvertureServitude(RegDate dateDebut, ServitudeRF servitude) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierFermetureServitude(RegDate dateFin, ServitudeRF servitude) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationServitude(RegDate dateModification, ServitudeRF servitude) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierCreationImmeuble(RegDate dateCreation, ImmeubleRF immeuble) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierRadiationImmeuble(RegDate dateRadiation, ImmeubleRF immeuble) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierReactivationImmeuble(RegDate dateReactivation, ImmeubleRF immeuble) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationEgridImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationSituationImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationSurfaceTotaleImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationSurfaceAuSolImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationQuotePartImmeuble(RegDate dateModification, ImmeubleRF immeuble) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierDebutEstimationFiscalImmeuble(RegDate dateDebutMetier, EstimationRF estimation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierChangementEnRevisionEstimationFiscalImmeuble(RegDate dateChangement, EstimationRF estimation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierFinEstimationFiscalImmeuble(RegDate dateFinMetier, EstimationRF estimation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierAnnulationEstimationFiscalImmeuble(RegDate dateAnnulation, EstimationRF estimation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierDebutImplantationBatiment(RegDate dateDebut, ImplantationRF implantation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierFinImplantationBatiment(RegDate dateFin, ImplantationRF implantation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationPrincipalCommunaute(RegDate dateDebut, CommunauteRF communaute) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierModificationHeritageCommunaute(RegDate dateDebut, CommunauteRF communaute) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierDebutRapprochementTiersRF(RegDate dateDebut, RapprochementRF rapprochement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierFinRapprochementTiersRF(RegDate dateFin, RapprochementRF rapprochement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void publierAnnulationRapprochementTiersRF(RegDate dateAnnulation, RapprochementRF rapprochement) {
		throw new UnsupportedOperationException();
	}

	public boolean isCalledOnce() {
		return calledOnce;
	}

	public Entreprise getEntreprise() {
		return entreprise;
	}

	public EvenementFiscalInformationComplementaire.TypeInformationComplementaire getType() {
		return type;
	}

	public RegDate getDateEvenement() {
		return dateEvenement;
	}
}
