package ch.vd.unireg.database;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.AuditLineDAO;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;
import ch.vd.unireg.scheduler.JobParam;
import ch.vd.unireg.scheduler.JobParamInteger;

public class AuditLogPurgeJob extends JobDefinition {

	private static final String NAME = "AuditLogPurgeJob";
	public static final String DAYS = "DAYS";
	private static final int MIN_DAYS_ALLOWED = 30;

	private AuditLineDAO auditLineDao;
	private PlatformTransactionManager transactionManager;

	public AuditLogPurgeJob(int sortOrder, String description) {
		super(NAME, JobCategory.DB, sortOrder, description);

		{
			final JobParam param = new JobParam();
			param.setDescription("Nombre maximum de jours à conserver");
			param.setMandatory(true);
			param.setName(DAYS);
			param.setType(new JobParamInteger());
			addParameterDefinition(param, 185);
		}
	}

	public void setAuditLineDAO(AuditLineDAO auditLineDao) {
		this.auditLineDao = auditLineDao;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final int delaiPurge = getIntegerValue(params, DAYS);
		if (delaiPurge < MIN_DAYS_ALLOWED) {
			throw new IllegalArgumentException("Le nombre de jours à conserver doit être d'au moins " + MIN_DAYS_ALLOWED);
		}
		final RegDate seuilPurge = RegDate.get().addDays(- delaiPurge);
		audit.info(String.format("Purge des lignes d'audit plus vieilles que %d jour(s) - %s.", delaiPurge, RegDateHelper.dateToDisplayString(seuilPurge)));

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(false);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final int nbPurged = template.execute(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				return auditLineDao.purge(seuilPurge);
			}
		});
		audit.info(String.format("Purge de l'audit terminée : %d ligne(s) effacée(s)", nbPurged));
	}
}
