package ch.vd.unireg.webservices.common;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.data.CivilDataEventListener;
import ch.vd.unireg.data.FiscalDataEventListener;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class WebServiceEventListenerAdapter implements CivilDataEventListener, FiscalDataEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceEventListenerAdapter.class);

	private List<WebServiceEventInterface> listeners;
	private TiersDAO tiersDAO;

	public void setListeners(List<WebServiceEventInterface> listeners) {
		this.listeners = listeners;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public void onTiersChange(long id) {
		// on récupère tous les ids des tiers impactés par le changement sur le tiers passé en paramètre
		final Set<Long> ids = tiersDAO.getRelatedIds(id, 2);

		for (WebServiceEventInterface listener : listeners) {
			try {
				// on les évicte tous, sans rémission possible
				listener.onTiersChange(ids);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	@Override
	public void onIndividuChange(long numero) {
		final PersonnePhysique pp = tiersDAO.getPPByNumeroIndividu(numero, true);
		if (pp != null) {
			onTiersChange(pp.getNumero());
		}
	}

	@Override
	public void onEntrepriseChange(long id) {
		final Entreprise entreprise = tiersDAO.getEntrepriseByNoEntrepriseCivile(id);
		if (entreprise != null) {
			onTiersChange(entreprise.getNumero());
		}
	}

	@Override
	public void onDroitAccessChange(long tiersId) {
		// rien à faire
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		// rien à faire
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		listeners.forEach(l -> l.onImmeubleChange(immeubleId));
	}

	@Override
	public void onBatimentChange(long batimentId) {
		listeners.forEach(l -> l.onBatimentChange(batimentId));
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		listeners.forEach(l -> l.onCommunauteChange(communauteId));
	}

	@Override
	public void onTruncateDatabase() {
		for (WebServiceEventInterface listener : listeners) {
			try {
				listener.onTruncateDatabase();
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	@Override
	public void onLoadDatabase() {
		for (WebServiceEventInterface listener : listeners) {
			try {
				listener.onLoadDatabase();
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}
}

