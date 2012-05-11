package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.CasePostale;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseImpl implements Adresse, Serializable {

	private static final long serialVersionUID = -8183104360877534906L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final CasePostale casePostale;
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
	private final Integer noOfsCommuneAdresse;
	private final Integer egid;
	private final Integer ewid;

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
		this.casePostale = CasePostale.parse(target.getCasePostale());
		this.localiteAbregeMinuscule = target.getLocaliteAbregeMinuscule();
		this.numero = target.getNumero();
		this.numeroAppartement = target.getNumeroAppartement();
		this.numeroRue = initNoRue(target.getNumeroTechniqueRue());
		this.numeroOrdrePostal = target.getNumeroOrdrePostal();
		this.numeroPostal = target.getNumeroPostal();
		this.numeroPostalComplementaire = target.getNumeroPostalComplementaire();
		this.noOfsPays =
				(target.getPays() == null ? ServiceInfrastructureRaw.noOfsSuisse : target.getPays().getNoOFS()); // le pays n'est pas toujours renseignée dans le base lorsqu'il s'agit de la Suisse
		this.rue = target.getRue();
		this.titre = target.getTitre();
		this.typeAdresse = initTypeAdresse(target.getTypeAdresse());

		final CommuneImpl commune = CommuneImpl.get(target.getCommuneAdresse());
		this.noOfsCommuneAdresse = commune == null ? null : commune.getNoOFSEtendu();

		if (this.typeAdresse == TypeAdresseCivil.COURRIER || this.typeAdresse == TypeAdresseCivil.TUTEUR) {
			// les adresses courrier (et tuteur) ne doivent pas posséder d'egid/ewid (= ça n'a pas de sens).
			this.egid = null;
			this.ewid = null;
		}
		else {
			// [SIFISC-3460] la valeur minimale admise pour les EGID et les EWID est 1 (on teste aussi les valeurs maximales, tant qu'on y est)
			this.egid = string2int(target.getEgid(), 1, 999999999);
			this.ewid = string2int(target.getEwid(), 1, 999);
		}
	}

	private static TypeAdresseCivil initTypeAdresse(EnumTypeAdresse type) {
		if (type == null) {
			return null;
		}

		if (type == EnumTypeAdresse.SECONDAIRE) {
			return TypeAdresseCivil.SECONDAIRE;
		}
		else if (type == EnumTypeAdresse.PRINCIPALE) {
			return TypeAdresseCivil.PRINCIPALE;
		}
		else if (type == EnumTypeAdresse.COURRIER) {
			return TypeAdresseCivil.COURRIER;
		}
		else if (type == EnumTypeAdresse.TUTELLE) {
			return TypeAdresseCivil.TUTEUR;
		}
		else {
			throw new IllegalArgumentException("Type d'adresse civile inconnue = [" + type.getName() + ']');
		}
	}

	private static Integer initNoRue(Integer numeroTechniqueRue) {
		if (numeroTechniqueRue == null || numeroTechniqueRue == 0) {
			return null;
		}
		else {
			return numeroTechniqueRue;
		}
	}

	private static Integer string2int(String string, int minValue, int maxValue) {
		string = StringUtils.trimToNull(string);
		if (string == null) {
			return null;
		}
		else {
			final int i = Integer.parseInt(string);
			if (i < minValue || i > maxValue) {
				return null;
			}
			else {
				return i;
			}
		}
	}

	@Override
	public CasePostale getCasePostale() {
		return casePostale;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public String getLocalite() {
		return localiteAbregeMinuscule;
	}

	@Override
	public String getNumero() {
		return numero;
	}

	@Override
	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	@Override
	public Integer getNumeroRue() {
		return numeroRue;
	}

	@Override
	public int getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	@Override
	public String getNumeroPostal() {
		return numeroPostal;
	}

	@Override
	public String getNumeroPostalComplementaire() {
		return numeroPostalComplementaire;
	}

	@Override
	public Integer getNoOfsPays() {
		return noOfsPays;
	}

	@Override
	public String getRue() {
		return rue;
	}

	@Override
	public String getTitre() {
		return titre;
	}

	@Override
	public TypeAdresseCivil getTypeAdresse() {
		return typeAdresse;
	}

	@Nullable
	@Override
	public Integer getNoOfsCommuneAdresse() {
		return noOfsCommuneAdresse;
	}

	@Override
	public Integer getEgid() {
		return egid;
	}

	@Override
	public Integer getEwid() {
		return ewid;
	}

	@Override
	public Localisation getLocalisationPrecedente() {
		// cette information n'est pas disponible dans host-interfaces
		return null;
	}

	@Override
	public Localisation getLocalisationSuivante() {
		// cette information n'est pas disponible dans host-interfaces
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
