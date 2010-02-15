package ch.vd.uniregctb.admin;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	@Override
	public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

		if (!SecurityProvider.isGranted(Role.ADMIN) && !SecurityProvider.isGranted(Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		final String action = request.getParameter("action");
		if (action != null) {
			if ("dump".equals(action)) {
				dumpToResponse(request, response);
			}
			else if ("dump2fs".equals(action)) {
				dumpToFilesystem();
				return new ModelAndView(new RedirectView("batch.do"));
			}
			else if ("fs2import".equals(action)) {
				importFromFilesystem(request, response);
				return new ModelAndView(new RedirectView("batch.do"));
			}
			else if ("dumptiers".equals(action)) {
				dumpTiers(request, response);
				return new ModelAndView(new RedirectView("batch.do"));
			}
		}

		return new ModelAndView(new RedirectView(getSuccessView()));
	}

	/**
	 * Dump le contenu de la base de données dans la réponse Htpp de telle manière que l'utilisateur puisse immédiatement le sauver sur sa
	 * machine. Cette manière de faire est limité à 1000 tiers pour éviter de tuer la machine.
	 */
	private void dumpToResponse(HttpServletRequest request, HttpServletResponse response) throws Exception {

		final int nbTiers = dao.getCount(Tiers.class);
		if (nbTiers > MAX_TIERS_TO_DUMP) {
			throw new TooManyTiersException("Il y a " + nbTiers + " tiers dans la base de données. Impossible d'exporter plus de "
					+ MAX_TIERS_TO_DUMP);
		}

		// Dump la base de données
		ByteArrayOutputStream content = new ByteArrayOutputStream();
		dbService.dumpToDbunitFile(content);

		// Retourne le contenu de la base sous forme de fichier XML
		final String filename = "database-dump-" + RegDate.get().toString() + ".xml";
		Audit.info("La base de données de données à été exportée directement sur le poste client (" + filename + ").");

		String mimetype = this.getServletContext().getMimeType(filename);
		response.setContentType(mimetype);
		response.setContentLength(content.size());
		response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		FileCopyUtils.copy(content.toByteArray(), response.getOutputStream());
	}

	/**
	 * Démarre un job qui va dumper la base de manière asynchrone
	 */
	private void dumpToFilesystem() throws Exception {
		batchScheduler.startJobWithDefaultParams(DumpDatabaseJob.NAME);
	}

	/**
	 * Démarre un job qui va dumper les données (tiers, adresses, rapports, etc.) relatives aux tiers spécifiés.
	 * @param parameter la liste d'id des tiers (séparés par virgule).
	 */
	private void dumpTiers(HttpServletRequest request, HttpServletResponse response) throws Exception {
		final HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(DumpTiersListJob.PARAM_TIERS_LIST, request.getParameter("tiers"));
		
		batchScheduler.startJob(DumpTiersListJob.NAME, params);
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

		final Document doc = docService.get(id);
		return doc;
	}

	/**
	 * Démarre un job qui va recharger la base de manière asynchrone
	 */
	private void importFromFilesystem(HttpServletRequest request, HttpServletResponse response) throws Exception {

		final Document doc = getDoc(request);
		if (doc == null) {
			return;
		}

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(LoadDatabaseJob.DOC_ID, doc.getId());
		batchScheduler.startJob(LoadDatabaseJob.NAME, params);
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
}
