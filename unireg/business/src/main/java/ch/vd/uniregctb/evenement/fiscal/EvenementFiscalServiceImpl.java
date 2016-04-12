package ch.vd.uniregctb.evenement.fiscal;

import java.util.Collection;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;
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

	private void publierEvenementFiscalDeclaration(RegDate date, Declaration declaration, EvenementFiscalDeclaration.TypeAction type) {
		saveAndPublish(new EvenementFiscalDeclaration(date, declaration, type));
	}

	private void publierEvenementFiscalEmissionDeclaration(RegDate date, Declaration declaration) {
		publierEvenementFiscalDeclaration(date, declaration, EvenementFiscalDeclaration.TypeAction.EMISSION);
	}

	private void publierEvenementFiscalQuittancementDeclaration(RegDate date, Declaration declaration) {
		publierEvenementFiscalDeclaration(date, declaration, EvenementFiscalDeclaration.TypeAction.QUITTANCEMENT);
	}

	private void publierEvenementFiscalSommationDeclaration(RegDate date, Declaration declaration) {
		publierEvenementFiscalDeclaration(date, declaration, EvenementFiscalDeclaration.TypeAction.SOMMATION);
	}

	private void publierEvenementFiscalEcheanceDeclaration(RegDate date, Declaration declaration) {
		publierEvenementFiscalDeclaration(date, declaration, EvenementFiscalDeclaration.TypeAction.ECHEANCE);
	}

	private void publierEvenementFiscalAnnulationDeclaration(RegDate date, Declaration declaration) {
		publierEvenementFiscalDeclaration(date, declaration, EvenementFiscalDeclaration.TypeAction.ANNULATION);
	}

	@Override
	public void publierEvenementFiscalEmissionListeRecapitulative(DeclarationImpotSource lr, RegDate dateEmission) {
		publierEvenementFiscalEmissionDeclaration(dateEmission, lr);
	}

	@Override
	public void publierEvenementFiscalQuittancementListeRecapitulative(DeclarationImpotSource lr, RegDate dateQuittancement) {
		publierEvenementFiscalQuittancementDeclaration(dateQuittancement, lr);
	}

	@Override
	public void publierEvenementFiscalAnnulationListeRecapitulative(DeclarationImpotSource lr) {
		publierEvenementFiscalAnnulationDeclaration(RegDateHelper.get(lr.getAnnulationDate()), lr);
	}

	@Override
	public void publierEvenementFiscalSommationListeRecapitulative(DeclarationImpotSource lr, RegDate dateSommation) {
		publierEvenementFiscalSommationDeclaration(dateSommation, lr);
	}

	@Override
	public void publierEvenementFiscalEcheanceListeRecapitulative(DeclarationImpotSource lr, RegDate dateEcheance) {
		publierEvenementFiscalEcheanceDeclaration(dateEcheance, lr);
	}

	@Override
	public void publierEvenementFiscalEmissionDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEmission) {
		publierEvenementFiscalEmissionDeclaration(dateEmission, di);
	}

	@Override
	public void publierEvenementFiscalQuittancementDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateQuittance) {
		publierEvenementFiscalQuittancementDeclaration(dateQuittance, di);
	}

	@Override
	public void publierEvenementFiscalSommationDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateSommation) {
		publierEvenementFiscalSommationDeclaration(dateSommation, di);
	}

	@Override
	public void publierEvenementFiscalEcheanceDeclarationImpot(DeclarationImpotOrdinaire di, RegDate dateEcheance) {
		publierEvenementFiscalEcheanceDeclaration(dateEcheance, di);
	}

	@Override
	public void publierEvenementFiscalAnnulationDeclarationImpot(DeclarationImpotOrdinaire di) {
		publierEvenementFiscalAnnulationDeclaration(RegDateHelper.get(di.getAnnulationDate()), di);
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
}
