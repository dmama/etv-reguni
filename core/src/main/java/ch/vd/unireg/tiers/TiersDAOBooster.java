package ch.vd.unireg.tiers;

import javax.persistence.FlushModeType;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseMandataire;
import ch.vd.unireg.adresse.AdresseTiers;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.documentfiscal.AutreDocumentFiscal;
import ch.vd.unireg.foncier.AllegementFoncier;

/**
 * Wrapper sur le Tiers DAO normal qui permet de "booster" (de ralentir, en fait) le temps de récupération de certains tiers. Cette classe est utilisée pour les tests Dynatrace. Elle ne devrait pas
 * être utilisée en production, donc ...
 */
public class TiersDAOBooster implements TiersDAO, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersDAOBooster.class);

	private TiersDAO target;
	private String filename;
	private final Set<Long> boostedIds = new HashSet<>();

	public void setTarget(TiersDAO target) {
		this.target = target;
	}

	public void setIdsFilename(String idsFilename) {
		this.filename = idsFilename;
	}

	@Override
	public boolean exists(Long id) {
		return target.exists(id);
	}

	@Override
	public List<Long> getAllIds() {
		return target.getAllIds();
	}

	@Override
	public List<Long> getAllIdsFor(boolean includeCancelled, TypeTiers... types) {
		return target.getAllIdsFor(includeCancelled, types);
	}

	@Override
	public List<Long> getAllIdsFor(boolean includeCancelled, @Nullable Collection<TypeTiers> types) {
		return target.getAllIdsFor(includeCancelled, types);
	}

	@Override
	public List<Tiers> getAll() {
		return target.getAll();
	}

	@Override
	public Tiers get(Long id) {
		if (boostedIds.contains(id)) {
			boostMePlenty(id);
		}
		return target.get(id);
	}

	@Override
	public boolean exists(Long id, FlushModeType flushModeOverride) {
		return target.exists(id, flushModeOverride);
	}

	@Override
	public Tiers save(Tiers object) {
		return target.save(object);
	}

	@Override
	public Object saveObject(Object object) {
		return target.saveObject(object);
	}

	@Override
	public void remove(Long id) {
		target.remove(id);
	}

	@Override
	public void removeAll() {
		target.removeAll();
	}

	@Override
	public Iterator<Tiers> iterate(String query) {
		return target.iterate(query);
	}

	@Override
	public int getCount(Class<?> clazz) {
		return target.getCount(clazz);
	}

	@Override
	public void clearSession() {
		target.clearSession();
	}

	@Override
	public void evict(Object o) {
		target.evict(o);
	}

	@Override
	public Tiers get(long id, boolean doNotAutoFlush) {
		if (boostedIds.contains(id)) {
			boostMePlenty(id);
		}
		return target.get(id, doNotAutoFlush);
	}

	@Override
	public Map<Class, List<Tiers>> getFirstGroupedByClass(int count) {
		return target.getFirstGroupedByClass(count);
	}

	@Override
	public Set<Long> getRelatedIds(long id, int maxDepth) {
		return target.getRelatedIds(id, maxDepth);
	}

	@Override
	public @NotNull List<Heritage> getLiensHeritage(@NotNull Collection<Long> tiersIds) {
		return target.getLiensHeritage(tiersIds);
	}

	@Override
	public Set<Long> getIdsTiersLies(Collection<Long> ids, boolean includeContactsImpotSource) {
		return target.getIdsTiersLies(ids, includeContactsImpotSource);
	}

	@Override
	public List<Tiers> getBatch(Collection<Long> ids, Set<Parts> parts) {
		for (Long id : ids) {
			if (boostedIds.contains(id)) {
				boostMePlenty(id);
			}
		}
		return target.getBatch(ids, parts);
	}

	@Override
	public RapportEntreTiers save(RapportEntreTiers object) {
		return target.save(object);
	}

	@Override
	public List<Long> getDirtyIds() {
		return target.getDirtyIds();
	}

	@Override
	public List<Long> getAllNumeroIndividu() {
		return target.getAllNumeroIndividu();
	}

	@Override
	public Set<Long> getNumerosIndividu(Collection<Long> tiersIds, boolean includesComposantsMenage) {
		return target.getNumerosIndividu(tiersIds, includesComposantsMenage);
	}

	@Override
	public Set<Long> getNumerosIndividusLiesParParente(long noIndividuSource) {
		return target.getNumerosIndividusLiesParParente(noIndividuSource);
	}

	@Nullable
	@Override
	public List<Long> getNumerosPMs(Collection<Long> tiersIds) {
		return target.getNumerosPMs(tiersIds);
	}

	@Override
	public List<Long> getTiersInRange(int ctbStart, int ctbEnd) {
		return target.getTiersInRange(ctbStart, ctbEnd);
	}

	@Override
	public Contribuable getContribuableByNumero(Long numeroContribuable) {
		return target.getContribuableByNumero(numeroContribuable);
	}

	@Override
	public DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI) {
		return target.getDebiteurPrestationImposableByNumero(numeroDPI);
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(long numeroIndividu) {
		return target.getPPByNumeroIndividu(numeroIndividu);
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		return target.getPPByNumeroIndividu(numeroIndividu, doNotAutoFlush);
	}

	@Override
	public Entreprise getEntrepriseByNoEntrepriseCivile(long numeroEntrepriseCivile) {
		return target.getEntrepriseByNoEntrepriseCivile(numeroEntrepriseCivile);
	}

	@Override
	public Etablissement getEtablissementByNumeroEtablissementCivil(long numeroEtablissementCivil) {
		return target.getEtablissementByNumeroEtablissementCivil(numeroEtablissementCivil);
	}


	@Override
	public Long getNumeroPPByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		return target.getNumeroPPByNumeroIndividu(numeroIndividu, doNotAutoFlush);
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu) {
		return target.getHabitantByNumeroIndividu(numeroIndividu);
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(long numeroIndividu, boolean doNotAutoFlush) {
		return target.getHabitantByNumeroIndividu(numeroIndividu, doNotAutoFlush);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
		return target.getCollectiviteAdministrativesByNumeroTechnique(numeroTechnique);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForDistrict(int numeroDistrict, boolean doNotAutoFlush) {
		return target.getCollectiviteAdministrativeForDistrict(numeroDistrict, doNotAutoFlush);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForRegion(int numeroRegion) {
		return target.getCollectiviteAdministrativeForRegion(numeroRegion);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {
		return target.getCollectiviteAdministrativesByNumeroTechnique(numeroTechnique, doNotAutoFlush);
	}

	@Override
	public List<PersonnePhysique> getSourciers(int noSourcier) {
		return target.getSourciers(noSourcier);
	}

	@Override
	public List<PersonnePhysique> getAllMigratedSourciers() {
		return target.getAllMigratedSourciers();
	}

	@Override
	public Tiers getTiersForIndexation(long id) {
		return target.getTiersForIndexation(id);
	}

	@Override
	public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
		return target.getContribuable(debiteur);
	}

	@Override
	public List<Long> getEntreprisesSansRegimeFiscal() {
		return target.getEntreprisesSansRegimeFiscal();
	}

	@Override
	public List<Long> getEntreprisesAvecRegimeFiscalAt(@NotNull String code, @NotNull RegDate date) {
		return target.getEntreprisesAvecRegimeFiscalAt(code, date);
	}

	@Override
	public void updateOids(Map<Long, Integer> tiersOidsMapping) {
		target.updateOids(tiersOidsMapping);
	}

	@Override
	public List<Long> getListeDebiteursSansPeriodicites() {
		return target.getListeDebiteursSansPeriodicites();
	}

	@Override
	public <T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal) {
		return target.addAndSave(tiers, forFiscal);
	}

	@Override
	public DecisionAci addAndSave(Contribuable tiers, DecisionAci decisionAci) {
		return target.addAndSave(tiers,decisionAci);
	}

	@Override
	public <T extends Declaration> T addAndSave(Tiers tiers, T declaration) {
		return target.addAndSave(tiers, declaration);
	}

	@Override
	public Periodicite addAndSave(DebiteurPrestationImposable debiteur, Periodicite periodicite) {
		return target.addAndSave(debiteur, periodicite);
	}

	@Override
	public SituationFamille addAndSave(ContribuableImpositionPersonnesPhysiques contribuable, SituationFamille situation) {
		return target.addAndSave(contribuable, situation);
	}

	@Override
	public AdresseTiers addAndSave(Tiers tiers, AdresseTiers adresse) {
		return target.addAndSave(tiers, adresse);
	}

	@Override
	public AdresseMandataire addAndSave(Contribuable contribuable, AdresseMandataire adresse) {
		return target.addAndSave(contribuable, adresse);
	}

	@Override
	public IdentificationPersonne addAndSave(PersonnePhysique pp, IdentificationPersonne ident) {
		return target.addAndSave(pp, ident);
	}

	@Override
	public IdentificationEntreprise addAndSave(Contribuable ctb, IdentificationEntreprise ident) {
		return target.addAndSave(ctb, ident);
	}

	@Override
	public DomicileEtablissement addAndSave(Etablissement etb, DomicileEtablissement domicile) {
		return target.addAndSave(etb, domicile);
	}

	@Override
	public <T extends AllegementFiscal> T addAndSave(Entreprise entreprise, T allegement) {
		return target.addAndSave(entreprise, allegement);
	}

	@Override
	public DonneeCivileEntreprise addAndSave(Entreprise entreprise, DonneeCivileEntreprise donneeCivile) {
		return target.addAndSave(entreprise, donneeCivile);
	}

	@Override
	public Bouclement addAndSave(Entreprise entreprise, Bouclement bouclement) {
		return target.addAndSave(entreprise, bouclement);
	}

	@Override
	public RegimeFiscal addAndSave(Entreprise entreprise, RegimeFiscal regime) {
		return target.addAndSave(entreprise, regime);
	}

	@Override
	public EtatEntreprise addAndSave(Entreprise entreprise, EtatEntreprise etat) {
		return target.addAndSave(entreprise, etat);
	}

	@Override
	public FlagEntreprise addAndSave(Entreprise entreprise, FlagEntreprise flag) {
		return target.addAndSave(entreprise, flag);
	}

	@Override
	public <T extends AutreDocumentFiscal> T addAndSave(Entreprise entreprise, T document) {
		return target.addAndSave(entreprise, document);
	}

	@Override
	public <T extends AllegementFoncier> T addAndSave(ContribuableImpositionPersonnesMorales pm, T allegementFoncier) {
		return target.addAndSave(pm, allegementFoncier);
	}

	@Override
	public List<Long> getListeCtbModifies(Date dateDebutRech, Date dateFinRech) {
		return target.getListeCtbModifies(dateDebutRech, dateFinRech);
	}

	@Override
	public List<Long> getIdsConnusDuCivil() {
		return target.getIdsConnusDuCivil();
	}

	@Override
	public List<Long> getIdsParenteDirty() {
		return target.getIdsParenteDirty();
	}

	@Override
	public boolean setFlagBlocageRemboursementAutomatique(long tiersId, boolean newFlag) {
		return target.setFlagBlocageRemboursementAutomatique(tiersId, newFlag);
	}

	@Override
	public void setDirtyFlag(@Nullable Collection<Long> ids, boolean flag, @NotNull Session session) {
		target.setDirtyFlag(ids, flag, session);
	}

	private static final String poppins = "Supercalifragilisticexpialidocious";

	private void boostMePlenty(long id) {
		try {
			Thread.sleep(2000);
		}
		catch (InterruptedException e) {
			// ignored
		}
		final StringBuilder message = new StringBuilder();
		for (char c : poppins.toCharArray()) {
			message.append(c);
		}
		message.append(" (").append(id).append(')');
		LOGGER.error(message.toString());
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		File file = new File(filename);
		if (!file.exists()) {
			LOGGER.warn("Aucun fichier " + filename + " trouvé. Les tiers ne seront pas boostés.");
			return;
		}

		if (!file.canRead()) {
			throw new FileNotFoundException("Le fichier '" + filename + "' n'est pas lisible.");
		}

		// on parse le fichier
		try (Scanner s = new Scanner(file)) {
			while (s.hasNextLine()) {

				final String line = s.nextLine().trim();
				if (StringUtils.isBlank(line)) {
					continue;
				}

				try {
					final Long id = Long.parseLong(line);
					boostedIds.add(id);
				}
				catch (NumberFormatException e) {
					LOGGER.error("La ligne [" + line + "] ne représente pas un long : " + e.getMessage());
				}
			}
		}
	}
}
