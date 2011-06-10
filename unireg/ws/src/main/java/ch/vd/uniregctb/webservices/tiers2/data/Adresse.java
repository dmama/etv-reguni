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

	/** La date de début de validité de l'adresse. Dans certains cas cette information n'est pas disponible et la date n'est pas renseignée. */
	@XmlElement(required = false)
	public Date dateDebut;

	/** La date de fin de validité de l'adresse. Si l'adresse est toujours active, cette date n'est pas renseignée. */
	@XmlElement(required = false)
	public Date dateFin;

	/** Titre de l'adresse. Exemple : "chez" ou "c/o" */
	@XmlElement(required = false)
	public String titre;

	/** Numéro de l'appartement */
	@XmlElement(required = false)
	public String numeroAppartement;

	/** Rue */
	@XmlElement(required = false)
	public String rue;

	/** Numéro du bâtiment dans la rue */
	@XmlElement(required = false)
	public String numeroRue;

	/** Case postale + numéro */
	@XmlElement(required = false)
	public String casePostale;

	/** La localité. Exemple : "Lausanne" */
	@XmlElement(required = true)
	public String localite;

	/** Le numéro postal de la localité. Exemple : "1001" pour "1001 Lausanne" */
	@XmlElement(required = true)
	public String numeroPostal;

	/** Le pays en toutes lettres (non-renseigné sur les adresses suisse). Exemples : "France", "Albanie", "Japon". */
	@XmlElement(required = false)
	public String pays;

	/** [Technique] numéro d'ordre postal de l'adresse */
	@XmlElement(required = true)
	public int noOrdrePostal;

	/** [Technique] numéro technique de la rue */
	@XmlElement(required = false)
	public Integer noRue;

	/** [Technique] numéro OFS du pays */
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
		this.casePostale = adresse.getCasePostale();
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
