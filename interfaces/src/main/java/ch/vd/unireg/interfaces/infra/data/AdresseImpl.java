package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.xml.XmlUtils;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClient;
import ch.vd.unireg.type.TypeAdresseCivil;

public class AdresseImpl implements Adresse, Serializable {

	private static final long serialVersionUID = 5993820226836872810L;

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final CasePostale casePostale;
	private final String localiteAbregeMinuscule;
	private final String numero;
	private final String numeroAppartement;
	private final Integer numeroRue;
	private final Integer numeroOrdrePostal;
	private final String numeroPostal;
	private final String numeroPostalComplementaire;
	private final int noOfsPays;
	private final String rue;
	private final String titre;
	private final TypeAdresseCivil typeAdresse;
	private final Integer noOfsCommuneAdresse;
	private final Integer egid;
	private final Integer ewid;

	public static Adresse get(ch.vd.common.model.rest.AdresseImpl target, ServiceInfrastructureClient client) {
		if (target == null) {
			return null;
		}
		// UNIREG-474 : si une adresse civile possède une date de fin de validité et que celle-ci est avant le 1er janvier 1900, il s'agit
		// en fait d'une adresse annulée et il ne faut pas en tenir compte
		final Date dateFinValidite = XmlUtils.cal2date(target.getDateFinValidite());
		if (dateFinValidite != null && DateHelper.isNullDate(dateFinValidite)) {
			return null;
		}
		return new AdresseImpl(target, client);
	}

	public AdresseImpl(ch.vd.common.model.rest.AdresseImpl target, ServiceInfrastructureClient client) {
		this.dateDebut = XmlUtils.cal2regdate(target.getDateDebutValidite());
		this.dateFin = XmlUtils.cal2regdate(target.getDateFinValidite());
		this.casePostale = CasePostale.parse(target.getCasePostale());
		this.localiteAbregeMinuscule = target.getLocaliteAbregeMinuscule();
		this.numero = null;         // numéro de police TOUJOURS déjà concaténé dans la rue
		this.numeroAppartement = target.getNumeroAppartement();
		this.numeroPostal = target.getNumeroPostal();
		this.numeroPostalComplementaire = target.getNumeroPostalComplementaire();
		this.noOfsPays = (target.getPays() == null ? ServiceInfrastructureRaw.noOfsSuisse : target.getPays().getNoOFS()); // le pays n'est pas toujours renseignée dans le base lorsqu'il s'agit de la Suisse
		this.rue = target.getRue(); // nom de la rue TOUJOURS résolu (+ concaténation éventuelle du numéro de police)
		this.numeroOrdrePostal = target.getNumeroOrdrePostal() == 0 ? null : AdresseHelper.getNoOrdrePosteOfficiel(target.getNumeroOrdrePostal());
		this.numeroRue = null;      // on ne veut plus de ces numéros qui viennent du host !!
		this.titre = target.getTitre();
		this.typeAdresse = initTypeAdresse(target.getTypeAdress());

		final CommuneImpl commune = CommuneImpl.get(target.getCommuneAdresse());
		this.noOfsCommuneAdresse = commune == null ? null : commune.getNoOFS();

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

	private TypeAdresseCivil initTypeAdresse(String type) {
		if (type == null) {
			return null;
		}

		if ("SECONDAIRE".equals(type)) {
			return TypeAdresseCivil.SECONDAIRE;
		}
		else if ("PRINCIPALE".equals(type)) {
			return TypeAdresseCivil.PRINCIPALE;
		}
		else if ("COURRIER".equals(type)) {
			return TypeAdresseCivil.COURRIER;
		}
		else if ("TUTELLE".equals(type)) {
			return TypeAdresseCivil.TUTEUR;
		}
		else {
			throw new IllegalArgumentException("Type d'adresse civile inconnue = [" + type + ']');
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
	public Integer getNumeroOrdrePostal() {
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
}
