package ch.vd.uniregctb.webservices.tiers2.mock;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FlushMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
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

	@Override
	public boolean exists(Long id) {
		return mockPM.existsPM(id) || target.exists(id);
	}

	@Override
	public List<Long> getAllIds() {
		final List<Long> list = mockPM.getAllIdsPM();
		list.addAll(target.getAllIds());
		return list;
	}

	@Override
	public List<Tiers> getAll() {
		throw new NotImplementedException();
	}

	@Override
	public Tiers get(Long id) {
		throw new NotImplementedException();
	}

	@Override
	public boolean exists(Long id, FlushMode flushModeOverride) {
		throw new NotImplementedException();
	}

	@Override
	public Tiers save(Tiers object) {
		throw new NotImplementedException();
	}

	@Override
	public Object saveObject(Object object) {
		throw new NotImplementedException();
	}

	@Override
	public void remove(Long id) {
		throw new NotImplementedException();
	}

	@Override
	public void removeAll() {
		throw new NotImplementedException();
	}

	@Override
	public HibernateTemplate getHibernateTemplate() {
		throw new NotImplementedException();
	}

	@Override
	public Iterator<Tiers> iterate(String query) {
		throw new NotImplementedException();
	}

	@Override
	public int getCount(Class<?> clazz) {
		throw new NotImplementedException();
	}

	@Override
	public void clearSession() {
		throw new NotImplementedException();
	}

	@Override
	public void evict(Object o) {
		throw new NotImplementedException();
	}

	@Override
	public Tiers get(long id, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	@Override
	public Map<Class, List<Tiers>> getFirstGroupedByClass(int count) {
		throw new NotImplementedException();
	}

	@Override
	public List<Tiers> getBatch(Collection<Long> ids, Set<Parts> parts) {
		throw new NotImplementedException();
	}

	@Override
	public RapportEntreTiers save(RapportEntreTiers object) {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getDirtyIds() {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getAllNumeroIndividu() {
		throw new NotImplementedException();
	}

	@Override
	public Set<Long> getNumerosIndividu(Collection<Long> tiersIds, boolean includesComposantsMenage) {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getHabitantsForMajorite(RegDate dateReference) {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getTiersInRange(int ctbStart, int ctbEnd) {
		throw new NotImplementedException();
	}

	@Override
	public Contribuable getContribuableByNumero(Long numeroContribuable) {
		throw new NotImplementedException();
	}

	@Override
	public DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI) {
		throw new NotImplementedException();
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu) {
		throw new NotImplementedException();
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	@Override
	public Long getNumeroPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu) {
		throw new NotImplementedException();
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
		throw new NotImplementedException();
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	@Override
	public List<PersonnePhysique> getSourciers(int noSourcier) {
		throw new NotImplementedException();
	}

	@Override
	public List<PersonnePhysique> getAllMigratedSourciers() {
		throw new NotImplementedException();
	}

	@Override
	public Tiers getTiersForIndexation(long id) {
		throw new NotImplementedException();
	}

	@Override
	public List<MenageCommun> getMenagesCommuns(List<Long> ids, Set<Parts> parts) {
		throw new NotImplementedException();
	}

	@Override
	public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
		throw new NotImplementedException();
	}

	@Override
	public void updateOids(Map<Long, Integer> tiersOidsMapping) {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getListeDebiteursSansPeriodicites() {
		throw new NotImplementedException();
	}

	@Override
	public <T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal) {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getListeCtbModifies(Date dateDebutRech, Date dateFinRech) {
		throw new NotImplementedException();
	}
}
