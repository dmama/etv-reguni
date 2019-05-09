package ch.vd.unireg.tiers.etats;

import javax.persistence.FlushModeType;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.foncier.AllegementFoncier;
import ch.vd.unireg.tiers.AllegementFiscal;
import ch.vd.unireg.tiers.Bouclement;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.DecisionAci;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.DonneeCivileEntreprise;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.EtatEntreprise;
import ch.vd.unireg.tiers.FlagEntreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.IdentificationPersonne;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RegimeFiscal;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersDAOImpl;
import ch.vd.unireg.tiers.TypeTiers;

/**
 * Classe de mock dédiée aux tests sur la gestion des états de l'entreprise.
 *
 * @author Raphaël Marmier, 2016-01-22, <raphael.marmier@vd.ch>
 */
class TransitionEtatMockTiersDao extends TiersDAOImpl implements TiersDAO {

	/**
	 * L'entreprise reçue en paramètre de addAndSave()
	 */
	private Entreprise entreprise;

	/**
	 * L'état reçu en paramètre de addAndSave()
	 */
	private EtatEntreprise nouvelEtatEntreprise;

	/**
	 * @return l'entreprise reçue en paramètre de addAndSave()
	 */
	public Entreprise getEntreprise() {
		return entreprise;
	}

	/**
 	 * @return l'état reçu en paramètre de addAndSave()
	 */
	public EtatEntreprise getNouvelEtatEntreprise() {
		return nouvelEtatEntreprise;
	}

	@Override
	public Tiers get(long id, boolean doNotAutoFlush) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Class, List<Tiers>> getFirstGroupedByClass(int count) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Long> getRelatedIds(long id, int maxDepth) {
		throw new UnsupportedOperationException();
	}

	@Override
	public @NotNull List<Heritage> getLiensHeritage(@NotNull Collection<Long> tiersIds) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Long> getIdsTiersLies(Collection<Long> ids, boolean includeContactsImpotSource) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Tiers> getBatch(Collection<Long> ids, Set<Parts> parts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RapportEntreTiers save(RapportEntreTiers object) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getAllIds() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getAllIdsFor(boolean includeCancelled, TypeTiers... types) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getDirtyIds() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getAllNumeroIndividu() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Long> getNumerosIndividu(Collection<Long> tiersIds, boolean includesComposantsMenage) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Long> getNumerosIndividusLiesParParente(long noIndividuSource) {
		throw new UnsupportedOperationException();
	}

	@Nullable
	@Override
	public List<Long> getNumerosPMs(Collection<Long> tiersIds) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getTiersInRange(int ctbStart, int ctbEnd) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Contribuable getContribuableByNumero(Long numeroContribuable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(long numeroIndividu) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entreprise getEntrepriseByNoEntrepriseCivile(long numeroEntrepriseCivile) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Etablissement getEtablissementByNumeroEtablissementCivil(long numeroEtablissementCivil) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Long getNumeroPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForDistrict(int numeroDistrict, boolean doNotAutoFlush) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForRegion(int numeroRegion) {
		throw new UnsupportedOperationException();
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<PersonnePhysique> getSourciers(int noSourcier) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<PersonnePhysique> getAllMigratedSourciers() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Tiers getTiersForIndexation(long id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateOids(Map<Long, Integer> tiersOidsMapping) {

	}

	@Override
	public List<Long> getListeDebiteursSansPeriodicites() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DecisionAci addAndSave(Contribuable tiers, DecisionAci decisionAci) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Declaration> T addAndSave(Tiers tiers, T declaration) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Periodicite addAndSave(DebiteurPrestationImposable debiteur, Periodicite periodicite) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SituationFamille addAndSave(ContribuableImpositionPersonnesPhysiques contribuable, SituationFamille situation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AdresseTiers addAndSave(Tiers tiers, AdresseTiers adresse) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AdresseMandataire addAndSave(Contribuable contribuable, AdresseMandataire adresse) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IdentificationPersonne addAndSave(PersonnePhysique pp, IdentificationPersonne ident) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IdentificationEntreprise addAndSave(Contribuable ctb, IdentificationEntreprise ident) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DomicileEtablissement addAndSave(Etablissement etb, DomicileEtablissement domicile) {
		throw new UnsupportedOperationException();
	}

	@Override
	public 	<T extends AllegementFiscal> T addAndSave(Entreprise entreprise, T allegement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DonneeCivileEntreprise addAndSave(Entreprise entreprise, DonneeCivileEntreprise donneeCivile) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Bouclement addAndSave(Entreprise entreprise, Bouclement bouclement) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RegimeFiscal addAndSave(Entreprise entreprise, RegimeFiscal regime) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EtatEntreprise addAndSave(Entreprise entreprise, EtatEntreprise etat) {
		entreprise.addEtat(etat);

		this.entreprise = entreprise;
		this.nouvelEtatEntreprise = etat;

		return etat;
	}

	@Override
	public FlagEntreprise addAndSave(Entreprise entreprise, FlagEntreprise flag) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends AutreDocumentFiscal> T addAndSave(Entreprise entreprise, T document) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends AllegementFoncier> T addAndSave(ContribuableImpositionPersonnesMorales pm, T allegementFoncier) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getListeCtbModifies(Date dateDebutRech, Date dateFinRech) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getIdsConnusDuCivil() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getIdsParenteDirty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setFlagBlocageRemboursementAutomatique(long tiersId, boolean newFlag) {
		return false;
	}

	@Override
	public List<Tiers> getAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Tiers get(Long aLong) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean exists(Long aLong) {
		return false;
	}

	@Override
	public boolean exists(Long aLong, FlushModeType flushMode) {
		return false;
	}

	@Override
	public Tiers save(Tiers tiers) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object saveObject(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(Long aLong) {

	}

	@Override
	public void removeAll() {

	}

	@Override
	public Iterator<Tiers> iterate(String s) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getCount(Class<?> aClass) {
		return 0;
	}

	@Override
	public void clearSession() {

	}

	@Override
	public void evict(Object o) {

	}
}
