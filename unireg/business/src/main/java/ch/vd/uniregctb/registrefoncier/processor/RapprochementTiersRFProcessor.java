package ch.vd.uniregctb.registrefoncier.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresEntreprise;
import ch.vd.uniregctb.evenement.identification.contribuable.CriteresPersonne;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.TooManyIdentificationPossibilitiesException;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class RapprochementTiersRFProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RapprochementTiersRFProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final AdresseService adresseService;
	private final RapprochementRFDAO rapprochementDAO;
	private final HibernateTemplate hibernateTemplate;
	private final IdentificationContribuableService identificationService;

	private final Map<Class<? extends TiersRF>, Identifier<? extends TiersRF>> identifiers;

	@FunctionalInterface
	private interface Identifier<T extends TiersRF> {
		List<Long> identify(T tiersRF, IdentificationContribuableService identificationService) throws TooManyIdentificationPossibilitiesException;
	}

	public RapprochementTiersRFProcessor(PlatformTransactionManager transactionManager, TiersService tiersService, AdresseService adresseService, RapprochementRFDAO rapprochementDAO, HibernateTemplate hibernateTemplate,
	                                     IdentificationContribuableService identificationService) {
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.rapprochementDAO = rapprochementDAO;
		this.hibernateTemplate = hibernateTemplate;
		this.identificationService = identificationService;
		this.identifiers = buildIdentifierMap();
	}

	private static <T extends TiersRF> void register(Map<Class<? extends TiersRF>, Identifier<? extends TiersRF>> map,
	                                                 Class<T> clazz,
	                                                 Identifier<? super T> identifier) {
		map.put(clazz, identifier);
	}

	private static Map<Class<? extends TiersRF>, Identifier<? extends TiersRF>> buildIdentifierMap() {
		final Map<Class<? extends TiersRF>, Identifier<? extends TiersRF>> map = new HashMap<>();
		register(map, PersonnePhysiqueRF.class, new PersonnePhysiqueIdentifier());
		register(map, CollectivitePubliqueRF.class, new NoopIdentifier());
		register(map, PersonneMoraleRF.class, new PersonneMoraleIdentifier());
		return map;
	}

	private <T extends TiersRF> Identifier<? super T> findIdentifier(Class<? extends TiersRF> clazz) {
		//noinspection unchecked
		final Identifier<? super T> identifier = (Identifier<? super T>) identifiers.get(clazz);
		if (identifier == null) {
			throw new IllegalArgumentException("Pas d'identification possible pour un tiers RF de classe " + clazz.getSimpleName());
		}
		return identifier;
	}

	private static final class PersonnePhysiqueIdentifier implements Identifier<PersonnePhysiqueRF> {
		@Override
		public List<Long> identify(PersonnePhysiqueRF tiersRF, IdentificationContribuableService identificationService) throws TooManyIdentificationPossibilitiesException {
			final CriteresPersonne criteres = new CriteresPersonne();
			criteres.setDateNaissance(tiersRF.getDateNaissance());
			criteres.setNom(tiersRF.getNom());
			criteres.setPrenoms(tiersRF.getPrenom());
			return identificationService.identifiePersonnePhysique(criteres, null);
		}
	}

	private static final class NoopIdentifier implements Identifier<TiersRF> {
		@Override
		public List<Long> identify(TiersRF tiersRF, IdentificationContribuableService identificationService) throws TooManyIdentificationPossibilitiesException {
			// jamais rien d'identifié...
			return Collections.emptyList();
		}
	}

	private static final class PersonneMoraleIdentifier implements Identifier<PersonneMoraleRF> {
		@Override
		public List<Long> identify(PersonneMoraleRF tiersRF, IdentificationContribuableService identificationService) throws TooManyIdentificationPossibilitiesException {
			final CriteresEntreprise criteres = new CriteresEntreprise();
			criteres.setRaisonSociale(tiersRF.getRaisonSociale());
			// TODO numéro RC...
			return identificationService.identifieEntreprise(criteres);
		}
	}

	public RapprochementTiersRFResults run(StatusManager status) {
		final StatusManager s = status != null ? status : new LoggingStatusManager(LOGGER);

		// récupération des données à rapprocher...
		s.setMessage("Récupération des tiers RF à identifier...");
		final List<Long> idsTiersRF = getIdsTiersRFSansRapprochement();

		// traitement de ces données
		s.setMessage("Rapprochements en cours...");

		final RapprochementTiersRFResults rapportFinal = new RapprochementTiersRFResults(tiersService, adresseService);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, RapprochementTiersRFResults> template = new BatchTransactionTemplateWithResults<>(idsTiersRF, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, s);
		final boolean nonInterrompu = template.execute(rapportFinal, new BatchWithResultsCallback<Long, RapprochementTiersRFResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, RapprochementTiersRFResults rapport) throws Exception {
				s.setMessage("Rapprochements en cours...", progressMonitor.getProgressInPercent());
				for (Long idTiersRF : batch) {
					final TiersRF tiersRF = hibernateTemplate.get(TiersRF.class, idTiersRF);
					traiterTiersRF(tiersRF, rapport);
					if (s.interrupted()) {
						break;
					}
				}
				return !s.interrupted();
			}

			@Override
			public RapprochementTiersRFResults createSubRapport() {
				return new RapprochementTiersRFResults(tiersService, adresseService);
			}
		}, progressMonitor);

		// fin
		s.setMessage("Rapprochements terminés.");
		rapportFinal.setInterrompu(!nonInterrompu);
		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterTiersRF(TiersRF tiersRF, RapprochementTiersRFResults rapport) {
		List<Long> resultatIdentification;
		try {
			resultatIdentification = identifie(tiersRF);
			if (resultatIdentification.size() == 1) {
				// c'est le bon !!
				creerRapprochement(tiersRF, resultatIdentification.get(0), TypeRapprochementRF.AUTO, rapport);
				return;
			}
			else if (resultatIdentification.size() > 1 && tiersRF.getNoContribuable() != null) {
				final Long idUnireg = resultatIdentification.stream()
						.filter(id -> id.equals(tiersRF.getNoContribuable()))
						.findFirst()
						.orElse(null);
				if (idUnireg != null) {
					creerRapprochement(tiersRF, idUnireg, TypeRapprochementRF.AUTO_MULTIPLE, rapport);
					return;
				}
			}
		}
		catch (TooManyIdentificationPossibilitiesException e) {
			// rien de spécial, pas d'identification positive, c'est tout...
			resultatIdentification = e.getExamplesFound();
		}

		// on n'a rien identifier... il faut faire une demande d'identification manuelle...
		// TODO à faire : création d'une demande d'identification manuelle de contribuable
		rapport.addTiersNonIdentifie(tiersRF, resultatIdentification);
	}

	private void creerRapprochement(TiersRF tiersRF, long idContribuableUnireg, TypeRapprochementRF typeRapprochement, RapprochementTiersRFResults rapport) {
		final Contribuable ctb = hibernateTemplate.get(Contribuable.class, idContribuableUnireg);
		if (ctb == null) {
			rapport.addErrorTiersIdentifiePasContribuable(tiersRF, idContribuableUnireg);
		}
		else {
			final RapprochementRF rapprochement = new RapprochementRF();
			rapprochement.setTiersRF(tiersRF);
			rapprochement.setContribuable(ctb);
			rapprochement.setDateDebut(null);
			rapprochement.setDateFin(null);
			rapprochement.setTypeRapprochement(typeRapprochement);

			final RapprochementRF saved = hibernateTemplate.merge(rapprochement);
			ctb.addRapprochementRF(saved);

			rapport.addNouveauRapprochement(saved);
		}
	}

	private <T extends TiersRF> List<Long> identifie(T tiers) throws TooManyIdentificationPossibilitiesException {
		final Identifier<? super T> identifier = findIdentifier(tiers.getClass());
		return identifier.identify(tiers, identificationService);
	}

	private List<Long> getIdsTiersRFSansRapprochement() {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(status -> rapprochementDAO.findTiersRFSansRapprochement(RegDate.get()).stream()
				.map(TiersRF::getId)
				.collect(Collectors.toCollection(LinkedList::new)));
	}
}
