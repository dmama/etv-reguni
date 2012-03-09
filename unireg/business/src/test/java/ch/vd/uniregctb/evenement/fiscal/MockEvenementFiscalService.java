package ch.vd.uniregctb.evenement.fiscal;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;

public class MockEvenementFiscalService implements EvenementFiscalService {

	@Override
	public Collection<EvenementFiscal> getEvenementsFiscaux(Tiers tiers) {
		return null;
	}

	@Override
	public void publierEvenementFiscal(EvenementFiscal evenementFiscal) {
	}

	@Override
	public void publierEvenementFiscalOuvertureFor(Tiers tiers, RegDate dateEvenement, MotifFor motifFor, Long id) {
	}

	@Override
	public void publierEvenementFiscalFermetureFor(Tiers tiers, RegDate dateEvenement, MotifFor motifFor, Long id) {
	}

	@Override
	public void publierEvenementFiscalAnnulationFor(ForFiscal forFiscal, RegDate dateAnnulation) {
	}

	@Override
	public void publierEvenementFiscalChangementModeImposition(Contribuable contribuable, RegDate dateEvenement, ModeImposition modeImposition, Long id) {
	}

	@Override
	public void publierEvenementFiscalFinAutoriteParentale(PersonnePhysique contribuableEnfant, Contribuable contribuableParent, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalNaissance(PersonnePhysique contribuableEnfant, Contribuable contribuableParent, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalChangementSituation(Contribuable contribuable, RegDate dateEvenement, Long id) {
	}

	@Override
	public void publierEvenementFiscalOuverturePeriodeDecompteLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalRetourLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalSommationLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalLRManquante(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalAnnulationLR(DebiteurPrestationImposable debiteur, DeclarationImpotSource lr, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalEnvoiDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalRetourDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalSommationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalEcheanceDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalTaxationOffice(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
	}

	@Override
	public void publierEvenementFiscalAnnulationDI(Contribuable contribuable, DeclarationImpotOrdinaire di, RegDate dateEvenement) {
	}
}
