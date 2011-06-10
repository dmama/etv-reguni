package ch.vd.uniregctb.declaration.ordinaire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.IdentifiantDeclaration;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * Processeur qui permet de faire passer les déclarations d'impôt ordinaires sommées à l'état <i>ECHUES</i> lorsque le délai de retour est
 * dépassé.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 * @see la spécification "Etablir la liste des sommations DI échues" (SCU-EtablirListeSommationsDIEchues.doc)
 */
public class EchoirDIsProcessor {

	private static final Logger LOGGER = Logger.getLogger(EchoirDIsProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final DelaisService delaisService;
	private final DeclarationImpotService diService;
	private final PlatformTransactionManager transactionManager;

	public EchoirDIsProcessor(HibernateTemplate hibernateTemplate, DelaisService delaisService, DeclarationImpotService diService, PlatformTransactionManager transactionManager) {
		this.hibernateTemplate = hibernateTemplate;
		this.delaisService = delaisService;
		this.diService = diService;
		this.transactionManager = transactionManager;
	}

	public EchoirDIsResults run(final RegDate dateTraitement, StatusManager s) throws DeclarationException {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Récupération des déclarations d'impôt...");

		final EchoirDIsResults rapportFinal = new EchoirDIsResults(dateTraitement);
		final List<IdentifiantDeclaration> dis = retrieveListDIsSommeesCandidates(dateTraitement);

		status.setMessage("Analyse des déclarations d'impôt...");

		final BatchTransactionTemplate<IdentifiantDeclaration, EchoirDIsResults> template = new BatchTransactionTemplate<IdentifiantDeclaration, EchoirDIsResults>(dis, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<IdentifiantDeclaration, EchoirDIsResults>() {

			@Override
			public EchoirDIsResults createSubRapport() {
				return new EchoirDIsResults(dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<IdentifiantDeclaration> batch, EchoirDIsResults r) throws Exception {

				status.setMessage(String.format("Déclarations d'impôt analysées : %d/%d", rapportFinal.nbDIsTotal, dis.size()), percent);

				traiterBatch(batch, r, status);
				return !status.interrupted();
			}
		});

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traite tout le batch des déclarations, une par une.
	 *
	 * @param batch le batch des déclarations à traiter
	 * @param rapport le rapport à remplir, voir {@link EchoirDIsProcessor#traiterDI(Long, EchoirDIsResults)}.
	 * @param statusManager utilisé pour tester l'interruption
	 */
	private void traiterBatch(List<IdentifiantDeclaration> batch, EchoirDIsResults rapport, StatusManager statusManager) {
		for (IdentifiantDeclaration id : batch) {
			traiterDI(id, rapport);
			if (statusManager.interrupted()) {
				break;
			}
		}
	}

	/**
	 * Traite une déclaration d'impôt ordinaire. C'est-à-dire vérifier qu'elle est dans l'état sommée et que le délai de retour est dépassé;
	 * puis si c'est bien le cas, la faire passer à l'état échu.
	 *
	 * @param id l'id de la déclaration à traiter
	 * @param rapport rapport à remplir
	 */
	protected void traiterDI(IdentifiantDeclaration ident, EchoirDIsResults rapport) {

		Assert.notNull(ident, "L'id doit être spécifié.");

		final DeclarationImpotOrdinaire di = hibernateTemplate.get(DeclarationImpotOrdinaire.class, ident.getIdDeclaration());
		Assert.notNull(di, "La déclaration n'existe pas.");

		final EtatDeclaration etat = di.getDernierEtat();
		Assert.notNull(etat, "La déclaration ne possède pas d'état.");

		// Vérifie l'état de la DI (en cas de bug)
		if (etat.getEtat() != TypeEtatDeclaration.SOMMEE) {
			rapport.addErrorEtatIncoherent(di, String.format("Etat attendu=%s, état constaté=%s. Erreur dans la requête SQL ?", TypeEtatDeclaration.SOMMEE, etat.getEtat()));
			return;
		}

		// On fait passer la DI à l'état échu
		diService.echoirDI(di, rapport.dateTraitement);
		rapport.addDeclarationTraitee(di);

		// un peu de paranoïa ne fait pas de mal
		Assert.isTrue(di.getDernierEtat().getEtat() == TypeEtatDeclaration.ECHUE, "L'état après traitement n'est pas ECHUE.");
	}

	/**
	 * @return les ids des DIs dont l'état courant est <i>sommée</i> et qui dont le délai de retour de sommation est maintenant dépassé.
	 * @param dateTraitement date de référence pour la détermination du dépassement du délai de retour
	 */
	private List<IdentifiantDeclaration> retrieveListDIsSommeesCandidates(final RegDate dateTraitement) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final StringBuilder b = new StringBuilder();
		b.append("SELECT DI.ID, ES.DATE_OBTENTION, DI.TIERS_ID, T.OID FROM DECLARATION DI");
		b.append(" JOIN ETAT_DECLARATION ES ON ES.DECLARATION_ID = DI.ID AND ES.ANNULATION_DATE IS NULL AND ES.TYPE='SOMMEE'");
		b.append(" JOIN TIERS T ON T.NUMERO = DI.TIERS_ID ");
		b.append(" WHERE DI.DOCUMENT_TYPE='DI' AND DI.ANNULATION_DATE IS NULL");
		b.append(" AND NOT EXISTS (SELECT 1 FROM ETAT_DECLARATION ED WHERE ED.DECLARATION_ID = DI.ID AND ED.ANNULATION_DATE IS NULL AND ED.TYPE IN ('RETOURNEE', 'ECHUE'))");
		b.append(" ORDER BY DI.TIERS_ID");
		final String sql = b.toString();

		return template.execute(new TransactionCallback<List<IdentifiantDeclaration>>() {
			@Override
			public List<IdentifiantDeclaration> doInTransaction(TransactionStatus status) {
				final List<IdentifiantDeclaration> identifiantDi = new ArrayList<IdentifiantDeclaration>();
				return hibernateTemplate.execute(new HibernateCallback<List<IdentifiantDeclaration>>() {
					@Override
					public List<IdentifiantDeclaration> doInHibernate(Session session) throws HibernateException {

						final Query query = session.createSQLQuery(sql);
						final List<Object[]> rows = query.list();
						if (rows != null && !rows.isEmpty()) {
							final List<Long> idDis = new LinkedList<Long>();
							for (Object[] row : rows) {
								final int indexDateSommation = ((Number) row[1]).intValue();
								final RegDate dateSommation = RegDate.fromIndex(indexDateSommation, false);
								final RegDate echeanceReelle = getSeuilEcheanceSommation(dateSommation);
								if (dateTraitement.isAfter(echeanceReelle)) {
									final long diId = ((Number) row[0]).longValue();
									idDis.add(diId);
									final Number numeroDi = ((Number) row[0]).longValue();
									final Number numeroTiers = ((Number) row[2]).longValue();
									final Number numeroOID = (row[3]==null?  0 :((Number) row[3]).intValue());
									final IdentifiantDeclaration identifiantDeclaration = new IdentifiantDeclaration(numeroDi.longValue(), numeroTiers.longValue(),numeroOID.intValue());
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
		final RegDate delaiTemp = delaisService.getDateFinDelaiEcheanceSommationDeclarationImpot(dateSommation); // 30 jours
		return delaisService.getDateFinDelaiEnvoiSommationDeclarationImpot(delaiTemp); // 15 jours
	}
}
