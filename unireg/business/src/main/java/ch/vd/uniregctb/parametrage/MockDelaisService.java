package ch.vd.uniregctb.parametrage;

import ch.vd.registre.base.date.RegDate;

public class MockDelaisService implements DelaisService {

	@Override
	public RegDate getDateFinDelaiAttenteDeclarationImpotPersonneDecedee(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiRetourDeclarationImpotEmiseManuellement(RegDate dateDebut) {
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
	public RegDate getDateFinDelaiEcheanceSommationDeclarationImpot(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEcheanceSommationListeRecapitualtive(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEnvoiSommationDeclarationImpot(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	@Override
	public RegDate getDateFinDelaiEnvoiSommationListeRecapitulative(RegDate dateDebut) {
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
