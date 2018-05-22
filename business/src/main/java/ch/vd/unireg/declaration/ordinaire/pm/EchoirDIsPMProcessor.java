package ch.vd.unireg.declaration.ordinaire.pm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.IdentifiantDeclaration;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * Processeur qui permet de faire passer les déclarations d'impôt ordinaires PM sommées à l'état <i>ECHUS</i> lorsque le délai de retour est
 * dépassé.
 */
public class EchoirDIsPMProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EchoirDIsPMProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final DelaisService delaisService;
	private final DeclarationImpotService diService;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final AdresseService adresseService;

	public EchoirDIsPMProcessor(HibernateTemplate hibernateTemplate, DelaisService delaisService, DeclarationImpotService diService, PlatformTransactionManager transactionManager,
	                            TiersService tiersService, AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.delaisService = delaisService;
		this.diService = diService;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	public EchoirDIsPMResults run(final RegDate dateTraitement, StatusManager s) throws DeclarationException {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Récupération des déclarations d'impôt PM...");

		final EchoirDIsPMResults rapportFinal = new EchoirDIsPMResults(dateTraitement, tiersService, adresseService);
		final List<IdentifiantDeclaration> dis = retrieveListDIsSommeesCandidates(dateTraitement);

		status.setMessage("Analyse des déclarations d'impôt PM...");

		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<IdentifiantDeclaration, EchoirDIsPMResults>
				template = new BatchTransactionTemplateWithResults<>(dis, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<IdentifiantDeclaration, EchoirDIsPMResults>() {

			@Override
			public EchoirDIsPMResults createSubRapport() {
				return new EchoirDIsPMResults(dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<IdentifiantDeclaration> batch, EchoirDIsPMResults r) throws Exception {
				status.setMessage(String.format("Déclarations d'impôt analysées : %d/%d", rapportFinal.nbDIsTotal, dis.size()), progressMonitor.getProgressInPercent());
				traiterBatch(batch, r, status);
				return !status.isInterrupted();
			}
		}, progressMonitor);

		rapportFinal.interrompu = status.isInterrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traite tout le batch des déclarations, une par une.
	 *
	 * @param batch le batch des déclarations à traiter
	 * @param rapport le rapport à remplir, voir {@link EchoirDIsPMProcessor#traiterDI(IdentifiantDeclaration, EchoirDIsPMResults)}.
	 * @param statusManager utilisé pour tester l'interruption
	 */
	private void traiterBatch(List<IdentifiantDeclaration> batch, EchoirDIsPMResults rapport, StatusManager statusManager) {
		for (IdentifiantDeclaration id : batch) {
			traiterDI(id, rapport);
			if (statusManager.isInterrupted()) {
				break;
			}
		}
	}

	/**
	 * Traite une déclaration d'impôt ordinaire. C'est-à-dire vérifier qu'elle est dans l'état sommée et que le délai de retour est dépassé;
	 * puis si c'est bien le cas, la faire passer à l'état échu.
	 *
	 * @param ident l'id de la déclaration à traiter
	 * @param rapport rapport à remplir
	 */
	protected void traiterDI(IdentifiantDeclaration ident, EchoirDIsPMResults rapport) {

		if (ident == null) {
			throw new IllegalArgumentException("L'id doit être spécifié.");
		}

		final DeclarationImpotOrdinairePM di = hibernateTemplate.get(DeclarationImpotOrdinairePM.class, ident.getIdDeclaration());
		if (di == null) {
			throw new IllegalArgumentException("La déclaration n'existe pas.");
		}

		final EtatDeclaration etat = di.getDernierEtatDeclaration();
		if (etat == null) {
			throw new IllegalArgumentException("La déclaration ne possède pas d'état.");
		}

		if (etat.getEtat() == TypeEtatDocumentFiscal.SUSPENDU) {
			rapport.addDISuspendueIgnoree(di);
			return;
		}

		// Vérifie l'état de la DI (en cas de bug)
		if (etat.getEtat() != TypeEtatDocumentFiscal.SOMME) {
			rapport.addErrorEtatIncoherent(di, String.format("Etat attendu=%s, état constaté=%s. Erreur dans la requête SQL ?", TypeEtatDocumentFiscal.SOMME, etat.getEtat()));
			return;
		}

		// vérification de la présence d'un éventuel sursis accordé
		final DelaiDeclaration dernierDelai = di.getDernierDelaiDeclarationAccorde();
		if (dernierDelai != null) {
			final RegDate delaiEffectif = getSeuilEcheanceApresDelaiOfficiel(dernierDelai.getDelaiAccordeAu());
			if (delaiEffectif.isAfterOrEqual(rapport.dateTraitement)) {
				rapport.addDIAvecSursisAccordeIgnoree(di);
				return;
			}
		}

		// On fait passer la DI à l'état échu
		diService.echoirDI(di, rapport.dateTraitement);
		rapport.addDeclarationTraitee(di);

		// un peu de paranoïa ne fait pas de mal
		if (di.getDernierEtatDeclaration().getEtat() != TypeEtatDocumentFiscal.ECHU) {
			throw new IllegalArgumentException("L'état après traitement n'est pas ECHU.");
		}
	}

	/**
	 * @return les ids des DIs dont l'état courant est <i>sommée</i> et qui dont le délai de retour de sommation est maintenant dépassé.
	 * @param dateTraitement date de référence pour la détermination du dépassement du délai de retour
	 */
	private List<IdentifiantDeclaration> retrieveListDIsSommeesCandidates(final RegDate dateTraitement) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final StringBuilder b = new StringBuilder();
		b.append("SELECT DI.ID, ES.DATE_OBTENTION, DI.TIERS_ID, T.OID FROM DOCUMENT_FISCAL DI");
		b.append(" JOIN ETAT_DOCUMENT_FISCAL ES ON ES.DOCUMENT_FISCAL_ID = DI.ID AND ES.ANNULATION_DATE IS NULL AND ES.TYPE='SOMME'");
		b.append(" JOIN TIERS T ON T.NUMERO = DI.TIERS_ID ");
		b.append(" WHERE DI.DOCUMENT_TYPE='DIPM' AND DI.ANNULATION_DATE IS NULL");
		b.append(" AND NOT EXISTS (SELECT 1 FROM ETAT_DOCUMENT_FISCAL ED WHERE ED.DOCUMENT_FISCAL_ID = DI.ID AND ED.ANNULATION_DATE IS NULL AND ED.TYPE IN ('RETOURNE', 'ECHU'))");
		b.append(" ORDER BY DI.TIERS_ID");
		final String sql = b.toString();

		return template.execute(new TransactionCallback<List<IdentifiantDeclaration>>() {
			@Override
			public List<IdentifiantDeclaration> doInTransaction(TransactionStatus status) {
				final List<IdentifiantDeclaration> identifiantDi = new ArrayList<>();
				return hibernateTemplate.execute(new HibernateCallback<List<IdentifiantDeclaration>>() {
					@Override
					public List<IdentifiantDeclaration> doInHibernate(Session session) throws HibernateException {

						final Query query = session.createSQLQuery(sql);
						//noinspection unchecked
						final List<Object[]> rows = query.list();
						if (rows != null && !rows.isEmpty()) {
							for (Object[] row : rows) {
								final int indexDateSommation = ((Number) row[1]).intValue();
								final RegDate dateSommation = RegDate.fromIndex(indexDateSommation, false);
								final RegDate echeanceReelle = getSeuilEcheanceSommation(dateSommation);
								if (dateTraitement.isAfter(echeanceReelle)) {
									final long diId = ((Number) row[0]).longValue();
									final long numeroTiers = ((Number) row[2]).longValue();
									final Integer numeroOID = row[3] == null ? null : ((Number) row[3]).intValue();
									final IdentifiantDeclaration identifiantDeclaration = new IdentifiantDeclaration(diId, numeroTiers, numeroOID);
									identifiantDi.add(identifiantDeclaration);
								}
							}
							return identifiantDi;
						}
						else {
							return Collections.emptyList();
						}
					}
				});
			}
		});
	}

	private RegDate getSeuilEcheanceSommation(RegDate dateSommation) {
		// [UNIREG-1468] L'échéance de sommation = date sommation + 30 jours (délai normal) + 15 jours (délai administratif)
		final RegDate delaiSommation = delaisService.getDateFinDelaiEcheanceSommationDeclarationImpotPM(dateSommation); // 30 jours
		return getSeuilEcheanceApresDelaiOfficiel(delaiSommation);
	}

	private RegDate getSeuilEcheanceApresDelaiOfficiel(RegDate delaiOfficiel) {
		return delaisService.getDateFinDelaiEnvoiSommationDeclarationImpotPM(delaiOfficiel); // +15 jours
	}
}
