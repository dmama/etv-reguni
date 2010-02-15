package ch.vd.uniregctb.declaration.ordinaire;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
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

	private final int BATCH_SIZE = 100;

	private final HibernateTemplate hibernateTemplate;
	private final DelaisService delaisService;
	private final DeclarationImpotService diService;
	private final PlatformTransactionManager transactionManager;

	private EchoirDIsResults rapport;

	public EchoirDIsProcessor(HibernateTemplate hibernateTemplate, DelaisService delaisService, DeclarationImpotService diService,
			PlatformTransactionManager transactionManager) {
		this.hibernateTemplate = hibernateTemplate;
		this.delaisService = delaisService;
		this.diService = diService;
		this.transactionManager = transactionManager;
	}

	public EchoirDIsResults run(final RegDate dateTraitement, StatusManager s) throws DeclarationException {

		final StatusManager status = (s != null ? s : new LoggingStatusManager(LOGGER));

		status.setMessage("Récupération des déclarations d'impôt...");

		final List<Long> dis = retrieveListDIsSommees();

		status.setMessage("Début du traitement des déclarations d'impôt...");

		final EchoirDIsResults rapportFinal = new EchoirDIsResults(dateTraitement);

		final BatchTransactionTemplate<Long> template = new BatchTransactionTemplate<Long>(dis, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, status, hibernateTemplate);
		template.execute(new BatchCallback<Long>() {

			private Long idDI = null;

			@Override
			public void beforeTransaction() {
				rapport = new EchoirDIsResults(dateTraitement);
				idDI = null;
			}

			@Override
			public boolean doInTransaction(List<Long> batch) throws Exception {

				status.setMessage("Traitement du batch [" + batch.get(0) + "; " + batch.get(batch.size() - 1) + "] ...", percent);

				if (batch.size() == 1) {
					idDI = batch.get(0);
				}
				traiterBatch(batch, dateTraitement);
				return true;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (willRetry) {
					// le batch va être rejoué -> on peut ignorer le rapport
					rapport = null;
				}
				else {
					// on ajoute l'exception directement dans le rapport final
					rapportFinal.addErrorException(idDI, e);
					rapport = null;
				}
			}

			@Override
			public void afterTransactionCommit() {
				rapportFinal.add(rapport);
			}
		});

		rapportFinal.interrompu = status.interrupted();
		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traite tout le batch des déclarations, une par une.
	 *
	 * @param batch
	 *            le batch des déclarations à traiter
	 * @param dateTraitement
	 *            la date de traitemet. Voir {@link EchoirDIsProcessor#traiterDI(Long, RegDate)}.
	 */
	private void traiterBatch(List<Long> batch, RegDate dateTraitement) {
		for (Long id : batch) {
			traiterDI(id, dateTraitement);
		}
	}

	/**
	 * Traite une déclaration d'impôt ordinaire. C'est-à-dire vérifier qu'elle est dans l'état sommée et que le délai de retour est dépassé;
	 * puis si c'est bien le cas, la faire passer à l'état échu.
	 *
	 * @param id
	 *            l'id de la déclaration à traiter
	 * @param dateTraitement
	 *            la date de traitement pour vérifier le dépassement du délai de retour, et - le cas échéant - pour définir la date
	 *            d'obtention de l'état échu.
	 */
	protected void traiterDI(Long id, RegDate dateTraitement) {

		Assert.notNull(id, "L'id doit être spécifié.");

		final DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) hibernateTemplate.get(DeclarationImpotOrdinaire.class, id);
		Assert.notNull(di, "La déclaration n'existe pas.");

		final EtatDeclaration etat = di.getDernierEtat();
		Assert.notNull(etat, "La déclaration ne possède pas d'état.");

		// Vérifie l'état de la DI
		if (etat.getEtat() != TypeEtatDeclaration.SOMMEE) {
			if (estSommeeEtRetourneeLeMemeJour(di)) {
				// dans ce cas, la requête SQL ne distingue pas lequel des deux états est le dernier. On peut recevoir des DIs retournées
				// (faux positifs), que l'on ignore silencieusement.
			}
			else {
				rapport.addErrorEtatIncoherent(di, "Etat attendu=" + TypeEtatDeclaration.SOMMEE + ", état constaté=" + etat.getEtat()
						+ ". Erreur dans la requête SQL ?");
			}
			return;
		}

		// [UNIREG-1468] L'échéance de sommation = date sommation + 30 jours (délai normal) + 15 jours (délai administratif)
		final RegDate dateSommation = etat.getDateObtention();
		final RegDate delaiTemp = delaisService.getDateFinDelaiEcheanceSommationDeclarationImpot(dateSommation); // 30 jours
		final RegDate delaiFinal = delaisService.getDateFinDelaiEnvoiSommationDeclarationImpot(delaiTemp); // 15 jours

		// Vérifie que le délai initial (+ le délai administratif) est bien dépassé
		if (delaiFinal.isAfterOrEqual(dateTraitement)) {
			rapport.addIgnoreDelaiNonEchu(di, "Date de traitement=" + RegDateHelper.dateToDisplayString(dateTraitement)
					+ ", délai calculé=" + RegDateHelper.dateToDisplayString(delaiFinal));
			return;
		}

		// On fait passer la DI à l'état échu
		diService.echoirDI(di, dateTraitement);
		rapport.addDeclarationTraitee(di);
		Assert.isTrue(di.getDernierEtat().getEtat() == TypeEtatDeclaration.ECHUE, "L'état après traitement n'est pas ECHUE.");
	}

	/**
	 * @return <b>vrai</b> si la déclaration a été sommée et retournée le même jour; <b>faux</b> autrement.
	 */
	private boolean estSommeeEtRetourneeLeMemeJour(final DeclarationImpotOrdinaire di) {
		final EtatDeclaration dernierEtat = di.getDernierEtat();
		final EtatDeclaration etatSomme = di.getEtatDeclarationActif(TypeEtatDeclaration.SOMMEE);
		return etatSomme != null && dernierEtat.getEtat() == TypeEtatDeclaration.RETOURNEE && dernierEtat.getDateObtention() == etatSomme.getDateObtention();
	}

	private static final String QUERY_STRING = "SELECT di.id"// ----------------------------------------------------
			+ " FROM DeclarationImpotOrdinaire AS di" // -----------------------------------------------------------
			+ " WHERE di.annulationDate IS NULL "// ----------------------------------------------------------------
			+ " AND EXISTS ("// ------------------------------------------------------------------------------------
			+ "   SELECT etat1.declaration.id"// -------------------------------------------------------------------
			+ "   FROM EtatDeclaration AS etat1"// -----------------------------------------------------------------
			+ "   WHERE di.id = etat1.declaration.id"// ------------------------------------------------------------
			+ "     AND etat1.annulationDate IS NULL"// ------------------------------------------------------------
			+ "     AND etat1.etat = 'SOMMEE'"// -------------------------------------------------------------------
			+ "     AND etat1.dateObtention IN ("// ----------------------------------------------------------------
			+ "       SELECT MAX(etat2.dateObtention) "// ----------------------------------------------------------
			+ "       FROM EtatDeclaration AS etat2 "// ------------------------------------------------------------
			+ "       WHERE etat1.declaration.id = etat2.declaration.id AND etat2.annulationDate IS NULL))" // ------
			+ " ORDER BY di.id";

	/**
	 * @return les ids des DIs dont l'état courant est <i>sommée</i>.
	 */
	@SuppressWarnings("unchecked")
	private List<Long> retrieveListDIsSommees() {

		final List<Long> ids = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {

				// requêtes nécessaires en production pour avoir des performances acceptables
				SQLQuery sqlQuery = session.createSQLQuery("alter session set optimizer_index_caching = 90");
				sqlQuery.executeUpdate();
				sqlQuery = session.createSQLQuery("alter session set optimizer_index_cost_adj = 10");
				sqlQuery.executeUpdate();

				Query query = session.createQuery(QUERY_STRING);
				return query.list();
			}
		});
		return ids;
	}

	/**
	 * Uniquement pour le testing.
	 */
	protected void setRapport(EchoirDIsResults rapport) {
		this.rapport = rapport;
	}
}
