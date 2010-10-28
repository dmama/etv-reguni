package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;
import java.util.Date;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseImpl implements Adresse, Serializable {

	private static final long serialVersionUID = -8140768971615719637L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final String casePostale;
	private final String localiteAbregeMinuscule;
	private final String numero;
	private final String numeroAppartement;
	private final Integer numeroRue;
	private final int numeroOrdrePostal;
	private final String numeroPostal;
	private final String numeroPostalComplementaire;
	private final int noOfsPays;
	private final String rue;
	private final String titre;
	private final TypeAdresseCivil typeAdresse;
	private final CommuneSimple communeAdresse;

	public static AdresseImpl get(ch.vd.common.model.Adresse target) {
		if (target == null) {
			return null;
		}
		// UNIREG-474 : si une adresse civile possède une date de fin de validité et que celle-ci est avant le 1er janvier 1900, il s'agit
		// en fait d'une adresse annulée et il ne faut pas en tenir compte
		final Date dateFinValidite = target.getDateFinValidite();
		if (dateFinValidite != null && DateHelper.isNullDate(dateFinValidite)) {
			return null;
		}
		return new AdresseImpl(target);
	}

	private AdresseImpl(ch.vd.common.model.Adresse target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		this.casePostale = target.getCasePostale();
		this.localiteAbregeMinuscule = target.getLocaliteAbregeMinuscule();
		this.numero = target.getNumero();
		this.numeroAppartement = target.getNumeroAppartement();
		this.numeroRue = target.getNumeroTechniqueRue();
		this.numeroOrdrePostal = target.getNumeroOrdrePostal();
		this.numeroPostal = target.getNumeroPostal();
		this.numeroPostalComplementaire = target.getNumeroPostalComplementaire();
		this.noOfsPays =
				(target.getPays() == null ? ServiceInfrastructureService.noOfsSuisse : target.getPays().getNoOFS()); // le pays n'est pas toujours renseignée dans le base lorsqu'il s'agit de la Suisse
		this.rue = target.getRue();
		this.titre = target.getTitre();
		this.typeAdresse = TypeAdresseCivil.get(target.getTypeAdresse());
		this.communeAdresse = CommuneSimpleImpl.get(target.getCommuneAdresse());
	}

	public String getCasePostale() {
		return casePostale;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public String getLocalite() {
		return localiteAbregeMinuscule;
	}

	public String getNumero() {
		return numero;
	}

	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public Integer getNumeroRue() {
		return numeroRue;
	}

	public int getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	public String getNumeroPostal() {
		return numeroPostal;
	}

	public String getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	public Integer getNoOfsPays() {
		return noOfsPays;
	}

	public String getRue() {
		return rue;
	}

	public String getTitre() {
		return titre;
	}

	public TypeAdresseCivil getTypeAdresse() {
		return typeAdresse;
	}

	public CommuneSimple getCommuneAdresse() {
		return communeAdresse;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
