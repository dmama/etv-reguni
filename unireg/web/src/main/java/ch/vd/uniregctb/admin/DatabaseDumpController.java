package ch.vd.uniregctb.admin;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.database.DatabaseService;
import ch.vd.uniregctb.database.DumpDatabaseJob;
import ch.vd.uniregctb.database.DumpTiersListJob;
import ch.vd.uniregctb.database.LoadDatabaseJob;
import ch.vd.uniregctb.dbutils.TooManyTiersException;
import ch.vd.uniregctb.document.Document;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Controller spring qui permet de dumper la base de données dans un fichier XML
 */
public class DatabaseDumpController extends AbstractSimpleFormController {

	// private final Logger LOGGER = Logger.getLogger(DatabaseDumpController.class);

	private static final int MAX_TIERS_TO_DUMP = 1000;

	private DatabaseService dbService;
	private DocumentService docService;
	private BatchScheduler batchScheduler;
	private TiersDAO dao;
	private PlatformTransactionManager transactionManager;

	private Map<String, Action> actions = new HashMap<String, Action>();

	public DatabaseDumpController() {
		actions.put("dump", new DownloadAll());
		actions.put("dump2fs", new DumpToFileSystem());
		actions.put("fs2import", new ImportFromFileSystem());
		actions.put("dumptiers", new DownloadTiers());
	}

	@Override
	public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

		if (!SecurityProvider.isAnyGranted(Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		final String environnement = UniregModeHelper.getEnvironnement();
		final boolean inDev = environnement.equals("Developpement") || environnement.equals("Standalone");

		final Action a = getAction(request);
		if (a != null) {

			if (!inDev && a.inDevOnly()) {
				flashError("Cette fonctionalité n'est disponible qu'en développement !");
				return new ModelAndView(new RedirectView("tiersImport/list.do"));
			}

			final ModelAndView mav = a.execute(request, response);
			if (mav != null) {
				return mav;
			}
		}

		return new ModelAndView(new RedirectView(getSuccessView()));
	}

	private Action getAction(HttpServletRequest request) {
		Action a = null;

		final String action = request.getParameter("action");
		if (action != null) {
			a = actions.get(action);
		}
		return a;
	}

	/**
	 * Une action supportée par ce contrôleur.
	 */
	private static interface Action {

		boolean inDevOnly();

		@Nullable
		ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception;
	}

	/**
	 * Dump le contenu de la base de données dans la réponse Htpp de telle manière que l'utilisateur puisse immédiatement le sauver sur sa machine. Cette manière de faire est limité à 1000 tiers pour
	 * éviter de tuer la machine.
	 */
	private class DownloadAll implements Action {

		@Override
		public boolean inDevOnly() {
			return true;
		}

		@Override
		public ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {

			TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);

			final int nbTiers = template.execute(new TransactionCallback<Integer>() {
				@Override
				public Integer doInTransaction(TransactionStatus status) {
					return dao.getCount(Tiers.class);
				}
			});

			if (nbTiers > MAX_TIERS_TO_DUMP) {
				throw new TooManyTiersException("Il y a " + nbTiers + " tiers dans la base de données. Impossible d'exporter plus de "
						+ MAX_TIERS_TO_DUMP);
			}

			final ByteArrayOutputStream content = new ByteArrayOutputStream();

			// Dump la base de données
			template.execute(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					try {
						dbService.dumpToDbunitFile(content);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
					return null;
				}
			});


			// Retourne le contenu de la base sous forme de fichier XML
			final String filename = "database-dump-" + RegDate.get().toString() + ".xml";
			Audit.info("La base de données de données à été exportée directement sur le poste client (" + filename + ").");

			ServletOutputStream out = response.getOutputStream();
			response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'

			String mimetype = DatabaseDumpController.this.getServletContext().getMimeType(filename);
			response.setContentType(mimetype);
			response.setContentLength(content.size());
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + '\"');
			FileCopyUtils.copy(content.toByteArray(), out);
			return null;
		}
	}

	/**
	 * Démarre un job qui va dumper la base de manière asynchrone
	 */
	private class DumpToFileSystem implements Action {

		@Override
		public boolean inDevOnly() {
			return true;
		}

		@Override
		public ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
			batchScheduler.startJob(DumpDatabaseJob.NAME, null);
			return new ModelAndView(new RedirectView("batch.do"));
		}
	}

	/**
	 * Démarre un job qui va recharger la base de manière asynchrone
	 */
	private class ImportFromFileSystem implements Action {

		@Override
		public boolean inDevOnly() {
			return true;
		}

		@Override
		public ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {

			final Document doc = getDoc(request);
			if (doc != null) {
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put(LoadDatabaseJob.DOC_ID, doc.getId());
				batchScheduler.startJob(LoadDatabaseJob.NAME, params);
			}
			return new ModelAndView(new RedirectView("batch.do"));
		}
	}

	/**
	 * Démarre un job qui va dumper les données (tiers, adresses, rapports, etc.) relatives aux tiers spécifiés.
	 */
	private class DownloadTiers implements Action {

		@Override
		public boolean inDevOnly() {
			// parce qu'on veut pouvoir exporter un tiers même en production pour permettre de reproduire une situation en développement
			return false;
		}

		@Override
		public ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
			final HashMap<String, Object> params = new HashMap<String, Object>();
			params.put(DumpTiersListJob.PARAM_TIERS_LIST, request.getParameter("tiers"));
			params.put(DumpTiersListJob.INCLUDE_DECLARATION, true);
			params.put(DumpTiersListJob.INCLUDE_RET, true);
			params.put(DumpTiersListJob.INCLUDE_SIT_FAM, true);

			batchScheduler.startJob(DumpTiersListJob.NAME, params);
			return new ModelAndView(new RedirectView("batch.do"));
		}
	}

	private Document getDoc(HttpServletRequest request) throws Exception {
		final String idAsString = request.getParameter("file");
		if (idAsString == null) {
			return null;
		}

		final Long id;
		try {
			id = Long.valueOf(idAsString);
		}
		catch (NumberFormatException ignored) {
			return null;
		}

		return docService.get(id);
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
}
