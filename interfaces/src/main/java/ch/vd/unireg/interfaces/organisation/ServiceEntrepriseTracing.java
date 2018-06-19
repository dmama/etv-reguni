package ch.vd.unireg.interfaces.organisation;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivileEvent;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 */
public class ServiceEntrepriseTracing implements ServiceEntrepriseRaw, InitializingBean, DisposableBean {

	private ServiceEntrepriseRaw target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServiceEntrepriseRaw target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	@Override
	public EntrepriseCivile getEntrepriseHistory(final long noEntreprise) throws ServiceEntrepriseException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final EntrepriseCivile entrepriseCivile = target.getEntrepriseHistory(noEntreprise);
			if (entrepriseCivile != null) {
				items = 1;
			}
			return entrepriseCivile;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getEntrepriseHistory", items, () -> String.format("noEntreprise=%d", noEntreprise));
		}
	}

	@Override
	public Long getNoEntrepriseFromNoEtablissement(final Long noEtablissementCivil) throws ServiceEntrepriseException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Long noEntreprise = target.getNoEntrepriseFromNoEtablissement(noEtablissementCivil);
			if (noEntreprise != null) {
				items = 1;
			}
			return noEntreprise;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getNoEntrepriseFromNoEtablissement", items, () -> String.format("noEtablissementCivil=%d", noEtablissementCivil));
		}
	}

	@Override
	public Identifiers getEntrepriseByNoIde(final String noide) throws ServiceEntrepriseException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Identifiers ids = target.getEntrepriseByNoIde(noide);
			if (ids != null) {
				items = 1;
			}
			return ids;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getEntrepriseByNoIde", items, () -> String.format("ide=%s", noide));
		}
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(final long noEvenement) throws ServiceEntrepriseException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Map<Long, EntrepriseCivileEvent> entreprises = target.getEntrepriseEvent(noEvenement);
			if (entreprises != null) {
				items = 1;
			}
			return entreprises;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getEntrepriseEvent", items, () -> String.format("noEvenement=%d", noEvenement));
		}
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull final AnnonceIDEQuery query, @Nullable final Sort.Order order, final int pageNumber, final int resultsPerPage) throws ServiceEntrepriseException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Page<AnnonceIDE> page = target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
			items = page.getNumberOfElements();
			return page;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "findAnnoncesIDE", items, () -> String.format("query=%s, order=%s, pageNumber=%d, resultsPerPage=%d", query, order, pageNumber, resultsPerPage));
		}
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(final BaseAnnonceIDE modele) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final AnnonceIDEEnvoyee.Statut annonceIDEStatut = target.validerAnnonceIDE(modele);
			if (annonceIDEStatut != null) {
				items = 1;
			}
			return annonceIDEStatut;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "validerAnnonceIDE", items, () -> String.format("nomEntreprise=%s", Optional.ofNullable(modele.getContenu()).map(BaseAnnonceIDE.Contenu::getNom).orElse(StringUtils.EMPTY)));
		}
	}

	@Override
	public void ping() throws ServiceEntrepriseException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.ping();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "ping", null);
		}
	}
}
