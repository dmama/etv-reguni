package ch.vd.unireg.parametrage;

import ch.vd.registre.base.date.RegDate;

public class MockDelaisService implements DelaisService {

	@Override
	public RegDate getDateFinDelaiAttenteDeclarationImpotPersonneDecedee(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiRetourDeclarationImpotPPEmiseManuellement(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiRetourDeclarationImpotPMEmiseManuellement(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiRetourQuestionnaireSNCEmisManuellement(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionDeclarationImpot(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionListesRecapitulatives(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionQuestionnaireSNC(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionLettreBienvenue(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiCadevImpressionDemandeDegrevementICI(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEcheanceSommationDeclarationImpotPP(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEcheanceSommationDeclarationImpotPM(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEcheanceSommationListeRecapitualtive(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEnvoiSommationDeclarationImpotPP(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEnvoiSommationDeclarationImpotPM(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEnvoiSommationListeRecapitulative(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEnvoiRappelQuestionnaireSNC(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiRetentionRapportTravailInactif(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiRetourListeRecapitulative(RegDate dateEmissionLr, RegDate dateFinPeriodeLr) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiRetourSommationListeRecapitulative(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getFinDelai(RegDate dateDebut, int delaiEnJours) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getFinDelai(RegDate dateDebut, int delaiEnJours, boolean joursOuvres, boolean repousseAuProchainJourOuvre) {
		throw new IllegalArgumentException("no meant to be called");
	}
}
