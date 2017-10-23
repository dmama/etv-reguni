package ch.vd.uniregctb.tiers.etats;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FlushMode;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.documentfiscal.AutreDocumentFiscal;
import ch.vd.uniregctb.foncier.AllegementFoncier;
import ch.vd.uniregctb.tiers.AllegementFiscal;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.DonneeCivileEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.EtatEntreprise;
import ch.vd.uniregctb.tiers.FlagEntreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.IdentificationEntreprise;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersDAOImpl;
import ch.vd.uniregctb.tiers.TypeTiers;

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
	public List<Long> getHabitantsForMajorite(RegDate dateReference) {
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
	public Entreprise getEntrepriseByNumeroOrganisation(long numeroOrganisation) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Etablissement getEtablissementByNumeroSite(long numeroSite) {
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
	public boolean exists(Long aLong, FlushMode flushMode) {
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
