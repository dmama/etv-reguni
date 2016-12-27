package ch.vd.uniregctb.role;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class RoleServiceImpl implements RoleService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RoleServiceImpl.class);

	private static final int BATCH_SIZE = 100;

	private RoleHelper roleHelper;
	private PlatformTransactionManager transactionManager;
	private TiersService tiersService;
	private ServiceInfrastructureService infraService;
	private AdresseService adresseService;

	public void setRoleHelper(RoleHelper roleHelper) {
		this.roleHelper = roleHelper;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	/**
	 * Coeur du calcul
	 * @param annee année du rôle à générer
	 * @param nbThreads nombre de threads de l'extraction
	 * @param ofsCommune si présent et non-vide, sous-ensemble des communes vaudoises à traiter (si <code>null</code> ou vide, on prendra toutes les communes vaudoises)
	 * @param variante implémentation des variantes de calcul
	 * @param statusManager status manager
	 * @param <T> type des contribuables concernés
	 * @param <R> type du rapport final
	 * @return le rapport final généré
	 */
	private <T extends Contribuable, R extends RoleResults<R>> R produireRole(int annee,
	                                                                          int nbThreads,
	                                                                          @Nullable Set<Integer> ofsCommune,
	                                                                          VarianteCalculRole<T, R> variante,
	                                                                          @Nullable StatusManager statusManager) {

		final StatusManager status = Optional.ofNullable(statusManager)
				.orElseGet(() -> new LoggingStatusManager(LOGGER));

		// recherche des contribuables concernés
		status.setMessage("Récupération des contribuables concernés...");
		final List<Long> idsContribuables = variante.getIdsContribuables(annee, ofsCommune);

		// génération du rapport final
		final R rapportFinal = variante.buildRapport();

		// traitement des contribuables par petits groupes
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final ParallelBatchTransactionTemplateWithResults<Long, R> template = new ParallelBatchTransactionTemplateWithResults<>(idsContribuables, BATCH_SIZE, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, AuthenticationInterface.INSTANCE);
		template.setReadonly(true);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, R>() {
			@Override
			public boolean doInTransaction(List<Long> batch, R rapport) throws Exception {
				status.setMessage("Extraction en cours...", progressMonitor.getProgressInPercent());

				final List<T> contribuables = batch.stream()
						.map(tiersService::getTiers)
						.map(tiers -> (T) tiers)
						.filter(Objects::nonNull)
						.collect(Collectors.toList());

				final Map<Integer, List<T>> map = variante.dispatch(annee, contribuables);
				final Stream<Map.Entry<Integer, List<T>>> entries;
				if (ofsCommune == null || ofsCommune.isEmpty()) {
					// on veut tout
					entries = map.entrySet().stream();
				}
				else {
					// on ne veut que la commune donnée, tous les autres devant être indiqués comme "ignorés" (-> key = null)
					entries = map.entrySet().stream()
							.map(entry -> Pair.of(ofsCommune.contains(entry.getKey()) ? entry.getKey() : null, entry.getValue()));
				}

				// traitement pour le rapport
				entries.map(entry -> entry.getValue().stream().map(ctb -> Pair.of(entry.getKey(), ctb)))
						.flatMap(Function.identity())
						.forEach(pair -> variante.compile(rapport, pair.getRight(), pair.getLeft()));

				return !status.interrupted();
			}

			@Override
			public R createSubRapport() {
				return variante.buildRapport();
			}
		}, progressMonitor);

		rapportFinal.setInterrupted(status.interrupted());
		rapportFinal.end();
		status.setMessage("Extraction terminée.");
		return rapportFinal;
	}

	/**
	 * Extraction du rôle communal des personnes physiques
	 * @param annee année du rôle
	 * @param nbThreads degré de parallélisation de l'extraction
	 * @param ofsCommune [optionel] numéro OFS de la commune spécifique pour laquelle est tiré le rôle
	 * @param statusManager status manager
	 * @return les données extraites
	 */
	public RolePPCommunesResults produireRolePPCommunes(int annee, int nbThreads, @Nullable Integer ofsCommune, @Nullable StatusManager statusManager) {
		return produireRole(annee,
		                    nbThreads,
		                    ofsCommune == null ? null : Collections.singleton(ofsCommune),
		                    new VarianteCalculRolePPCommunes(roleHelper, () -> new RolePPCommunesResults(annee, nbThreads, ofsCommune, adresseService, infraService, tiersService)),
		                    statusManager);
	}

	/**
	 * Extraction du rôle communal (par office d'impôt) des personnes physiques
	 * @param annee année du rôle
	 * @param nbThreads degré de parallélisation de l'extraction
	 * @param oid [optionel] numéro de collectivité administrative de l'OID concerné
	 * @param statusManager status manager
	 * @return les données extraites
	 */
	public RolePPOfficesResults produireRolePPOffices(int annee, int nbThreads, @Nullable final Integer oid, @Nullable StatusManager statusManager) {

		// on va chercher les associations des communes avec leur OID
		final List<OfficeImpot> oids = infraService.getOfficesImpot();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		final Map<Integer, Integer> officeParCommune = template.execute(new TransactionCallback<Map<Integer, Integer>>() {
			@Override
			public Map<Integer, Integer> doInTransaction(TransactionStatus status) {
				return oids.stream()
						.filter(OfficeImpot::isValide)
						.filter(OfficeImpot::isOID)
						.map(OfficeImpot::getNoColAdm)
						.filter(colAdmId -> oid == null || oid.equals(colAdmId))
						.map(colAdmId -> infraService.getListeCommunesByOID(colAdmId).stream().map(commune -> Pair.of(commune.getNoOFS(), colAdmId)))
						.flatMap(Function.identity())
						.distinct()
						.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
			}
		});

		// quelles sont les communes intéressantes, au final ?
		final Set<Integer> ofsCommunes;
		if (oid == null) {
			ofsCommunes = null;
		}
		else {
			ofsCommunes = officeParCommune.entrySet().stream()
					.filter(entry -> oid.equals(entry.getValue()))
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
		}

		return produireRole(annee,
		                    nbThreads,
		                    ofsCommunes,
		                    new VarianteCalculRolePPOffices(roleHelper, officeParCommune, () -> new RolePPOfficesResults(annee, nbThreads, oid, adresseService, infraService, tiersService)),
		                    statusManager);
	}

	/**
	 * Extraction du rôle communal des personnes morales
	 * @param annee année du rôle
	 * @param nbThreads degré de parallélisation de l'extraction
	 * @param ofsCommune [optionel] numéro OFS de la commune spécifique pour laquelle est tiré le rôle
	 * @param statusManager status manager
	 * @return les données extraites
	 */
	public RolePMCommunesResults produireRolePMCommunes(int annee, int nbThreads, @Nullable Integer ofsCommune, @Nullable StatusManager statusManager) {
		return produireRole(annee,
		                    nbThreads,
		                    ofsCommune == null ? null : Collections.singleton(ofsCommune),
		                    new VarianteCalculRolePMCommunes(roleHelper, () -> new RolePMCommunesResults(annee, nbThreads, ofsCommune, adresseService, infraService, tiersService)),
		                    statusManager);
	}

	/**
	 * Extraction du rôle complet des personnes morales (une seule liste pour l'OIPM)
	 * @param annee année du rôle
	 * @param nbThreads degré de parallélisation de l'extraction
	 * @param statusManager status manager
	 * @return les données extraites
	 */
	public RolePMOfficeResults produireRolePMOffice(int annee, int nbThreads, @Nullable StatusManager statusManager) {
		return produireRole(annee,
		                    nbThreads,
		                    null,
	                        new VarianteCalculRolePMOffice(roleHelper, () -> new RolePMOfficeResults(annee, nbThreads, adresseService, infraService, tiersService)),
		                    statusManager);
	}
}
