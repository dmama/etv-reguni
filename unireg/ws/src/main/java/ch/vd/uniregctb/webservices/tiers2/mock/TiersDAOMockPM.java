package ch.vd.uniregctb.webservices.tiers2.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.hqlbuilder.AbstractCriteria;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.tiers.*;
import org.hibernate.FlushMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import javax.persistence.NonUniqueResultException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implémentation particulière du Tiers DAO qui permet d'exposer les tiers du Tiers DAO normal fusionnés avec les personnes morales du mock PM.
 * <p>
 * <b>Note:</b> cette classe est destinée à être utilisée avec le SecurityProviderCache uniquement.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class TiersDAOMockPM implements TiersDAO {

	private TiersDAO target;
	private TiersWebServiceMockPM mockPM;

	public void setTarget(TiersDAO target) {
		this.target = target;
	}

	public void setMockPM(TiersWebServiceMockPM mockPM) {
		this.mockPM = mockPM;
	}

	public boolean exists(Long id) {
		return mockPM.existsPM(id) || target.exists(id);
	}

	public List<Long> getAllIds() {
		final List<Long> list = mockPM.getAllIdsPM();
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

	public List<Tiers> getResultList(AbstractCriteria aCriteria) {
		throw new NotImplementedException();
	}

	public List<Tiers> getResultList(AbstractCriteria aCriteria, int aMaxResults) {
		throw new NotImplementedException();
	}

	public Tiers getSingleResult(AbstractCriteria aCriteria) throws NonUniqueResultException {
		throw new NotImplementedException();
	}

	public List<Tiers> getDistinctResultList(AbstractCriteria aCriteria) {
		throw new NotImplementedException();
	}

	public List<Tiers> getDistinctResultList(AbstractCriteria aCriteria, int aMaxResults) {
		throw new NotImplementedException();
	}

	public long getRowCountResult(AbstractCriteria aCriteria) {
		throw new NotImplementedException();
	}

	public Tiers get(long id, boolean doNotAutoFlush) {
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

	public List<Long> getNumerosIndividu(Set<Long> tiersIds, boolean includesComposantsMenage) {
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

	public void updateOids(Map<Long, Integer> tiersOidsMapping) {
		throw new NotImplementedException();
	}
}
