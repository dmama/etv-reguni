package ch.vd.uniregctb.parametrage;

import ch.vd.registre.base.date.RegDate;

public class MockDelaisService implements DelaisService {

	public RegDate getDateFinDelaiAttenteDeclarationImpotPersonneDecedee(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiRetourDeclarationImpotEmiseManuellement(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiCadevImpressionDeclarationImpot(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiCadevImpressionListesRecapitulatives(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiEcheanceSommationDeclarationImpot(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiEcheanceSommationListeRecapitualtive(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiEnvoiSommationDeclarationImpot(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiEnvoiSommationListeRecapitulative(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiRetentionRapportTravailInactif(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiRetourListeRecapitulative(RegDate dateEmissionLr, RegDate dateFinPeriodeLr) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getDateFinDelaiRetourSommationListeRecapitulative(RegDate dateDebut) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getFinDelai(RegDate dateDebut, int delaiEnJours) {
		throw new IllegalArgumentException("no meant to be called");
	}

	public RegDate getFinDelai(RegDate dateDebut, int delaiEnJours, boolean joursOuvres, boolean repousseAuProchainJourOuvre) {
		throw new IllegalArgumentException("no meant to be called");
	}
}
