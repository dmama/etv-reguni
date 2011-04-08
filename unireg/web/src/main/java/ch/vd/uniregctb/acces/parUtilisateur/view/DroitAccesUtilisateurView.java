package ch.vd.uniregctb.acces.parUtilisateur.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class DroitAccesUtilisateurView implements Annulable {

	private final Long id;
	private final boolean annule;
	private final TypeDroitAcces type;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Niveau niveau;
	private final Long numeroCTB;
	private final String prenomNom;
	private final String localite;
	private final RegDate dateNaissance;

	public DroitAccesUtilisateurView(DroitAcces droit, TiersService tiersService, AdresseService adresseService) throws AdresseException {
		this.id = droit.getId();
		this.annule = droit.isAnnule();
		this.dateDebut = droit.getDateDebut();
		this.dateFin = droit.getDateFin();
		this.type = droit.getType();
		this.niveau = droit.getNiveau();

		final PersonnePhysique pp = droit.getTiers();
		this.numeroCTB = pp.getNumero();

		final AdresseEnvoiDetaillee adresse = adresseService.getAdresseEnvoi(pp, null, TypeAdresseFiscale.COURRIER, false);
		if (adresse != null) {
			this.localite = adresse.getNpaEtLocalite();
		}
		else {
			this.localite = null;
		}

		this.prenomNom = tiersService.getNomPrenom(pp);
		this.dateNaissance = tiersService.getDateNaissance(pp);
	}

	public Long getId() {
		return id;
	}

	public boolean isAnnule() {
		return annule;
	}

	public TypeDroitAcces getType() {
		return type;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public Niveau getNiveau() {
		return niveau;
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

	public boolean isLectureSeule() {
		return niveau == Niveau.LECTURE;
	}
}
