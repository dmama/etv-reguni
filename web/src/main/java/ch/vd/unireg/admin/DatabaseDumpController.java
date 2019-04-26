package ch.vd.unireg.admin;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.database.DatabaseService;
import ch.vd.unireg.database.DumpDatabaseJob;
import ch.vd.unireg.database.DumpTiersListJob;
import ch.vd.unireg.database.LoadDatabaseJob;
import ch.vd.unireg.dbutils.TooManyTiersException;
import ch.vd.unireg.document.Document;
import ch.vd.unireg.document.DocumentService;
import ch.vd.unireg.scheduler.BatchScheduler;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.utils.UniregModeHelper;

/**
 * Controller spring qui permet de dumper la base de données dans un fichier XML
 */
@Controller
@RequestMapping(value = "/admin/dbdump")
public class DatabaseDumpController {

	// private final Logger LOGGER = LoggerFactory.getLogger(DatabaseDumpController.class);

	private static final int MAX_TIERS_TO_DUMP = 1000;

	private DatabaseService dbService;
	private DocumentService docService;
	private BatchScheduler batchScheduler;
	private TiersDAO dao;
	private PlatformTransactionManager transactionManager;
	private SecurityProviderInterface securityProvider;
	private AuditManager audit;

	@RequestMapping(value = "/dump.do", method = RequestMethod.POST)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String dump(HttpServletResponse response) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final String environnement = UniregModeHelper.getEnvironnement();
		final boolean inDev = environnement.equals("Developpement");
		if (!inDev) {
			Flash.error("Cette fonctionalité n'est disponible qu'en développement !");
			return "redirect:/admin/tiersImport/list.do";
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final int nbTiers = template.execute(status -> dao.getCount(Tiers.class));
		if (nbTiers > MAX_TIERS_TO_DUMP) {
			throw new TooManyTiersException("Il y a " + nbTiers + " tiers dans la base de données. Impossible d'exporter plus de " + MAX_TIERS_TO_DUMP);
		}

		final ByteArrayOutputStream content = new ByteArrayOutputStream();

		// Dump la base de données
		template.execute(status -> {
			try {
				dbService.dumpToDbunitFile(content);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			return null;
		});

		// Retourne le contenu de la base sous forme de fichier XML
		final String filename = "database-dump-" + RegDate.get().toString() + ".xml";
		audit.info("La base de données de données à été exportée directement sur le poste client (" + filename + ").");

		ServletOutputStream out = response.getOutputStream();
		response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'

		final ServletContext servletContext = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getServletContext();
		String mimetype = servletContext.getMimeType(filename);
		response.setContentType(mimetype);
		response.setContentLength(content.size());
		response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + '\"');
		FileCopyUtils.copy(content.toByteArray(), out);

		return "redirect:/admin/tiersImport/list.do";
	}

	@RequestMapping(value = "/dump2fs.do", method = RequestMethod.POST)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String dump2fs() throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final String environnement = UniregModeHelper.getEnvironnement();
		final boolean inDev = environnement.equals("Developpement");
		if (!inDev) {
			Flash.error("Cette fonctionalité n'est disponible qu'en développement !");
			return "redirect:/admin/tiersImport/list.do";
		}

		batchScheduler.startJob(DumpDatabaseJob.NAME, null);
		return "redirect:/admin/batch.do";
	}

	@RequestMapping(value = "/fs2import.do", method = RequestMethod.POST)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String fs2import(@RequestParam(value = "file") long fileId) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final String environnement = UniregModeHelper.getEnvironnement();
		final boolean inDev = environnement.equals("Developpement");
		if (!inDev) {
			Flash.error("Cette fonctionalité n'est disponible qu'en développement !");
			return "redirect:/admin/tiersImport/list.do";
		}

		final Document doc = docService.get(fileId);
		if (doc == null) {
			Flash.error("Le document n°" + fileId + " est inconnu.");
			return "redirect:/admin/tiersImport/list.do";
		}

		final Map<String, Object> params = new HashMap<>();
		params.put(LoadDatabaseJob.DOC_ID, doc.getId());
		batchScheduler.startJob(LoadDatabaseJob.NAME, params);
		return "redirect:/admin/batch.do";
	}

	@RequestMapping(value = "/dumptiers.do", method = RequestMethod.POST)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String dumptiers(@RequestParam(value = "tiers") String tiersList) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final Map<String, Object> params = new HashMap<>();
		params.put(DumpTiersListJob.PARAM_TIERS_LIST, tiersList);
		params.put(DumpTiersListJob.INCLUDE_DECLARATION, true);
		params.put(DumpTiersListJob.INCLUDE_RET, true);
		params.put(DumpTiersListJob.INCLUDE_SIT_FAM, true);

		batchScheduler.startJob(DumpTiersListJob.NAME, params);
		return "redirect:/admin/batch.do";
	}

	public void setDatabaseService(DatabaseService dbService) {
		this.dbService = dbService;
	}

	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	public void setTiersDAO(TiersDAO dao) {
		this.dao = dao;
	}

	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}
}
