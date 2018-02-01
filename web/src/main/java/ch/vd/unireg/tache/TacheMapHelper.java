package ch.vd.unireg.tache;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeUtilisateur;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.common.ApplicationConfig;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CommonMapHelper;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.SecurityDebugConfig;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

public class TacheMapHelper extends CommonMapHelper {

	private Map<Integer, String> mapPeriodeFiscale;
	private Map<TypeEtatTache, String> mapEtatTache;
	private Map<TypeTache, String> mapTypeTache;

	private PeriodeFiscaleDAO periodeFiscaleDAO;
    private ServiceSecuriteService serviceSecurite;
	private PlatformTransactionManager transactionManager;
	private TacheService tacheService;
	private ServiceInfrastructureService infraService;

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * Initialise la map des periodes fiscales
	 * @return une map
	 */
	public Map<Integer, String> initMapPeriodeFiscale() {
		if (mapPeriodeFiscale == null) {

			TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);

			final Map<Integer, String> map = new TreeMap<>();

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

		final Map<Integer, String> map = new HashMap<>();
		if (!SecurityDebugConfig.isIfoSecDebug()) {
			final List<CollectiviteAdministrativeUtilisateur> collectivites = serviceSecurite.getCollectivitesUtilisateur(AuthenticationHelper.getCurrentPrincipal());
			if (collectivites != null) {
				for (CollectiviteAdministrative collectivite : collectivites) {
					map.put(collectivite.getNoColAdm(), collectivite.getNomCourt());
				}
			}
		}
		else {
			map.put(MockCollectiviteAdministrative.ACI.getNoColAdm(), MockCollectiviteAdministrative.ACI.getNomCourt());
			map.put(MockCollectiviteAdministrative.ACI_SECTION_DE_TAXATION.getNoColAdm(), MockCollectiviteAdministrative.ACI_SECTION_DE_TAXATION.getNomCourt());
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

		return sortMapCollectivitesAdministratives(map);
	}

	/**
	 * @return une map, triée par nom court, des mappings entre numéro de collectivité et nom court, des collectivités administratives sur lesquelles on a des tâches
	 */
	public Map<Integer, String> initMapCollectivitesAvecTaches() {
		final Set<Integer> cols = tacheService.getCollectivitesAdministrativesAvecTaches();
		final Map<Integer, String> unsortedMap = cols.stream()
				.map(infraService::getCollectivite)
				.collect(Collectors.toMap(CollectiviteAdministrative::getNoColAdm,
				                          CollectiviteAdministrative::getNomCourt));
		return sortMapCollectivitesAdministratives(unsortedMap);
	}

	private static Map<Integer, String> sortMapCollectivitesAdministratives(Map<Integer, String> unsortedMap) {
		// récupération des libellés -> si plusieurs sont identiques, il faut les différencier
		final Map<String, List<Integer>> libellesUtilises = unsortedMap.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getValue,
				                          entry -> Collections.singletonList(entry.getKey()),
				                          (id1, id2) -> Stream.concat(id1.stream(), id2.stream()).collect(Collectors.toList())));

		final Map<Integer, String> distinctMap = new HashMap<>(unsortedMap.size());
		for (Map.Entry<String, List<Integer>> entry : libellesUtilises.entrySet()) {
			final List<Integer> noCollectivites = entry.getValue();
			final String libelle = entry.getKey();
			if (noCollectivites.size() > 1) {
				noCollectivites.forEach(noCol -> distinctMap.put(noCol, String.format("%s (%d)", libelle, noCol)));
			}
			else {
				final Integer noCol = noCollectivites.get(0);
				distinctMap.put(noCol, libelle);
			}
		}

		final Map<Integer, String> sorted = distinctMap.entrySet().stream()
				.sorted(Comparator.comparing(Map.Entry::getValue))
				.collect(Collectors.toMap(Map.Entry::getKey,
				                          Map.Entry::getValue,
				                          (s1, s2) -> { throw new IllegalArgumentException("Plusieurs collectivités administratives avec le même numéro ?"); },
				                          LinkedHashMap::new));
		return Collections.unmodifiableMap(sorted);
	}
}
