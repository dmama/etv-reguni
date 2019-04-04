package ch.vd.unireg.admin;

import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.dbunit.DatabaseUnitException;
import org.dbunit.dataset.DataSetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import ch.vd.unireg.admin.ScriptBean.DBUnitMode;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.database.DatabaseService;
import ch.vd.unireg.document.DatabaseDump;
import ch.vd.unireg.document.DocumentService;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.utils.UniregModeHelper;

/**
 * Cette classe est le controlleur correspondant au cas d'utilisation "Lancer un script DBUnit".
 * <p/>
 * Dans un premier temps il récupère la liste des scripts DBUnit classpath:DBUnit4Import/*.xml Puis lors de la soumission du formulaire, il charge et lance le script choisi, ou lance le script uploadé
 * si le chemin vers un script a été donnée dans le formulaire.
 *
 * @author Ludovic Bertin
 */
@Controller
@RequestMapping(value = "/admin/tiersImport")
public class TiersImportController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersImportController.class);

	private static final String SCRIPTS_FOLDER_PATH = "DBUnit4Import";
	private static final String SCRIPTS_LIST_FILES = SCRIPTS_FOLDER_PATH + "/files-list.txt";

	private TiersDAO dao;
	private DocumentService docService;
	private DatabaseService dbService;

	private GlobalTiersIndexer globalIndexer;
	private SecurityProviderInterface securityProvider;

	/**
	 * Cette méthode est appelée pour afficher la page qui liste les scripts DBUnit préxistants + le formulaire pour en uploader d'autres.
	 *
	 * @param model le modèle associé à la requête
	 * @return le chemin vers la vue JSP qui doit être associée avec le modèle
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String listScripts(Model model) {

		if (!UniregModeHelper.isTestMode()) {
			return "redirect:/index.do";
		}

		final List<LoadableFileDescription> scriptFilenames;
		final Collection<DatabaseDump> documents;
		try {
			scriptFilenames = getScriptFilenames();
			documents = docService.getDocuments(DatabaseDump.class);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		final Integer tiersCount = dao.getCount(Tiers.class);

		model.addAttribute("script", new ScriptBean());
		model.addAttribute("listFilesName", scriptFilenames);
		model.addAttribute("fileDumps", documents);
		model.addAttribute("tiersCount", tiersCount);

		return "admin/tiersImportForm";
	}

	/**
	 * Cette méthode est appelée lorsque l'utilisateur choisit d'importer un fichier DBUnit préexistant (= tiers-basic,xml, pour l'instant).
	 *
	 * @param fileName le nom du fichier XML préexistant à charger
	 * @param action   l'action à effectuer sur le fichier
	 * @return une redirection vers la page de prévisualisation des tiers
	 * @throws Exception en cas d'erreur
	 */
	@RequestMapping(value = "/import", method = RequestMethod.POST)
	public String importBuiltinScript(@RequestParam("fileName") String fileName, @RequestParam("action") String action) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final String environnement = UniregModeHelper.getEnvironnement();
		if (!"Developpement".equals(environnement) && !"Hudson".equals(environnement)) {
			Flash.error("Cette fonctionalité n'est disponible qu'en développement !");
			return "redirect:list.do";
		}

		if (!fileName.startsWith("/")) {
			fileName = '/' + SCRIPTS_FOLDER_PATH + '/' + fileName;
		}
		final InputStream inputXML = getClass().getResourceAsStream(fileName);

		if (inputXML == null) {
			Flash.error("Impossible de trouver le script '" + HtmlUtils.htmlEscape(fileName) + '\'');
			return "redirect:list.do";
		}

		// lancement du script
		launchDbUnit(inputXML, DBUnitMode.CLEAN_INSERT, fileName);

		// tout c'est bien passé, on redirige vers la preview des données de la base
		return "redirect:/admin/dbpreview.do";
	}

	/**
	 * Cette méthode est appelée lorsque l'utilisateur upload un fichier DBUnit dans le but de le charger dans la base de données.
	 *
	 * @param script le script DBUnit (nom, données, ...) uploadé par l'utilisateur
	 * @param result le résultat du binding (qui permet de vérifier d'éventuelles erreurs de validation)
	 * @return une redirection vers la page de prévisualisation des tiers
	 * @throws Exception en cas d'erreur
	 */
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String importUploadedScript(@Valid ScriptBean script, BindingResult result) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final String environnement = UniregModeHelper.getEnvironnement();
		if (!"Developpement".equals(environnement) && !"Hudson".equals(environnement)) {
			Flash.error("Cette fonctionalité n'est disponible qu'en développement !");
			return "redirect:list.do";
		}

		if (result.hasErrors()) {
			Flash.error("Les erreurs suivantes ont été détectées :  " + Arrays.toString(result.getAllErrors().toArray()));
			return "redirect:list.do";
		}

		// on regarde si un fichier a bien été uploadé
		if (script.getScriptData() == null || script.getScriptData().isEmpty()) {
			Flash.error("Aucun fichier n'a été spécifié ou il est vide !");
			return "redirect:list.do";
		}

		final InputStream inputXML = new ByteArrayInputStream(script.getScriptData().getBytes());
		if (inputXML == null) {
			throw new IllegalArgumentException();
		}

		// lancement du script
		launchDbUnit(inputXML, script.getMode(), "<Raw data>");

		// tout c'est bien passé, on redirige vers la preview des données de la base
		return "redirect:/admin/dbpreview.do";
	}

	protected List<LoadableFileDescription> getScriptFilenames() throws Exception {

		LOGGER.debug("Getting DBunit files from " + SCRIPTS_LIST_FILES);

		final List<LoadableFileDescription> scriptFileNames = new ArrayList<>();
		try (InputStream scriptFiles = getClass().getClassLoader().getResourceAsStream(SCRIPTS_LIST_FILES);
		     InputStreamReader inReader = new InputStreamReader(scriptFiles, "UTF-8");
		     BufferedReader reader = new BufferedReader(inReader)) {

			String line = reader.readLine();
			while (line != null) {
				final String[] parts = line.split(",");
				final String filename = parts[0];
				final String description = parts[1];
				LOGGER.debug("Added file " + filename + " (" + description + ") to list of loadable DBunit file");

				// Juste pour vérifier que le fichier existe!
				final URL scriptFile = getClass().getClassLoader().getResource(SCRIPTS_FOLDER_PATH + '/' + filename);
				if (scriptFile == null) {
					throw new IllegalArgumentException("Le fichier DBunit " + filename + " n'existe pas dans le repertoire " + SCRIPTS_FOLDER_PATH);
				}

				final LoadableFileDescription descr = new LoadableFileDescription(description, filename);
				scriptFileNames.add(descr);

				line = reader.readLine();
			}
		}

		LOGGER.debug("Files found in " + SCRIPTS_FOLDER_PATH + ": " + scriptFileNames.size());
		return scriptFileNames;
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
			dbService.loadFromDbunitFile(inputXML, null, false);
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDocService(DocumentService docService) {
		this.docService = docService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDbService(DatabaseService dbService) {
		this.dbService = dbService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}
