package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.adresse.AdressesCivilesHisto;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class ServiceCivilServiceBase implements ServiceCivilService {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilServiceBase.class);

	private final List<CivilListener> listeners = new ArrayList<CivilListener>();

	public final AdressesCiviles getAdresses(long noIndividu, RegDate date) {

		final int year = (date == null ? 2400 : date.year());
		final Collection<Adresse> adressesCiviles = getAdresses(noIndividu, year);

		AdressesCiviles resultat = new AdressesCiviles();

		try {
			if (adressesCiviles != null) {
				for (Adresse adresse : adressesCiviles) {
					if (adresse != null && adresse.isValidAt(date)) {
						resultat.set(adresse);
					}
				}
			}
		}
		catch (DonneesCivilesException e) {
			throw new DonneesCivilesException(e.getMessage() + " sur l'individu n°" + noIndividu + " et pour l'année " + year + ".");
		}

		return resultat;
	}

	public final AdressesCivilesHisto getAdressesHisto(long noIndividu) {

		final int all = 2400;
		final Collection<Adresse> adressesCiviles = getAdresses(noIndividu, all);

		AdressesCivilesHisto resultat = new AdressesCivilesHisto();

		if (adressesCiviles != null) {
			for (Adresse adresse : adressesCiviles) {
				if (adresse != null) {
					resultat.add(adresse);
				}
			}
		}
		resultat.sort();

		return resultat;
	}

	public final Collection<Adresse> getAdresses(long noIndividu, int annee) {
		final Individu individu = getIndividu(noIndividu, annee, EnumAttributeIndividu.ADRESSES);
		if (individu == null) {
			return null;
		}
		return individu.getAdresses();
	}

	public final Individu getIndividu(long noIndividu, int annee) {
		return getIndividu(noIndividu, annee, (EnumAttributeIndividu[])null); // -> va charger implicitement l'état-civil et l'historique
	}

	public void onIndividuChange(long numero) {
		for (CivilListener l : listeners) {
			try {
				l.onIndividuChange(numero);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	public void register(CivilListener listener) {
		listeners.add(listener);
	}
}
