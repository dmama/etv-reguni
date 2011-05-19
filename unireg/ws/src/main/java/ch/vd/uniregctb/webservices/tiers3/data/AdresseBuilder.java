package ch.vd.uniregctb.webservices.tiers3.data;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.webservices.tiers3.Adresse;
import ch.vd.uniregctb.webservices.tiers3.AdresseAutreTiers;
import ch.vd.uniregctb.webservices.tiers3.TypeAdressePoursuiteAutreTiers;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers3.impl.ExceptionHelper;

public class AdresseBuilder {

	private static final Logger LOGGER = Logger.getLogger(AdresseBuilder.class);

	public static Adresse newAdresse(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra) throws WebServiceException {
		final Adresse a = new Adresse();
		fillAdresse(adresse, serviceInfra, a);
		return a;
	}

	public static void fillAdresse(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra, Adresse a) throws WebServiceException {
		a.setDateDebut(DataHelper.coreToWeb(adresse.getDateDebut()));
		a.setDateFin(DataHelper.coreToWeb(adresse.getDateFin()));
		a.setTitre(adresse.getComplement());
		a.setNumeroAppartement(adresse.getNumeroAppartement());
		a.setRue(adresse.getRue());
		a.setNumeroRue(adresse.getNumero());
		a.setCasePostale(adresse.getCasePostale());
		a.setLocalite(adresse.getLocalite());
		a.setNumeroPostal(adresse.getNumeroPostal());

		final Integer noOfsPays = adresse.getNoOfsPays();
		if (noOfsPays != null) {
			ch.vd.uniregctb.interfaces.model.Pays p;
			try {
				p = serviceInfra.getPays(noOfsPays);
			}
			catch (ServiceInfrastructureException e) {
				LOGGER.error(e, e);
				throw ExceptionHelper.newBusinessException(e.getMessage());
			}
			if (p != null && !p.isSuisse()) {
				a.setPays(p.getNomMinuscule());
			}
		}

		a.setNoOrdrePostal(adresse.getNumeroOrdrePostal());
		a.setNoRue(adresse.getNumeroRue());
		a.setNoPays(noOfsPays);
	}

	public static AdresseAutreTiers newAdresseAutreTiers(AdresseGenerique adresse, ServiceInfrastructureService serviceInfra) throws WebServiceException {
		final AdresseAutreTiers a = new AdresseAutreTiers();
		fillAdresse(adresse, serviceInfra, a);
		a.setType(source2type(adresse.getSource().getType()));
		return a;
	}

	public static TypeAdressePoursuiteAutreTiers source2type(AdresseGenerique.SourceType source) {

		if (source == null) {
			return null;
		}

		switch (source) {
		case FISCALE:
			return TypeAdressePoursuiteAutreTiers.SPECIFIQUE;
		case REPRESENTATION:
			return TypeAdressePoursuiteAutreTiers.MANDATAIRE;
		case CURATELLE:
			return TypeAdressePoursuiteAutreTiers.CURATELLE;
		case CONSEIL_LEGAL:
			return TypeAdressePoursuiteAutreTiers.CONSEIL_LEGAL;
		case TUTELLE:
			return TypeAdressePoursuiteAutreTiers.TUTELLE;
		default:
			throw new IllegalArgumentException("Le type de source = [" + source + "] n'est pas représentable comme type d'adresse autre tiers");
		}

	}
}
