package ch.vd.unireg.acces.parUtilisateur.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeDroitAcces;

public class DroitAccesUtilisateurView extends BaseDroitAccesDossierView implements Annulable {

	private final Long id;
	private final boolean annule;
	private final TypeDroitAcces type;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final Niveau niveau;

	public DroitAccesUtilisateurView(DroitAcces droit, TiersService tiersService, AdresseService adresseService) throws AdresseException {
		super(droit.getTiers(), tiersService, adresseService);
		this.id = droit.getId();
		this.annule = droit.isAnnule();
		this.dateDebut = droit.getDateDebut();
		this.dateFin = droit.getDateFin();
		this.type = droit.getType();
		this.niveau = droit.getNiveau();
	}

	public DroitAccesUtilisateurView(DroitAcces droit, Exception erreur) {
		super(droit.getTiers(), erreur);
		this.id = droit.getId();
		this.annule = droit.isAnnule();
		this.dateDebut = droit.getDateDebut();
		this.dateFin = droit.getDateFin();
		this.type = droit.getType();
		this.niveau = droit.getNiveau();
	}

	public Long getId() {
		return id;
	}

	@Override
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

	public boolean isLectureSeule() {
		return niveau == Niveau.LECTURE;
	}
}
