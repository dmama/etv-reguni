package ch.vd.uniregctb.interfaces.model.wrapper.apireg;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import ch.vd.apireg.datamodel.AdrIndividu;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class AdresseWrapper implements Adresse {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final String numeroPostal;
	private final String numeroPostalComplementaire;
	private final String localite;
	private final String numero;
	private final String numeroAppartement;
	private final Integer numeroRue;
	private final Integer numeroOrdrePostal;
	private final Integer noOfsPays;
	private final String rue;
	private final String titre;
	private final EnumTypeAdresse type;

	private final static Date nullHostDate;

	static {
		// voir la class ch.vd.utils.database.AbstractDbObjet du projet host-interfaces.
		Calendar c = new GregorianCalendar(1, Calendar.JANUARY, 1, 1, 0, 0);
		c.set(Calendar.MILLISECOND, 0);
		nullHostDate = c.getTime();
	}

	public static AdresseWrapper get(AdrIndividu target, ServiceInfrastructureService infraService) {
		if (target == null) {
			return null;
		}
		Date dateFinValidite = target.getDateFinValidite();
		if (dateFinValidite != null && DateHelper.isSameDay(nullHostDate, dateFinValidite)) {
			// Update (msi) 02.07.2009 : Apireg n'interprète pas les dates retournées par la base de données, or le host utilise le 1er
			// janvier de l'an 1 pour signaler qu'une date est nulle. Cette interprétation est faite de manière transparente par
			// host-interface, mais pas par Apireg. On le fait donc explicitement ici.
			dateFinValidite = null;
		}
		// UNIREG-474 : si une adresse civile possède une date de fin de validité et que celle-ci est avant le 1er janvier 1900, il s'agit
		// en fait d'une adresse annulée et il ne faut pas en tenir compte
		if (dateFinValidite != null && DateHelper.isNullDate(dateFinValidite)) {
			return null;
		}
		return new AdresseWrapper(target, infraService);
	}

	private AdresseWrapper(AdrIndividu target, ServiceInfrastructureService infraService) {
		this.dateDebut = RegDate.get(target.getDateValidite());
		this.dateFin = RegDate.get(target.getDateFinValidite());
		final Localite loc = getLocalite(target, infraService);
		if (loc == null) {
			this.numeroPostal = null;
			this.numeroPostalComplementaire = null;
			this.localite = target.getLieu();
		}
		else {
			this.numeroPostal = loc.getNPA().toString();
			final Integer complement = loc.getComplementNPA();
			this.numeroPostalComplementaire = (complement == null ? null : complement.toString());
			this.localite = loc.getNomAbregeMinuscule();
		}
		this.numero = target.getNoPolice();
		this.numeroAppartement = target.getNumMenage();
		this.numeroRue = target.getRueOFS(); // Note: apireg retourne bien le numéro technique de la rue, et non pas le numéro Ofs !
		this.numeroOrdrePostal = target.getNoOrdrePostal();
		this.noOfsPays = (target.getPaysOFS() == null ? ServiceInfrastructureService.noOfsSuisse : target.getPaysOFS());
		this.rue = target.getRue();
		this.titre = target.getChez();
		this.type = extractType(target);
	}

	public String getCasePostale() {
		return null;
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public RegDate getDateFinValidite() {
		return dateFin;
	}

	public String getLocalite() {
		return localite;
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
		return numeroOrdrePostal == null ? 0 : numeroOrdrePostal.intValue();
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

	public EnumTypeAdresse getTypeAdresse() {
		return type;
	}

	private EnumTypeAdresse extractType(AdrIndividu target) {
		switch (target.getTypeAdresse()) {
		case AdrIndividu.TYPE_COURRIER:
			return EnumTypeAdresse.COURRIER;
		case AdrIndividu.TYPE_PRINCIPALE:
			return EnumTypeAdresse.PRINCIPALE;
		case AdrIndividu.TYPE_SECONDAIRE:
			return EnumTypeAdresse.SECONDAIRE;
		case AdrIndividu.TYPE_TUTELLE:
			return EnumTypeAdresse.TUTELLE;
		default:
			throw new IllegalArgumentException("Type d'adresse inconnu = [" + target.getTypeAdresse() + "]");
		}
	}

	private static Localite getLocalite(AdrIndividu target, ServiceInfrastructureService infraService) {

		Localite loc = null;

		final Integer onrp = target.getNoOrdrePostal();
		if (onrp != null && onrp.intValue() > 0) {
			try {
				loc = infraService.getLocaliteByONRP(onrp);
			}
			catch (InfrastructureException e) {
				throw new IllegalArgumentException("Localité postale inconnue = [" + onrp + "]");
			}
		}

		return loc;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
