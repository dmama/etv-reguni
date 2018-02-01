package ch.vd.unireg.tiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.foncier.AllegementFoncier;

public class MockTiersDAO implements TiersDAO {
	
	private final List<CollectiviteAdministrative> oids = buildOidMap();
	private final Map<Long, Tiers> store = buildInitialStore(oids);

	private static List<CollectiviteAdministrative> buildOidMap() {
		final List<CollectiviteAdministrative> list = new ArrayList<>();
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID, 1, 1, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 4, 5, 2, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 5, 6, 3, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 6, 7, 4, 1));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 7, 8, 5, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 8, 9, 6, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 9, 10, 7, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 10, 11, 8, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 11, 12, 9, 2));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 12, 13, 10, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 14, 15, 11, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 15, 16, 12, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 16, 17, 13, null));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 17, 18, 14, 3));
		list.add(new CollectiviteAdministrative((long) AutreCommunaute.CAAC_GEN_FIRST_ID + 18, 19, 15, 4));
		return Collections.unmodifiableList(list);
	}

	private static Map<Long, Tiers> buildInitialStore(List<CollectiviteAdministrative> oids) {
		final Map<Long, Tiers> map = new HashMap<>();
		fillStoreWithOids(map, oids);
		return map;
	}

	private static void fillStoreWithOids(Map<Long, Tiers> store, List<CollectiviteAdministrative> oids) {
		for (CollectiviteAdministrative oid : oids) {
			store.put(oid.getId(), oid);
		}
	}

	public void clear() {
		store.clear();
		fillStoreWithOids(store, oids);
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
	public @NotNull List<Heritage> getLiensHeritage(@NotNull Collection<Long> tiersIds) {
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
	public List<Long> getAllIdsFor(boolean includeCancelled, TypeTiers... types) {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getAllIdsFor(boolean includeCancelled, @Nullable Collection<TypeTiers> types) {
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
	public Set<Long> getNumerosIndividusLiesParParente(long noIndividuSource) {
		throw new NotImplementedException();
	}

	@Nullable
	@Override
	public List<Long> getNumerosPMs(Collection<Long> tiersIds) {
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
	public Entreprise getEntrepriseByNumeroOrganisation(long numeroOrganisation) {
		throw new NotImplementedException();
	}

	public Etablissement getEtablissementByNumeroSite(long numeroSite) {
		throw new NotImplementedException();
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
		for (CollectiviteAdministrative ca : oids) {
			if (ca.getNumeroCollectiviteAdministrative() != null && ca.getNumeroCollectiviteAdministrative() == numeroTechnique) {
				return ca;
			}
		}
		return null;
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForDistrict(int numeroDistrict, boolean doNotAutoFlush) {
		for (CollectiviteAdministrative ca : oids) {
			if (ca.getIdentifiantDistrictFiscal() != null && ca.getIdentifiantDistrictFiscal() == numeroDistrict) {
				return ca;
			}
		}
		return null;
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForRegion(int numeroRegion) {
		for (CollectiviteAdministrative ca : oids) {
			if (ca.getIdentifiantRegionFiscale() != null && ca.getIdentifiantRegionFiscale() == numeroRegion) {
				return ca;
			}
		}
		return null;
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {
		return getCollectiviteAdministrativesByNumeroTechnique(numeroTechnique);
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
	public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
		final Long ctbId = debiteur.getContribuableId();
		return (Contribuable) get(ctbId);
	}

	@Override
	public List<Long> getEntreprisesSansRegimeFiscal() {
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
	public DecisionAci addAndSave(Contribuable tiers, DecisionAci decisionAci) {
		throw new NotImplementedException();
	}

	@Override
	public <T extends Declaration> T addAndSave(Tiers tiers, T declaration) {
		throw new NotImplementedException();
	}

	@Override
	public Periodicite addAndSave(DebiteurPrestationImposable debiteur, Periodicite periodicite) {
		throw new NotImplementedException();
	}

	@Override
	public SituationFamille addAndSave(ContribuableImpositionPersonnesPhysiques contribuable, SituationFamille situation) {
		throw new NotImplementedException();
	}

	@Override
	public AdresseTiers addAndSave(Tiers tiers, AdresseTiers adresse) {
		throw new NotImplementedException();
	}

	@Override
	public AdresseMandataire addAndSave(Contribuable contribuable, AdresseMandataire adresse) {
		throw new NotImplementedException();
	}

	@Override
	public IdentificationPersonne addAndSave(PersonnePhysique pp, IdentificationPersonne ident) {
		throw new NotImplementedException();
	}

	@Override
	public IdentificationEntreprise addAndSave(Contribuable ctb, IdentificationEntreprise ident) {
		throw new NotImplementedException();
	}

	@Override
	public DomicileEtablissement addAndSave(Etablissement etb, DomicileEtablissement domicile) {
		throw new NotImplementedException();
	}

	@Override
	public <T extends AllegementFiscal> T addAndSave(Entreprise entreprise, T allegement) {
		throw new NotImplementedException();
	}

	@Override
	public DonneeCivileEntreprise addAndSave(Entreprise entreprise, DonneeCivileEntreprise donneeCivile) {
		throw new NotImplementedException();
	}

	@Override
	public RegimeFiscal addAndSave(Entreprise entreprise, RegimeFiscal regime) {
		throw new NotImplementedException();
	}

	@Override
	public EtatEntreprise addAndSave(Entreprise entreprise, EtatEntreprise etat) {
		throw new NotImplementedException();
	}

	@Override
	public FlagEntreprise addAndSave(Entreprise entreprise, FlagEntreprise flag) {
		throw new NotImplementedException();
	}

	@Override
	public <T extends AutreDocumentFiscal> T addAndSave(Entreprise entreprise, T document) {
		throw new NotImplementedException();
	}

	@Override
	public <T extends AllegementFoncier> T addAndSave(ContribuableImpositionPersonnesMorales pm, T allegementFoncier) {
		throw new NotImplementedException();
	}

	@Override
	public Bouclement addAndSave(Entreprise entreprise, Bouclement bouclement) {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getListeCtbModifies(Date dateDebutRech, Date dateFinRech) {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getIdsConnusDuCivil() {
		throw new NotImplementedException();
	}

	@Override
	public List<Long> getIdsParenteDirty() {
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
	public boolean setFlagBlocageRemboursementAutomatique(long tiersId, boolean newFlag) {
		final Tiers tiers = store.get(tiersId);
		if (tiers != null && (tiers.getBlocageRemboursementAutomatique() == null || newFlag != tiers.getBlocageRemboursementAutomatique())) {
			tiers.setBlocageRemboursementAutomatique(newFlag);
			return true;
		}
		return false;
	}
}
