package ch.vd.uniregctb.tiers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.RegDate;

/**
 * Wrapper sur le Tiers DAO normal qui permet de "booster" (de ralentir, en fait) le temps de récupération de certains tiers. Cette classe est utilisée pour les tests Dynatrace. Elle ne devrait pas
 * être utilisée en production, donc ...
 */
public class TiersDAOBooster implements TiersDAO, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(TiersDAOBooster.class);

	private TiersDAO target;
	private String filename;
	private Set<Long> boostedIds = new HashSet<Long>();

	public void setTarget(TiersDAO target) {
		this.target = target;
	}

	public void setIdsFilename(String idsFilename) {
		this.filename = idsFilename;
	}

	public boolean exists(Long id) {
		return target.exists(id);
	}

	public List<Long> getAllIds() {
		return target.getAllIds();
	}

	public List<Tiers> getAll() {
		return target.getAll();
	}

	public Tiers get(Long id) {
		if (boostedIds.contains(id)) {
			boostMePlenty(id);
		}
		return target.get(id);
	}

	public boolean exists(Long id, FlushMode flushModeOverride) {
		return target.exists(id, flushModeOverride);
	}

	public Tiers save(Tiers object) {
		return target.save(object);
	}

	public Object saveObject(Object object) {
		return target.saveObject(object);
	}

	public void remove(Long id) {
		target.remove(id);
	}

	public void removeAll() {
		target.removeAll();
	}

	public HibernateTemplate getHibernateTemplate() {
		return target.getHibernateTemplate();
	}

	public Iterator<Tiers> iterate(String query) {
		return target.iterate(query);
	}

	public int getCount(Class<?> clazz) {
		return target.getCount(clazz);
	}

	public void clearSession() {
		target.clearSession();
	}

	public void evict(Object o) {
		target.evict(o);
	}

	public Tiers get(long id, boolean doNotAutoFlush) {
		if (boostedIds.contains(id)) {
			boostMePlenty(id);
		}
		return target.get(id, doNotAutoFlush);
	}

	public Map<Class, List<Tiers>> getFirstGroupedByClass(int count) {
		return target.getFirstGroupedByClass(count);
	}

	public List<Tiers> getBatch(Collection<Long> ids, Set<Parts> parts) {
		for (Long id : ids) {
			if (boostedIds.contains(id)) {
				boostMePlenty(id);
			}
		}
		return target.getBatch(ids, parts);
	}

	public RapportEntreTiers save(RapportEntreTiers object) {
		return target.save(object);
	}

	public List<Long> getDirtyIds() {
		return target.getDirtyIds();
	}

	public List<Long> getAllNumeroIndividu() {
		return target.getAllNumeroIndividu();
	}

	public Set<Long> getNumerosIndividu(Collection<Long> tiersIds, boolean includesComposantsMenage) {
		return target.getNumerosIndividu(tiersIds, includesComposantsMenage);
	}

	public List<Long> getHabitantsForMajorite(RegDate dateReference) {
		return target.getHabitantsForMajorite(dateReference);
	}

	public List<Long> getTiersInRange(int ctbStart, int ctbEnd) {
		return target.getTiersInRange(ctbStart, ctbEnd);
	}

	public Contribuable getContribuableByNumero(Long numeroContribuable) {
		return target.getContribuableByNumero(numeroContribuable);
	}

	public DebiteurPrestationImposable getDebiteurPrestationImposableByNumero(Long numeroDPI) {
		return target.getDebiteurPrestationImposableByNumero(numeroDPI);
	}

	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu) {
		return target.getPPByNumeroIndividu(numeroIndividu);
	}

	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		return target.getPPByNumeroIndividu(numeroIndividu, doNotAutoFlush);
	}

	public Long getNumeroPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		return target.getNumeroPPByNumeroIndividu(numeroIndividu, doNotAutoFlush);
	}

	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu) {
		return target.getHabitantByNumeroIndividu(numeroIndividu);
	}

	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		return target.getHabitantByNumeroIndividu(numeroIndividu, doNotAutoFlush);
	}

	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
		return target.getCollectiviteAdministrativesByNumeroTechnique(numeroTechnique);
	}

	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique, boolean doNotAutoFlush) {
		return target.getCollectiviteAdministrativesByNumeroTechnique(numeroTechnique, doNotAutoFlush);
	}

	public List<PersonnePhysique> getSourciers(int noSourcier) {
		return target.getSourciers(noSourcier);
	}

	public List<PersonnePhysique> getAllMigratedSourciers() {
		return target.getAllMigratedSourciers();
	}

	public Tiers getTiersForIndexation(long id) {
		return target.getTiersForIndexation(id);
	}

	public List<MenageCommun> getMenagesCommuns(List<Long> ids, Set<Parts> parts) {
		return target.getMenagesCommuns(ids, parts);
	}

	public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
		return target.getContribuable(debiteur);
	}

	public void updateOids(Map<Long, Integer> tiersOidsMapping) {
		target.updateOids(tiersOidsMapping);
	}

	public List<Long> getListeDebiteursSansPeriodicites() {
		return target.getListeDebiteursSansPeriodicites();
	}

	public <T extends ForFiscal> T addAndSave(Tiers tiers, T forFiscal) {
		return target.addAndSave(tiers, forFiscal);
	}

	private static final String poppins = "Supercalifragilisticexpialidocious";

	private void boostMePlenty(long id) {
		try {
			Thread.sleep(2000);
		}
		catch (InterruptedException e) {
			// ignored
		}
		String message = "";
		for (char c : poppins.toCharArray()) {
			message = message + c;
		}
		message += " (" + id + ")";
		LOGGER.error(message);
	}

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
		Scanner s = new Scanner(file);
		try {
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
		finally {
			s.close();
		}
	}
}
