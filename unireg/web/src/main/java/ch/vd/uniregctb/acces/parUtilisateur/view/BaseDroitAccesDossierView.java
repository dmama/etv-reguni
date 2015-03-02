package ch.vd.uniregctb.acces.parUtilisateur.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

public class BaseDroitAccesDossierView {

	private final Long numeroCTB;
	private final String prenomNom;
	private final String localite;
	private final RegDate dateNaissance;

	public BaseDroitAccesDossierView(PersonnePhysique pp, TiersService tiersService, AdresseService adresseService) throws AdresseException {
		this.numeroCTB = pp.getNumero();

		final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(pp, null, TypeAdresseFiscale.COURRIER, false);
		if (adresse != null && adresse.getNpaEtLocalite() != null) {
			this.localite = adresse.getNpaEtLocalite().toString();
		}
		else {
			this.localite = null;
		}

		this.prenomNom = tiersService.getNomPrenom(pp);
		this.dateNaissance = tiersService.getDateNaissance(pp);
	}

	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public String getPrenomNom() {
		return prenomNom;
	}

	public String getLocalite() {
		return localite;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}
}
