package ch.vd.uniregctb.tache;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CommonMapHelper;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.SecurityDebugConfig;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

public class TacheMapHelper extends CommonMapHelper {

	private Map<Integer, String> mapPeriodeFiscale;
	private Map<TypeEtatTache, String> mapEtatTache;
	private Map<TypeTache, String> mapTypeTache;
	private Map<Integer, String> mapOID;

	private PeriodeFiscaleDAO periodeFiscaleDAO;

    private ServiceSecuriteService serviceSecurite;

	private PlatformTransactionManager transactionManager;

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Initialise la map des periodes fiscales
	 * @return une map
	 */
	public Map<Integer, String> initMapPeriodeFiscale() {
		if (mapPeriodeFiscale == null) {

			TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);

			final Map<Integer, String> map = new TreeMap<Integer, String>();

			template.execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					final List<PeriodeFiscale> periodes = periodeFiscaleDAO.getAllDesc();
					for (PeriodeFiscale periode : periodes) {
						final int annee = periode.getAnnee();
						map.put(annee, Integer.toString(annee));
					}
					return null;
				}
			});

			mapPeriodeFiscale = map;
		}
		return mapPeriodeFiscale;
	}

	/**
	 * Initialise la map des etats de tache
	 * @return une map
	 */
	public Map<TypeEtatTache, String> initMapEtatTache() {
		if (mapEtatTache == null) {
			mapEtatTache = initMapEnum(ApplicationConfig.masterKeyEtatTache, TypeEtatTache.class);
		}
		return mapEtatTache;
	}

	/**
	 * Initialise la map des types de tache
	 * @return une map
	 */
	public Map<TypeTache, String> initMapTypeTache() {
		if (mapTypeTache == null) {
			mapTypeTache = initMapEnum(ApplicationConfig.masterKeyTypeTache, TypeTache.class, TypeTache.TacheNouveauDossier);
		}
		return mapTypeTache;
	}

	/**
	 * Initialise la map des OID
	 * @return une map
	 */
	public Map<Integer, String> initMapOfficeImpotUtilisateur() {

		final Map<Integer, String> map = new HashMap<Integer, String>();
		if (!SecurityDebugConfig.isIfoSecDebug()) {
			final List<ch.vd.infrastructure.model.CollectiviteAdministrative> collectivites = serviceSecurite.getCollectivitesUtilisateur(AuthenticationHelper.getCurrentPrincipal());
			if (collectivites != null) {
				for (ch.vd.infrastructure.model.CollectiviteAdministrative collectivite : collectivites) {
					map.put(collectivite.getNoColAdm(), collectivite.getNomCourt());
				}
			}
		}
		else {
			map.put(MockCollectiviteAdministrative.ACI.getNoColAdm(), MockCollectiviteAdministrative.ACI.getNomCourt());
			map.put(MockOfficeImpot.OID_AIGLE.getNoColAdm(), MockOfficeImpot.OID_AIGLE.getNomCourt());
			map.put(MockOfficeImpot.OID_ROLLE.getNoColAdm(), MockOfficeImpot.OID_ROLLE.getNomCourt());
			map.put(MockOfficeImpot.OID_AVENCHE.getNoColAdm(), MockOfficeImpot.OID_AVENCHE.getNomCourt());
			map.put(MockOfficeImpot.OID_COSSONAY.getNoColAdm(), MockOfficeImpot.OID_COSSONAY.getNomCourt());
			map.put(MockOfficeImpot.OID_ECHALLENS.getNoColAdm(), MockOfficeImpot.OID_ECHALLENS.getNomCourt());
			map.put(MockOfficeImpot.OID_GRANDSON.getNoColAdm(), MockOfficeImpot.OID_GRANDSON.getNomCourt());
			map.put(MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), MockOfficeImpot.OID_LAUSANNE_OUEST.getNomCourt());
			map.put(MockOfficeImpot.OID_LA_VALLEE.getNoColAdm(), MockOfficeImpot.OID_LA_VALLEE.getNomCourt());
			map.put(MockOfficeImpot.OID_LAVAUX.getNoColAdm(), MockOfficeImpot.OID_LAVAUX.getNomCourt());
			map.put(MockOfficeImpot.OID_MORGES.getNoColAdm(), MockOfficeImpot.OID_MORGES.getNomCourt());
			map.put(MockOfficeImpot.OID_MOUDON.getNoColAdm(), MockOfficeImpot.OID_MOUDON.getNomCourt());
			map.put(MockOfficeImpot.OID_NYON.getNoColAdm(), MockOfficeImpot.OID_NYON.getNomCourt());
			map.put(MockOfficeImpot.OID_ORBE.getNoColAdm(), MockOfficeImpot.OID_ORBE.getNomCourt());
			map.put(MockOfficeImpot.OID_ORON.getNoColAdm(), MockOfficeImpot.OID_ORON.getNomCourt());
			map.put(MockOfficeImpot.OID_PAYERNE.getNoColAdm(), MockOfficeImpot.OID_PAYERNE.getNomCourt());
			map.put(MockOfficeImpot.OID_PAYS_D_ENHAUT.getNoColAdm(), MockOfficeImpot.OID_PAYS_D_ENHAUT.getNomCourt());
			map.put(MockOfficeImpot.OID_ROLLE_AUBONNE.getNoColAdm(), MockOfficeImpot.OID_ROLLE_AUBONNE.getNomCourt());
			map.put(MockOfficeImpot.OID_VEVEY.getNoColAdm(), MockOfficeImpot.OID_VEVEY.getNomCourt());
			map.put(MockOfficeImpot.OID_YVERDON.getNoColAdm(), MockOfficeImpot.OID_YVERDON.getNomCourt());
			map.put(MockOfficeImpot.OID_LAUSANNE_VILLE.getNoColAdm(), MockOfficeImpot.OID_LAUSANNE_VILLE.getNomCourt());
			map.put(MockOfficeImpot.OID_PM.getNoColAdm(), MockOfficeImpot.OID_PM.getNomCourt());
			map.put(MockOfficeImpot.OID_ST_CROIX.getNoColAdm(), MockOfficeImpot.OID_ST_CROIX.getNomCourt());
		}

		final Map<Integer, String> treeMap = new TreeMap<Integer, String>(new ValueComparator<String>(map));
		treeMap.putAll(map);
		mapOID = Collections.unmodifiableMap(treeMap);

		return mapOID;
	}

	/**
	 * Compare the keys of a map (to be used in a TreeMap, for instance) according to
	 * the order of the value associated with each key
	 * @param <T>
	 */
	private static class ValueComparator<T extends Comparable<T>> implements Comparator<Integer>, Serializable
	{
		private static final long serialVersionUID = -8041248573478594844L;

		private final Map<Integer, T> map;

		public ValueComparator(Map<Integer, T> map) {
			this.map = map;
		}

		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Integer o1, Integer o2) {
			final T value1 = map.get(o1);
			final T value2 = map.get(o2);
			final int valueComparison = compareNullFirst(value1, value2);
			if (valueComparison != 0) {
				return valueComparison;
			}
			return compareNullFirst(o1, o2);
		}

		private static <C extends Comparable<C>> int compareNullFirst(C c1, C c2) {
			return c1 == c2 ? 0 : (c1 == null ? -1 : (c2 == null ? 1 : c1.compareTo(c2)));
		}
	}
}
