package ch.vd.uniregctb.interfaces.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.apireg.datamodel.HostIndividu;
import ch.vd.apireg.regpp.HostIndividuDAO;
import ch.vd.apireg.type.PartiesIndividu;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.model.wrapper.apireg.IndividuWrapper;
import ch.vd.uniregctb.stats.StatsService;

public class ServiceCivilApireg extends ServiceCivilServiceBase implements ServiceCivilService, ServiceTracingInterface, InitializingBean, DisposableBean {

	//private static final Logger LOGGER = Logger.getLogger(ServiceCivilApireg.class);

	private static final String SERVICE_NAME = "Apireg";

	private HibernateTemplate template;
	private HostIndividuDAO dao;
	private ServiceInfrastructureService infraService;

	private final ServiceTracing tracing = new ServiceTracing();

	public void setDao(HostIndividuDAO dao) {
		this.dao = dao;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void afterPropertiesSet() throws Exception {
		this.template = dao.getHibernateTemplate();
		StatsService.registerRawService(SERVICE_NAME, this);
	}

	public void destroy() throws Exception {
		StatsService.unregisterRawService(SERVICE_NAME);
	}

	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date) {
		throw new NotImplementedException();
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)	// transactional : autrement on reçoit des LazyInitializationException sur les états-civils de temps en temps...
	public List<Individu> getIndividus(final Collection<Long> nosIndividus, final RegDate date, final EnumAttributeIndividu... parties) {

		long time = tracing.start();
		try {

			return (List<Individu>) template.executeWithNewSession(new HibernateCallback() {
				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					final List<HostIndividu> lot = dao.getLotIndividus(convertIds(nosIndividus), convertParties(parties));
					final List<Individu> individus = new ArrayList<Individu>(lot.size());
					for (HostIndividu i : lot) {
						individus.add(IndividuWrapper.get(i, date, infraService));
					}
					return individus;
				}
			});

		}
		finally {
			tracing.end(time);
		}

	}

	private Set<Integer> convertIds(Collection<Long> nosIndividus) {
		Set<Integer> ids = new HashSet<Integer>(nosIndividus.size());
		for (Long l : nosIndividus) {
			ids.add(Integer.valueOf(l.intValue()));
		}
		return ids;
	}

	private Set<PartiesIndividu> convertParties(EnumAttributeIndividu... parties) {
		if (parties == null) {
			return null;
		}
		Set<PartiesIndividu> set = new HashSet<PartiesIndividu>(parties.length);
		for (EnumAttributeIndividu e : parties) {
			switch (e.getCode()) {
			case 1:
				set.add(PartiesIndividu.ORIGINE);
				break;
			case 2:
				set.add(PartiesIndividu.TUTELLE);
				break;
			case 4:
				set.add(PartiesIndividu.CONJOINT);
				break;
			case 8:
				set.add(PartiesIndividu.ENFANTS);
				break;
			case 16:
				set.add(PartiesIndividu.ADOPTIONS);
				break;
			case 32:
				set.add(PartiesIndividu.ADRESSES);
				break;
			case 64:
				set.add(PartiesIndividu.PARENTS);
				break;
			case 128:
				set.add(PartiesIndividu.PERMIS);
				break;
			case 256:
				set.add(PartiesIndividu.NATIONALITE);
				break;
			default:
				throw new IllegalArgumentException("Code de partie d'individu inconnu = [" + e.getCode() + "]");
			}
		}
		return set;
	}

	public Individu getIndividu(final long noIndividu, int annee, EnumAttributeIndividu... parties) {
		long time = tracing.start();
		try {
			if (annee == 2400) {
				// une date nulle est transformée en année 2400, mais RegDate n'accepte pas 2400.12.31 -> on transforme donc en 2399.12.31.
				annee--;
			}
			final RegDate date = RegDate.get(annee, 12, 31);

			return (Individu) template.executeWithNewSession(new HibernateCallback() {
				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					HostIndividu individu = dao.get((int) noIndividu);
					return IndividuWrapper.get(individu, date, infraService); // TODO (msi) initialiser les parties
				}
			});

		}
		finally {
			tracing.end(time);
		}
	}

	public Collection<Nationalite> getNationalites(long noIndividu, int annee) {
		throw new NotImplementedException();
	}

	public Origine getOrigine(long noIndividu, int annee) {
		throw new NotImplementedException();
	}

	public Collection<Permis> getPermis(long noIndividu, int annee) {
		throw new NotImplementedException();
	}

	public Permis getPermisActif(long noIndividu, RegDate date) {
		throw new NotImplementedException();
	}

	public Tutelle getTutelle(long noIndividu, int annee) {
		throw new NotImplementedException();
	}

	public void setUp(ServiceCivilService target) {
	}

	public void tearDown() {
	}

	public boolean isWarmable() {
		return false;
	}

	public void warmCache(List<Individu> individus, RegDate date, EnumAttributeIndividu... parties) {
		// rien à faire ici
	}

	public long getLastCallTime() {
		return tracing.getLastCallTime();
	}

	public long getTotalTime() {
		return tracing.getTotalTime();
	}

	public long getTotalPing() {
		return tracing.getTotalPing();
	}

	public long getRecentTime() {
		return tracing.getRecentTime();
	}

	public long getRecentPing() {
		return tracing.getRecentPing();
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
	}
}
