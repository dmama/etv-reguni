package ch.vd.unireg.acces.parUtilisateur.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

public class BaseDroitAccesDossierView {

	private final Long numeroCTB;
	private final String prenomNom;
	private final String localite;
	private final RegDate dateNaissance;
	private final String erreur;

	public BaseDroitAccesDossierView(Contribuable ctb, TiersService tiersService, AdresseService adresseService) throws AdresseException {
		this.numeroCTB = ctb.getNumero();

		final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(ctb, null, TypeAdresseFiscale.COURRIER, false);
		if (adresse != null && adresse.getNpaEtLocalite() != null) {
			this.localite = adresse.getNpaEtLocalite().toString();
		}
		else {
			this.localite = null;
		}

		this.prenomNom = tiersService.getNomRaisonSociale(ctb);
		if (ctb instanceof PersonnePhysique) {
			this.dateNaissance = tiersService.getDateNaissance((PersonnePhysique) ctb);
		}
		else if (ctb instanceof Entreprise) {
			this.dateNaissance = tiersService.getDateCreation((Entreprise) ctb);
		}
		else {
			this.dateNaissance = null;
		}

		this.erreur = null;
	}

	public BaseDroitAccesDossierView(Contribuable ctb, Exception e) {
		this.numeroCTB = ctb.getNumero();
		this.prenomNom = null;
		this.localite = null;
		this.dateNaissance = null;
		this.erreur = e.getMessage();
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

	public String getErreur() {
		return erreur;
	}
}
