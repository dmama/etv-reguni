package ch.vd.uniregctb.webservices.common;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.data.DataEventListener;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class WebServiceEventListenerAdapter implements DataEventListener, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceEventListenerAdapter.class);

	private DataEventService dataEventService;
	private List<WebServiceEventInterface> listeners;
	private TiersDAO tiersDAO;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setListeners(List<WebServiceEventInterface> listeners) {
		this.listeners = listeners;
	}

	@SuppressWarnings({"UnusedDeclaration"})
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
	public void onOrganisationChange(long id) {
		final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(id);
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

	@Override
	public void afterPropertiesSet() throws Exception {
		dataEventService.register(this);
	}
}

