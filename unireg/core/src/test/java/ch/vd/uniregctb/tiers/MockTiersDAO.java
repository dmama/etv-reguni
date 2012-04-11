package ch.vd.uniregctb.tiers;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FlushMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.rf.Immeuble;

public class MockTiersDAO implements TiersDAO {
	
	private Map<Long, Tiers> store = new HashMap<Long, Tiers>();

	public void clear() {
		store.clear();
	}

	@Override
	public Tiers get(long id, boolean doNotAutoFlush) {
		return store.get(id);
	}

	@Override
	public Map<Class, List<Tiers>> getFirstGroupedByClass(int count) {
		throw new NotImplementedException();
	}

	@Override
	public Set<Long> getRelatedIds(long id, int maxDepth) {
		throw new NotImplementedException();
	}

	@Override
	public Set<Long> getIdsTiersLies(Collection<Long> ids, boolean includeContactsImpotSource) {
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
	public List<Long> getAllIds() {
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
	public PersonnePhysique getPPByNumeroIndividu(long numeroIndividu) {
		throw new NotImplementedException();
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		for (Tiers tiers : store.values()) {
			if (tiers instanceof PersonnePhysique && numeroIndividu == ((PersonnePhysique) tiers).getNumeroIndividu()) {
				return (PersonnePhysique) tiers;
			}
		}
		return null;
	}

	@Override
	public Long getNumeroPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu) {
		throw new NotImplementedException();
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		throw new NotImplementedException();
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
		throw new NotImplementedException();
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForDistrict(Integer numeroDistrict) {
		throw new NotImplementedException();
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForRegion(Integer numeroRegion) {
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
		final Long ctbId = debiteur.getContribuableId();
		return (Contribuable) get(ctbId);
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
	public Immeuble addAndSave(Contribuable tiers, Immeuble immeuble) {
		throw new NotImplementedException();
	}

	@Override
	public Declaration addAndSave(Tiers tiers, Declaration declaration) {
		throw new NotImplementedException();
	}

	@Override
	public Periodicite addAndSave(DebiteurPrestationImposable debiteur, Periodicite periodicite) {
		throw new NotImplementedException();
	}

	@Override
	public SituationFamille addAndSave(Contribuable contribuable, SituationFamille situation) {
		throw new NotImplementedException();
	}

	@Override
	public AdresseTiers addAndSave(Tiers tiers, AdresseTiers adresse) {
		throw new NotImplementedException();
	}

	@Override
	public IdentificationPersonne addAndSave(PersonnePhysique pp, IdentificationPersonne ident) {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getListeCtbModifies(Date dateDebutRech, Date dateFinRech) {
		throw new NotImplementedException();
	}

	@Override
	public List<Tiers> getAll() {
		throw new NotImplementedException();
	}

	@Override
	public Tiers get(Long id) {
		return store.get(id);
	}

	@Override
	public boolean exists(Long id) {
		return store.containsKey(id);
	}

	@Override
	public boolean exists(Long id, FlushMode flushModeOverride) {
		throw new NotImplementedException();
	}

	@Override
	public Tiers save(Tiers object) {
		if (object.getId() == null) {
			throw new IllegalArgumentException("L'id doit être renseigné !");
		}
		store.put(object.getId(), object);
		return object;
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
}
