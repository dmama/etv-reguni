package ch.vd.uniregctb.tiers;

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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.rf.Immeuble;

/**
 * Wrapper sur le Tiers DAO normal qui permet de "booster" (de ralentir, en fait) le temps de récupération de certains tiers. Cette classe est utilisée pour les tests Dynatrace. Elle ne devrait pas
 * être utilisée en production, donc ...
 */
public class TiersDAOBooster implements TiersDAO, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(TiersDAOBooster.class);

	private TiersDAO target;
	private String filename;
	private final Set<Long> boostedIds = new HashSet<Long>();

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
	public boolean exists(Long id, FlushMode flushModeOverride) {
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
	public HibernateTemplate getHibernateTemplate() {
		return target.getHibernateTemplate();
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
	public List<Long> getHabitantsForMajorite(RegDate dateReference) {
		return target.getHabitantsForMajorite(dateReference);
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
	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu) {
		return target.getPPByNumeroIndividu(numeroIndividu);
	}

	@Override
	public PersonnePhysique getPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		return target.getPPByNumeroIndividu(numeroIndividu, doNotAutoFlush);
	}

	@Override
	public Long getNumeroPPByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		return target.getNumeroPPByNumeroIndividu(numeroIndividu, doNotAutoFlush);
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu) {
		return target.getHabitantByNumeroIndividu(numeroIndividu);
	}

	@Override
	public PersonnePhysique getHabitantByNumeroIndividu(Long numeroIndividu, boolean doNotAutoFlush) {
		return target.getHabitantByNumeroIndividu(numeroIndividu, doNotAutoFlush);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativesByNumeroTechnique(int numeroTechnique) {
		return target.getCollectiviteAdministrativesByNumeroTechnique(numeroTechnique);
	}

	@Override
	public CollectiviteAdministrative getCollectiviteAdministrativeForRegion(Integer numeroRegion) {
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
	public List<MenageCommun> getMenagesCommuns(List<Long> ids, Set<Parts> parts) {
		return target.getMenagesCommuns(ids, parts);
	}

	@Override
	public Contribuable getContribuable(DebiteurPrestationImposable debiteur) {
		return target.getContribuable(debiteur);
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
	public Immeuble addAndSave(Contribuable tiers, Immeuble immeuble) {
		return target.addAndSave(tiers, immeuble);
	}

	@Override
	public List<Long> getListeCtbModifies(Date dateDebutRech, Date dateFinRech) {
		return target.getListeCtbModifies(dateDebutRech, dateFinRech);
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
