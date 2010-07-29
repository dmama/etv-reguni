package ch.vd.uniregctb.admin;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.admin.ScriptBean.DBUnitMode;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.database.DatabaseService;
import ch.vd.uniregctb.document.DatabaseDump;
import ch.vd.uniregctb.document.DocumentService;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Cette classe est le controlleur correspondant au cas d'utilisation "Lancer un
 * script DBUnit".
 *
 * Dans un premier temps il récupère la liste des scripts DBUnit
 * classpath:DBUnit4Import/*.xml Puis lors de la soumission du formulaire, il
 * charge et lance le script choisi, ou lance le script uploadé si le chemin
 * vers un script a été donnée dans le formulaire.
 *
 * @author Ludovic Bertin
 *
 */
public class TiersImportController extends AbstractSimpleFormController {

	private static final Logger LOGGER = Logger.getLogger(TiersImportController.class);

	private static final String SCRIPTS_FOLDER_PATH = "DBUnit4Import";
	private static final String SCRIPTS_LIST_FILES = SCRIPTS_FOLDER_PATH+"/files-list.txt";

	private TiersDAO dao;
	private AdresseService adresseService;
	private DocumentService docService;
	private DatabaseService dbService;

	private GlobalTiersIndexer globalIndexer;
	private PlatformTransactionManager transactionManager;

	/**
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */

	@Override
	@SuppressWarnings("unchecked")
	protected Map referenceData(HttpServletRequest request) throws Exception {
		Map returnedMap = new HashMap();
		returnedMap.put("scriptFileNames", getScriptFilenames());
		return returnedMap;
	}

	protected List<LoadableFileDescription> getScriptFilenames() throws Exception {

		LOGGER.debug("Getting DBunit files from "+SCRIPTS_LIST_FILES);
		InputStream scriptFiles = getClass().getClassLoader().getResourceAsStream(SCRIPTS_LIST_FILES);

		List<LoadableFileDescription> scriptFileNames = new ArrayList<LoadableFileDescription>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(scriptFiles, "UTF-8"));
		String line = reader.readLine();
		while (line != null) {

			String[] parts = line.split(",");
			String filename = parts[0];
			String description = parts[1];
			LOGGER.debug("Added file "+filename+" ("+description+") to list of loadable DBunit file");

			// Juste pour vérifier que le fichier existe!
			URL scriptFile = getClass().getClassLoader().getResource(SCRIPTS_FOLDER_PATH+"/"+filename);
			Assert.notNull(scriptFile, "Le fichier DBunit "+filename+" n'existe pas dans le repertoire "+SCRIPTS_FOLDER_PATH);

			LoadableFileDescription descr = new LoadableFileDescription(description, filename);
			scriptFileNames.add(descr);

			line = reader.readLine();
		}

		LOGGER.debug("Files found in "+SCRIPTS_FOLDER_PATH+": "+scriptFileNames.size());
		return scriptFileNames;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors, Map model) throws Exception {

		if (!UniregModeHelper.isTestMode()) {
			return new ModelAndView(new RedirectView("/index.do", true));
		}

		final ModelAndView mav = super.showForm(request, response, errors, model);
		//HttpSession session = request.getSession();

		mav.addObject("listFilesName", getScriptFilenames());
		mav.addObject("fileDumps", docService.getDocuments(DatabaseDump.class));

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				mav.getModel().put("tiersCount", dao.getCount(Tiers.class));
				return null;
			}
		});

		return mav;
	}

	/**
	 * On ajoute un custom editeur pour que Spring puisse mapper l'objet
	 * MultiPart en un tableau de byte.
	 *
	 * @param request
	 *            la requête HTTP
	 * @param binder
	 *            le binder Spring
	 */
	@Override
	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {
		binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
	}

	/**
	 * Handler déclenché à la soumission du formulaire.
	 *
	 * @throws Exception
	 */
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {

		if (!SecurityProvider.isGranted(Role.ADMIN) && !SecurityProvider.isGranted(Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		// recupere la vue de confirmation
		ModelAndView successView = super.onSubmit(request, response, command, errors);

		// Déclaration d'un ByteArrayInputStream qui sera donné à DBUnit
		InputStream inputXML = null;
		// Récupération du bean
		ScriptBean script = (ScriptBean) command;

		String action = request.getParameter("action");
		if (action != null && action.equals("launchUnit")) {
			String fileName  = request.getParameter("fileName");
			script.setScriptFileName(fileName);
		}

		String filename = "";
		// on regarde si un script a été uploadé
		if ((script.getScriptData() != null) && (script.getScriptData().length > 0)) {
			filename = "<Raw data>";
			inputXML = new ByteArrayInputStream(script.getScriptData());
		}
		// sinon on prend le script connu sélectionné
		else if (script.getScriptFileName() != null) {

			filename = script.getScriptFileName();
			if (!filename.startsWith("/")) {
				filename = "/"+SCRIPTS_FOLDER_PATH + "/" + filename;
			}
			inputXML = getClass().getResourceAsStream(filename);
		}

		if (inputXML != null || script.getMode() == DBUnitMode.DELETE_ALL) {
			try {

				// lancement du script
				launchDbUnit(inputXML, script.getMode(), filename);

				successView.addObject("scriptResult", "success");

				final List<InfoTiers> infoTiers = buildInfoTiers();
				successView.addObject("infoTiers", infoTiers);
			}
			catch (IndexerException e) {
				successView.addObject("scriptResult", "IndexerException");
				successView.addObject("exception", e);
			}
			catch (DataSetException e) {
				successView.addObject("scriptResult", "datasetException");
				successView.addObject("exception", e);
			}
			catch (DatabaseUnitException e) {
				successView.addObject("scriptResult", "databaseUnitException");
				successView.addObject("exception", e);
			}
			catch (SQLException e) {
				successView.addObject("scriptResult", "sqlException");
				successView.addObject("exception", e);
			}
		}
		else {
			successView.addObject("scriptResult", "noInputStream");
		}
		return successView;
	}

	/**
	 * Construit la liste des numéros, type et noms courrier des 100 premiers tiers de la base de données.
	 *
	 * @return une liste contenant des informaitons de tiers
	 */
	private List<InfoTiers> buildInfoTiers() {

		final List<InfoTiers> infoTiers = new ArrayList<InfoTiers>();

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final List<Tiers> list = dao.getFirst(100);
				for (Tiers t : list) {
					
					final Long numero = t.getNumero();
					final String type = t.getNatureTiers();
					List<String> nomsPrenoms;
					try {
						nomsPrenoms = adresseService.getNomCourrier(t, null, false);
					}
					catch (Exception e) {
						nomsPrenoms = Arrays.asList("Exception: " + e.getMessage());
					}

					InfoTiers info = new InfoTiers(numero, type, nomsPrenoms);
					infoTiers.add(info);
				}
				return null;
			}
		});

		return infoTiers;
	}

	public static class InfoTiers {
		
		private final long numero;
		private final String type;
		private final String nomsPrenoms;

		public InfoTiers(long numero, String type, List<String> nomsPrenoms) {
			this.numero = numero;
			this.type = type;

			StringBuilder b = new StringBuilder();
			for (String n : nomsPrenoms) {
				if (b.length() != 0) {
					b.append(" / ");
				}
				b.append(n);
			}
			this.nomsPrenoms = b.toString();
		}

		public long getNumero() {
			return numero;
		}

		public String getType() {
			return type;
		}

		@SuppressWarnings({"UnusedDeclaration"})
		public String getNomsPrenoms() {
			return nomsPrenoms;
		}
	}

	/**
	 * Lancement du script DBUnit. Attention a bien configurer le transaction manager dans Spring pour que cette méthode se fasse dans une transaction.
	 *
	 * @param inputXML le flux d'entrée contenant le script DBUnit
	 * @param mode     le mode de lancement
	 * @param filename le nom fichier à charger
	 * @throws DataSetException      si le script XML n'est pas syntaxiquement correct
	 * @throws SQLException          s'il y a un problème de connection à la base de données
	 * @throws DatabaseUnitException s'il y a une erreur lors de l'exécution du script
	 */
	private void launchDbUnit(InputStream inputXML, DBUnitMode mode, String filename) throws Exception {

		LOGGER.info("Chargement de la base en mode = " + mode + " avec le fichier " + filename);

		// D'abord vide la base
		switch (mode) {
		case DELETE_ALL:
		case CLEAN_INSERT: {
			dbService.truncateDatabase();
			globalIndexer.overwriteIndex();
			Audit.success("La base de données est vidée");
		}
		break;
		}

		switch (mode) {
		case INSERT_APPEND:
		case CLEAN_INSERT: {
			dbService.loadFromDbunitFile(inputXML, null);
			Audit.success("La base de données a été chargée avec le fichier " + filename);
		}
		break;
		}

		// Tout s'est bien passé => Re-Indexation des données
		final int count = globalIndexer.indexAllDatabase();
		Audit.success("La base de données a été réindexée. Elle contient " + count + " entrées");
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setGlobalIndexer(GlobalTiersIndexer globalIndexer) {
		this.globalIndexer = globalIndexer;
	}

	public void setTiersDAO(TiersDAO dao) {
		this.dao = dao;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDbService(DatabaseService dbService) {
		this.dbService = dbService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
