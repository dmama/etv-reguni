package ch.vd.uniregctb.webservices.tiers2.impl.pm;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FlushMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Implémentation particulière du Tiers DAO qui permet d'exposer les tiers du Tiers DAO normal fusionnés avec les personnes morales du web-service PM.
 * <p>
 * <b>Note:</b> cette classe est destinée à être utilisée avec le SecurityProviderCache uniquement.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class TiersDAOWithPM implements TiersDAO {

	private TiersDAO target;
	private ServicePersonneMoraleService servicePM;

	public void setTarget(TiersDAO target) {
		this.target = target;
	}

	public void setServicePM(ServicePersonneMoraleService servicePM) {
		this.servicePM = servicePM;
	}

	public boolean exists(Long id) {
		return target.exists(id) || servicePM.getPersonneMorale(id) != null;
	}

	public List<Long> getAllIds() {
		final List<Long> list = servicePM.getAllIds();
		list.addAll(target.getAllIds());
		return list;
	}

	public List<Tiers> getAll() {
		throw new NotImplementedException();
	}

	public Tiers get(Long id) {
		throw new NotImplementedException();
	}

	public boolean exists(Long id, FlushMode flushModeOverride) {
		throw new NotImplementedException();
	}

	public Tiers save(Tiers object) {
		throw new NotImplementedException();
	}

	public Object saveObject(Object object) {
		throw new NotImplementedException();
	}

	public void remove(Long id) {
		throw new NotImplementedException();
	}

	public void removeAll() {
		throw new NotImplementedException();
	}

	public HibernateTemplate getHibernateTemplate() {
		throw new NotImplementedException();
	}

	public Iterator<Tiers> iterate(String query) {
		throw new NotImplementedException();
	}

	public int getCount(Class<?> clazz) {
		throw new NotImplementedException();
	}

	public void clearSession() {
		throw new NotImplementedException();
	}

	public void evict(Object o) {
		throw new NotImplementedException();
	}

	public Tiers get(long id, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	public Map<Class, List<Tiers>> getFirstGroupedByClass(int count) {
		throw new NotImplementedException();
	}

	public List<Tiers> getBatch(Collection<Long> ids, Set<Parts> parts) {
		throw new NotImplementedException();
	}

	public RapportEntreTiers save(RapportEntreTiers object) {
		throw new NotImplementedException();
	}

	public List<Long> getDirtyIds() {
		throw new NotImplementedException();
	}

	public List<Long> getAllNumeroIndividu() {
		throw new NotImplementedException();
	}

	public Set<Long> getNumerosIndividu(Collection<Long> tiersIds, boolean includesComposantsMenage) {
		throw new NotImplementedException();
	}

	public List<Long> getHabitantsForMajorite(RegDate dateReference) {
		throw new NotImplementedException();
	}

	public List<Long> getTiersInRange(int ctbStart, int ctbEnd) {
		throw new NotImplementedException();
	}

	public Contribuable getContribuableByNumero(Long numeroContribuable) {
		throw new NotImplementedException();
	}

	public DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI) {
		throw new NotImplementedException();
	}

	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu) {
		throw new NotImplementedException();
	}

	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	public Long getNumeroPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}
	
	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu) {
		throw new NotImplementedException();
	}

	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
		throw new NotImplementedException();
	}

	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	public List<PersonnePhysique> getSourciers(int noSourcier) {
		throw new NotImplementedException();
	}

	public List<PersonnePhysique> getAllMigratedSourciers() {
		throw new NotImplementedException();
	}

	public Tiers getTiersForIndexation(long id) {
		throw new NotImplementedException();
	}

	public List<MenageCommun> getMenagesCommuns(List<Long> ids, Set<Parts> parts) {
		throw new NotImplementedException();
	}

	public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
		throw new NotImplementedException();
	}

	public void updateOids(Map<Long,Integer> tiersOidsMapping) {
		throw new NotImplementedException();
	}

	public List<Long> getListeDebiteursSansPeriodicites() {
		throw new NotImplementedException();
	}

	public <T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal) {
		throw new NotImplementedException();
	}
}
