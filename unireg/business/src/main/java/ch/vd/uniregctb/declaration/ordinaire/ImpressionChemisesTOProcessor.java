package ch.vd.uniregctb.declaration.ordinaire;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * Processeur pour l'impression en masse des chemises TO
 */
public class ImpressionChemisesTOProcessor {

	private final Logger LOGGER = Logger.getLogger(ImpressionChemisesTOProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final DeclarationImpotOrdinaireDAO diDAO;
	private final PlatformTransactionManager transactionManager;
	private final EditiqueCompositionService editiqueService;
	private final ServiceInfrastructureService infraService;

	public ImpressionChemisesTOProcessor(HibernateTemplate hibernateTemplate, DeclarationImpotOrdinaireDAO diDAO,
			PlatformTransactionManager transactionManager, EditiqueCompositionService editiqueService, ServiceInfrastructureService infraService) {
		this.hibernateTemplate = hibernateTemplate;
		this.diDAO = diDAO;
		this.transactionManager = transactionManager;
		this.editiqueService = editiqueService;
		this.infraService = infraService;
	}

	@SuppressWarnings("unchecked")
	public ImpressionChemisesTOResults run(int nombreMax, final Integer noColOid, StatusManager status) {

		// première transaction pour récupérer les identifiants des DI candidates à la chemise TO
		final int nbMaxTO = nombreMax > 0 ? nombreMax : 0;

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> ids = (List<Long>) template.execute(new TransactionCallback() {
			public List<Long> doInTransaction(TransactionStatus status) {
				return (List<Long>) hibernateTemplate.executeWithNativeSession(new HibernateCallback() {
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
						return getIdDesDIPourTO(session);
					}
				});
			}
		});

		// récupère les numéros Ofs des communes gérées par l'office d'impôt spécifié
		final Set<Integer> nosOfsCommunes = new HashSet<Integer>();
		final String nomOid;
		if (noColOid != null) {
			final List<Commune> communes = infraService.getListeCommunesByOID(noColOid);
			for (Commune c : communes) {
				nosOfsCommunes.add(c.getNoOFSEtendu());
			}

			final OfficeImpot oid = infraService.getOfficeImpot(noColOid);
			if (oid != null) {
				nomOid = oid.getNomCourt();
			}
			else {
				nomOid = noColOid.toString();
			}
		}
		else {
			nomOid = null;
		}


		final StatusManager s = status != null ? status : new LoggingStatusManager(LOGGER);

		// transactions suivantes pour faire le boulot de l'impression des chemises TO
		final ImpressionChemisesTOResults resultsFinaux = new ImpressionChemisesTOResults(nbMaxTO, nomOid);
		final BatchTransactionTemplate<Long, ImpressionChemisesTOResults> batchTemplate = new BatchTransactionTemplate<Long, ImpressionChemisesTOResults>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		batchTemplate.execute(resultsFinaux, new BatchCallback<Long, ImpressionChemisesTOResults>() {

			private ImpressionChemisesTOResults results;

			@Override
			public ImpressionChemisesTOResults createSubRapport() {
				return new ImpressionChemisesTOResults(nbMaxTO, nomOid);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, ImpressionChemisesTOResults r) throws Exception {

				results = r;

				boolean plafondAtteint = false;
				for (int i = 0; i < batch.size() && !s.interrupted(); ++i) {
					final Long id = batch.get(i);
					final DeclarationImpotOrdinaire di = diDAO.get(id);
					final EtatDeclaration etat = di.getDernierEtat();
					if (etat != null && etat.getEtat() == TypeEtatDeclaration.ECHUE && !di.getTiers().isAnnule()) {

						if (noColOid == null || nosOfsCommunes.contains(di.getNumeroOfsForGestion())) {
							editiqueService.imprimeTaxationOfficeBatch(di);
							di.setDateImpressionChemiseTaxationOffice(DateHelper.getCurrentDate());
							results.addChemiseTO(di);

							// on n'imprime pas plus de chemises que demandé
							if (nbMaxTO > 0 && resultsFinaux.getNbChemisesImprimees() + i + 1 >= nbMaxTO) {
								plafondAtteint = true;
								break;
							}
						}
					}
				}

				return !s.interrupted() && !plafondAtteint;
			}

			@Override
			public void afterTransactionCommit() {
				s.setMessage(String.format("%d chemises TO imprimées", resultsFinaux.getNbChemisesImprimees()));
			}
		});

		if (s.interrupted()) {
			s.setMessage("L'impression des chemises de taxation d'office a été interrompue. Nombre de chemises imprimées au moment de l'interruption = "
					+ resultsFinaux.getNbChemisesImprimees());
			resultsFinaux.setInterrompu();
		}
		else {
			s.setMessage("L'impression des chemises de taxation d'office est terminé. Nombre de chemises imprimées = "
					+ resultsFinaux.getNbChemisesImprimees() + ". Nombre d'erreurs = " + resultsFinaux.getErreurs().size());
		}

		resultsFinaux.end();
		return resultsFinaux;
	}

	@SuppressWarnings("unchecked")
	private List<Long> getIdDesDIPourTO(Session session) {
		final String queryString = "select di.id from DeclarationImpotOrdinaire AS di"
				+ " where exists (select ed.id from EtatDeclaration AS ed where ed.declaration.id = di.id and ed.class = EtatDeclarationEchue and ed.annulationDate is null)"
				+ " and not exists (select ed.id from EtatDeclaration AS ed where ed.declaration.id = di.id and ed.class = EtatDeclarationRetournee and ed.annulationDate is null)"
				+ " and di.annulationDate is null and di.dateImpressionChemiseTaxationOffice is null order by di.tiers.numero asc";
		final Query query = session.createQuery(queryString);
		return query.list();
	}
}
