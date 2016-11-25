package ch.vd.unireg.interfaces.organisation;

import java.util.Map;

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
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 */
public class ServiceOrganisationTracing implements ServiceOrganisationRaw, InitializingBean, DisposableBean {

	private ServiceOrganisationRaw target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServiceOrganisationRaw target) {
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
	public Organisation getOrganisationHistory(final long noOrganisation) throws ServiceOrganisationException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Organisation organisation = target.getOrganisationHistory(noOrganisation);
			if (organisation != null) {
				items = 1;
			}
			return organisation;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOrganisationHistory", items, new Object() {
				@Override
				public String toString() {
					return String.format("noOrganisation=%d", noOrganisation);
				}
			});
		}
	}

	@Override
	public Long getOrganisationPourSite(final Long noSite) throws ServiceOrganisationException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Long noOrganisation = target.getOrganisationPourSite(noSite);
			if (noOrganisation != null) {
				items = 1;
			}
			return noOrganisation;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOrganisationPourSite", items, new Object() {
				@Override
				public String toString() {
					return String.format("noSite=%d", noSite);
				}
			});
		}
	}

	@Override
	public Identifiers getOrganisationByNoIde(final String noide) throws ServiceOrganisationException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Identifiers ids = target.getOrganisationByNoIde(noide);
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
			tracing.end(time, t, "getOrganisationByNoIde", items, new Object() {
				@Override
				public String toString() {
					return String.format("ide=%s", noide);
				}
			});
		}
	}

	@Override
	public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(final long noEvenement) throws ServiceOrganisationException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Map<Long, ServiceOrganisationEvent> organisations = target.getOrganisationEvent(noEvenement);
			if (organisations != null) {
				items = 1;
			}
			return organisations;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPseudoOrganisationHistory", items, new Object() {
				@Override
				public String toString() {
					return String.format("noEvenement=%d", noEvenement);
				}
			});
		}
	}

	@Override
	public AnnonceIDEEnvoyee getAnnonceIDE(final long numero) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final AnnonceIDEEnvoyee annonceIDE = target.getAnnonceIDE(numero);
			if (annonceIDE != null) {
				items = 1;
			}
			return annonceIDE;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAnnonceIDE", items, new Object() {
				@Override
				public String toString() {
					return String.format("noAnnonceIDE=%d", numero);
				}
			});
		}
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull final AnnonceIDEQuery query, @Nullable final Sort.Order order, final int pageNumber, final int resultsPerPage) throws ServiceOrganisationException {
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
			tracing.end(time, t, "findAnnoncesIDE", items, new Object() {
				@Override
				public String toString() {
					return String.format("query=%s, order=%s, pageNumber=%d, resultsPerPage=%d", query, order, pageNumber, resultsPerPage);
				}
			});
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
			tracing.end(time, t, "validerAnnonceIDE", items, new Object() {
				@Override
				public String toString() {
					final BaseAnnonceIDE.Contenu contenu = modele.getContenu();
					return String.format("nomEntreprise=%s", contenu == null ? "" : contenu.getNom());
				}
			});
		}
	}

	@Override
	public void ping() throws ServiceOrganisationException {
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
