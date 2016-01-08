package ch.vd.uniregctb.migration.pm.engine.helpers;

import java.util.Map;
import java.util.TreeMap;

import ch.vd.unireg.interfaces.organisation.WrongOrganisationReceivedException;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.utils.LockHelper;

public class OrganisationServiceAccessor {

	private final ServiceOrganisationService service;
	private final boolean rcentEnabled;

	/**
	 * On ne prendra en compte qu'un numéro cantonal sur x (x > 0...)<br/>
	 * Si x = 1, on les prend tous, sinon, on en prend moins...
	 */
	private final int frequencePriseEnCompteIdCantonal;
	private final Map<Long, Boolean> idsCantonauxVus = new TreeMap<>();
	private final LockHelper lock = new LockHelper(true);

	public OrganisationServiceAccessor(ServiceOrganisationService service, boolean rcentEnabled, int frequencePriseEnCompteIdCantonal) {
		this.service = service;
		this.rcentEnabled = rcentEnabled;
		this.frequencePriseEnCompteIdCantonal = frequencePriseEnCompteIdCantonal;
		if (frequencePriseEnCompteIdCantonal < 1) {
			throw new IllegalArgumentException("Le champ 'frequencePriseEnCompteIdCantonal' doit avoir une valeur strictement positive (" + frequencePriseEnCompteIdCantonal + ").");
		}
	}

	public Organisation getOrganisation(long idCantonal, MigrationResultProduction mr) {
		if (!rcentEnabled) {
			return null;
		}

		if (frequencePriseEnCompteIdCantonal > 1) {
			// n'aurait-on pas déjà vu ce numéro
			final Boolean aPrendreEnCompte = lock.doInReadLock(() -> idsCantonauxVus.get(idCantonal));

			// si on ne sait pas encore, il faut se prononcer
			if (aPrendreEnCompte == null) {
				final boolean prisEnCompte = lock.doInWriteLock(() -> {
					if (idsCantonauxVus.size() % frequencePriseEnCompteIdCantonal > 0) {
						mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Lien vers le civil ignoré.");
						idsCantonauxVus.put(idCantonal, Boolean.FALSE);
						return false;
					}

					// il faut nous en souvenir pour la prochaine fois, pour rester cohérent
					idsCantonauxVus.put(idCantonal, Boolean.TRUE);
					return true;
				});

				if (!prisEnCompte) {
					return null;
				}
			}
			else if (!aPrendreEnCompte) {
				return null;
			}
		}

		try {
			return service.getOrganisationHistory(idCantonal);
		}
		catch (WrongOrganisationReceivedException ex) {

			// le numéro que nous avions était un numéro d'établissement, flûte !...
			// il faut donc faire un nouvel appel avec le bon numéro pour obtenir la totalité des données de l'entreprise,,,

			idsCantonauxVus.put(ex.getReceivedId(), Boolean.TRUE);
			mr.addMessage(LogCategory.SUIVI, LogLevel.WARN, String.format("Le numéro cantonal fourni est un numéro d'établissement, le vrai numéro d'organisation est %d.", ex.getReceivedId()));
			return service.getOrganisationHistory(ex.getReceivedId());
		}
	}

	public Long getOrganisationPourSite(long idCantonalSite) {
		if (!rcentEnabled) {
			return null;
		}

		return service.getOrganisationPourSite(idCantonalSite);
	}

	public boolean isRcentEnabled() {
		return rcentEnabled;
	}
}
