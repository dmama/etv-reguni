package ch.vd.uniregctb.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;

/**
 * {@inheritDoc}
 */
public class DocumentServiceImpl implements DocumentService {

	private final Logger LOGGER = LoggerFactory.getLogger(DocumentServiceImpl.class);

	private HibernateTemplate hibernateTemplate;

	private File repository;

	/**
	 * Contient les types concrets de documents, indexés par leur noms en lettres minuscules
	 */
	private static final Map<String, Class<? extends Document>> docType = new HashMap<>();
	static {
		docType.put(DatabaseDump.class.getSimpleName().toLowerCase(), DatabaseDump.class);
		docType.put(DeterminationDIsRapport.class.getSimpleName().toLowerCase(), DeterminationDIsRapport.class);
		docType.put(EnvoiDIsRapport.class.getSimpleName().toLowerCase(), EnvoiDIsRapport.class);
		docType.put(MajoriteRapport.class.getSimpleName().toLowerCase(), MajoriteRapport.class);
		docType.put(FusionDeCommunesRapport.class.getSimpleName().toLowerCase(), FusionDeCommunesRapport.class);
		docType.put(RolesCommunesRapport.class.getSimpleName().toLowerCase(), RolesCommunesRapport.class);
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setRepository(String repository) {
		if (repository.startsWith("${")) {
			throw new RuntimeException(
					"Le repository des documents n'est pas défini. Veuillez spécifier la propriété 'extprop.documents.repository' dans le fichier unireg.properties.");
		}
		File dir = new File(repository);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		this.repository = dir;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Document> ramasseDocs() {
		List<Document> docs = new ArrayList<>();
		ramasseDir(repository, "", docs);
		return docs;
	}

	private void ramasseDir(File dir, String subpath, List<Document> docs) {

		File[] files = dir.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				// on descend en récursion sur les répertoires
				final String dirsubpath = FilenameUtils.concat(subpath, f.getName().toLowerCase());
				ramasseDir(f, dirsubpath, docs);
			}
			else {
				final String dirname = dir.getName().toLowerCase();
				ramasseFile(f, subpath, dirname, docs);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void ramasseFile(File f, String subpath, String dirname, List<Document> docs) {

		// on vérifie que le fichier courant existe bien dans la base
		final Map<String, String> params = new HashMap<>(2);
		params.put("subPath", subpath);
		params.put("fileName", f.getName());
		List<Document> list = hibernateTemplate.find("from Document as doc where doc.subPath=:subPath and doc.fileName=:fileName", params, null);
		if (list != null && !list.isEmpty()) {
			// le fichier existe -> ok
			return;
		}

		// en fonction du nom du répertoire courant (= design du repository) on peut déduire le nom du type de document
		Class<? extends Document> clazz = docType.get(dirname);
		if (clazz == null) {
			LOGGER.warn("Impossible de ramasser le fichier [" + f + "] car le type de document [" + dirname + "] est inconnu.");
			return;
		}

		// on crée un nouveau document
		Document doc;
		try {
			doc = clazz.newInstance();
		}
		catch (Exception e) {
			LOGGER.error("Impossible de ramasser le fichier [" + f + "] car il est impossible d'instancier un objet de la classe " + clazz,
					e);
			return;
		}

		// on reconstruit les infos le mieux possible à partir du nom du fichier
		String filename = f.getName();
		doc.setNom(FilenameUtils.getBaseName(filename));
		doc.setFileExtension(FilenameUtils.getExtension(filename));
		doc.setDescription("(fichier récupéré du disque par la job de ramassage des documents");
		doc.setFileName(filename);
		doc.setSubPath(subpath);

		doc.setFileSize(f.length());
		doc = hibernateTemplate.merge(doc);
		docs.add(doc);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Document get(Long id) throws Exception {
		return hibernateTemplate.get(Document.class, id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(final Document doc) throws Exception {
		Assert.notNull(doc);

		String filepath = getFilePath(doc);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Effacement du document " + filepath);
		}

		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				session.delete(doc);
				return null;
			}
		});

		File file = ResourceUtils.getFile(filepath);
		if (file != null && file.exists()) {
			file.delete();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Collection<Document> getDocuments() throws Exception {
		return hibernateTemplate.find("FROM Document AS doc WHERE doc.annulationDate IS null", null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Document> Collection<T> getDocuments(Class<T> clazz) throws Exception {
		return hibernateTemplate.find("FROM " + clazz.getSimpleName() + " AS doc WHERE doc.annulationDate IS null", null);
	}

	/**
	 * {@inheritDoc}
	 */
	private String getFilePath(Document doc) {
		String path = getFileDir(doc);
		path = addPath(path, doc.getFileName());
		return path;
	}

	/**
	 * @return le chemin absolu du sous-répertoire du repository contenant le document spécifié
	 */
	private String getFileDir(Document doc) {
		String path = repository.getAbsolutePath();
		path = addPath(path, doc.getSubPath());
		return path;
	}

	/**
	 * Crée le répertoire dans le repository pour le document spécifié, et retourne le filepath du document.
	 */
	private String createFilePath(Document doc) {
		String path = getFileDir(doc);
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		path = addPath(path, doc.getFileName());
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Document> T newDoc(Class<T> clazz, String nom, String description, String fileExtension, WriteDocCallback<T> callback)
			throws Exception {

		Date date = DateHelper.getCurrentDate();

		// Création du nouveau document
		T doc = clazz.newInstance();
		doc.setNom(nom);
		doc.setDescription(description);
		doc.setFileName(buildFileName(nom, fileExtension, date));
		doc.setFileExtension(fileExtension);
		doc.setSubPath(buildSubPath(clazz, date));

		// Ouverture du flux
		String filepath = createFilePath(doc);
		File file = new File(filepath);
		if (!file.createNewFile()) {
			throw new RuntimeException("Le fichier " + doc.getFileName() + " existe déjà. Veuillez choisir un autre nom.");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Création du document " + filepath);
		}

		try (OutputStream os = new FileOutputStream(file)) {
			// Remplissage du document
			callback.writeDoc(doc, os);
		}

		doc.setFileSize(file.length());
		return hibernateTemplate.merge(doc);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Document> void readDoc(T doc, ReadDocCallback<T> callback) throws Exception {

		// Ouverture en lecture du document
		String path = getFilePath(doc);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Lecture du document " + path);
		}

		try (FileInputStream is = new FileInputStream(path)) {
			// Lecture du document
			callback.readDoc(doc, is);
		}
	}

	/**
	 * Construit un nom de fichier sur le disque à partir de la date courante et du nom "utilisateur" spécifié.
	 * <p>
	 * Le format est : yyyyMMdd_$(nom_utilisateur_sans_les_caracteres_speciaux)
	 */
	private String buildFileName(String nom, String fileExtension, Date d) {
		StringBuilder b = new StringBuilder();
		b.append(new SimpleDateFormat("yyyyMMdd_kkmmss").format(d));
		b.append('_');
		b.append(nom.replaceAll("[^-+0-9a-zA-Z._]", "_"));
		b.append('.').append(fileExtension);
		String filename = b.toString();
		return filename;
	}

	/**
	 * Construit le chemin d'accès (relatif au répertoire de base des fichiers) en fonction du type de document et de la date courante.
	 */
	private <T extends Document> String buildSubPath(Class<T> typeDoc, Date d) {
		RegDate date = RegDateHelper.get(d);
		String subPath = String.valueOf(date.year());
		subPath = addPath(subPath, String.valueOf(date.month()));
		subPath = addPath(subPath, typeDoc.getSimpleName().toLowerCase());
		return subPath;
	}

	private static String addPath(String left, String right) {
		if (left.charAt(left.length() - 1) == File.separatorChar) {
			return left + right;
		}
		else {
			return left + File.separatorChar + right;
		}
	}
}
