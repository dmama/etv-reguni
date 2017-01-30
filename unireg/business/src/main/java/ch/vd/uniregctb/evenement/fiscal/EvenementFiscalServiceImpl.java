package ch.vd.uniregctb.evenement.fiscal;

import java.util.Collection;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
import ch.vd.uniregctb.fourreNeutre.FourreNeutre;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;

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
		saveAndPublish(new EvenementFiscalImpressionFourreNeutre(fourreNeutre.getTiers(),fourreNeutre.getPeriodeFIscale(), dateTraitement));

	}
}
