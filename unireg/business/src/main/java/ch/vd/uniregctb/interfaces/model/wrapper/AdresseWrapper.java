package ch.vd.uniregctb.interfaces.model.wrapper;

import java.util.Date;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.infrastructure.model.Pays;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class AdresseWrapper implements Adresse {

	private final ch.vd.common.model.Adresse target;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public static AdresseWrapper get(ch.vd.common.model.Adresse target) {
		if (target == null) {
			return null;
		}
		// UNIREG-474 : si une adresse civile possède une date de fin de validité et que celle-ci est avant le 1er janvier 1900, il s'agit
		// en fait d'une adresse annulée et il ne faut pas en tenir compte
		final Date dateFinValidite = target.getDateFinValidite();
		if (dateFinValidite != null && DateHelper.isNullDate(dateFinValidite)) {
			return null;
		}
		return new AdresseWrapper(target);
	}

	private AdresseWrapper(ch.vd.common.model.Adresse target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
	}

	public String getCasePostale() {
		return target.getCasePostale();
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getLocalite() {
		return target.getLocaliteAbregeMinuscule();
	}

	public String getNumero() {
		return target.getNumero();
	}

	public String getNumeroAppartement() {
		return target.getNumeroAppartement();
	}

	@SuppressWarnings("deprecation")
	public Integer getNumeroRue() {
		// Note: host-interface retourne bien le numéro technique de la rue, et non pas le numéro Ofs !
		return target.getNumeroOfsRue();
	}

	public int getNumeroOrdrePostal() {
		return target.getNumeroOrdrePostal();
	}

	public String getNumeroPostal() {
		return target.getNumeroPostal();
	}

	public String getNumeroPostalComplementaire() {
		return target.getNumeroPostalComplementaire();
	}

	public Integer getNoOfsPays() {
		final Pays pays = target.getPays();
		if (pays == null) {
			return ServiceInfrastructureService.noOfsSuisse; // le pays n'est pas toujours renseignée dans le base lorsqu'il s'agit de la Suisse
		}
		else {
			return pays.getNoOFS();
		}
	}

	public String getRue() {
		return target.getRue();
	}

	public String getTitre() {
		return target.getTitre();
	}

	public EnumTypeAdresse getTypeAdresse() {
		return target.getTypeAdresse();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
