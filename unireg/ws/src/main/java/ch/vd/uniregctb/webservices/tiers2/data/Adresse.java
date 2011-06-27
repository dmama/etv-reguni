package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

/**
 * Contient les données métier caractérisant une adresse fiscale d'un tiers.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>addressType</i> (xml) / <i>Address</i> (client java)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Adresse", propOrder = {
		"dateDebut", "dateFin", "titre", "numeroAppartement", "rue", "numeroRue", "casePostale", "localite", "numeroPostal", "pays",
		"noOrdrePostal", "noRue", "noPays"
})
public class Adresse implements Range {

	private static final Logger LOGGER = Logger.getLogger(Adresse.class);

	/**
	 * La date de début de validité de l'adresse. Dans certains cas cette information n'est pas disponible et la date n'est pas renseignée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateFrom</i>
	 */
	@XmlElement(required = false)
	public Date dateDebut;

	/**
	 * La date de fin de validité de l'adresse. Si l'adresse est toujours active, cette date n'est pas renseignée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateTo</i>
	 */
	@XmlElement(required = false)
	public Date dateFin;

	/**
	 * Titre de l'adresse. Exemple : "chez" ou "c/o"
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>title</i>
	 */
	@XmlElement(required = false)
	public String titre;

	/**
	 * Numéro de l'appartement
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dwellingNumber</i>
	 */
	@XmlElement(required = false)
	public String numeroAppartement;

	/**
	 * Rue
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>street</i>
	 */
	@XmlElement(required = false)
	public String rue;

	/**
	 * Numéro du bâtiment dans la rue
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>houseNumber</i>
	 */
	@XmlElement(required = false)
	public String numeroRue;

	/**
	 * Case postale + numéro
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>postOfficeBox</i>
	 */
	@XmlElement(required = false)
	public String casePostale;

	/**
	 * La localité. Exemple : "Lausanne"
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>town</i>
	 */
	@XmlElement(required = true)
	public String localite;

	/**
	 * Le numéro postal de la localité. Exemple : "1001" pour "1001 Lausanne"
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>zipCode</i>
	 */
	@XmlElement(required = true)
	public String numeroPostal;

	/**
	 * Le pays en toutes lettres (non-renseigné sur les adresses suisse). Exemples : "France", "Albanie", "Japon".
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>country</i>
	 */
	@XmlElement(required = false)
	public String pays;

	/**
	 * [Technique] numéro d'ordre postal de l'adresse
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>swissZipCodeId</i>
	 */
	@XmlElement(required = true)
	public int noOrdrePostal;

	/**
	 * [Technique] numéro technique de la rue
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>streetId</i>
	 */
	@XmlElement(required = false)
	public Integer noRue;

	/**
	 * [Technique] numéro OFS du pays
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>countryId</i>
	 */
	@XmlElement(required = false)
	public Integer noPays;

	public Adresse() {
	}

	public Adresse(ch.vd.uniregctb.adresse.AdresseGenerique adresse, ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService serviceInfra) throws BusinessException {
		this.dateDebut = DataHelper.coreToWeb(adresse.getDateDebut());
		this.dateFin = DataHelper.coreToWeb(adresse.getDateFin());
		this.titre = adresse.getComplement();
		this.numeroAppartement = adresse.getNumeroAppartement();
		this.rue = adresse.getRue();
		this.numeroRue = adresse.getNumero();
		this.casePostale = adresse.getCasePostale() == null ? null : adresse.getCasePostale().toString();
		this.localite = adresse.getLocalite();
		this.numeroPostal = adresse.getNumeroPostal();

		final Integer noOfsPays = adresse.getNoOfsPays();
		if (noOfsPays != null) {
			ch.vd.uniregctb.interfaces.model.Pays p;
			try {
				p = serviceInfra.getPays(noOfsPays);
			}
			catch (ServiceInfrastructureException e) {
				LOGGER.error(e, e);
				throw new BusinessException(e);
			}
			if (p != null && !p.isSuisse()) {
				this.pays = p.getNomMinuscule();
			}
		}

		this.noOrdrePostal = adresse.getNumeroOrdrePostal();
		this.noRue = adresse.getNumeroRue();
		this.noPays = noOfsPays;
	}

	@Override
	public Date getDateDebut() {
		return dateDebut;
	}

	@Override
	public Date getDateFin() {
		return dateFin;
	}

	@Override
	public void setDateDebut(Date v) {
		dateDebut = v;
	}

	@Override
	public void setDateFin(Date v) {
		dateFin = v;
	}
}
