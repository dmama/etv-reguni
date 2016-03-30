package ch.vd.uniregctb.migration.pm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.migration.pm.engine.probe.ProgressMeasurementProbe;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.WithLongId;

/**
 * Intermédiaire de sérialisation
 */
public class SerializationIntermediary implements Worker, Feeder {

	/**
	 * Répertoire de base des fichiers utilisés
	 */
	private Path fsDirectory;

	/**
	 * Compteur pour les noms de fichiers
	 */
	private final AtomicInteger index = new AtomicInteger(0);

	/**
	 * Map (clé = fichier, valeur = identifiants des objets contenus dans le graphe du fichier) qui permet, au final, de générer le fichier index.txt en fin de DUMP
	 */
	private final Map<File, Ids> fileMap = new TreeMap<>();

	/**
	 * Map (clé = numéro cantonal, valeur = identifiants des entreprises appariées à ce numéro cantonal) qui permet de détecter les numéros cantonaux
	 * d'entreprise utilisés plusieurs fois dans l'appariement, et de générer, au final, le fichier appariements.csv, en fin de DUMP
	 */
	private final Map<Long, Set<Long>> idCantonalMap = new TreeMap<>();

	/**
	 * Sonde de mesure de l'avancement du travail de sérialisation (= DUMP) ou de dé-sérialisation (= FROM_DUMP)
	 */
	private ProgressMeasurementProbe progressMonitor;

	private static class Ids {

		private final List<Long> idsEntreprises;
		private final List<Long> idsEtablissements;
		private final List<Long> idsIndividus;

		public Ids(Graphe graphe) {
			this.idsEntreprises = buildIdList(graphe.getEntreprises());
			this.idsEtablissements = buildIdList(graphe.getEtablissements());
			this.idsIndividus = buildIdList(graphe.getIndividus());
		}

		private static <T extends WithLongId> List<Long> buildIdList(Map<Long, T> map) {
			return map == null || map.isEmpty() ? Collections.<Long>emptyList() : new ArrayList<>(map.keySet());
		}

		@Override
		public String toString() {
			final List<String> array = new ArrayList<>(3);
			if (!idsEntreprises.isEmpty()) {
				array.add(String.format("%d entreprise(s) (%s)", idsEntreprises.size(), Arrays.toString(idsEntreprises.toArray(new Long[idsEntreprises.size()]))));
			}
			if (!idsEtablissements.isEmpty()) {
				array.add(String.format("%d établissement(s) (%s)", idsEtablissements.size(), Arrays.toString(idsEtablissements.toArray(new Long[idsEtablissements.size()]))));
			}
			if (!idsIndividus.isEmpty()) {
				array.add(String.format("%d individu(s) (%s)", idsIndividus.size(), Arrays.toString(idsIndividus.toArray(new Long[idsIndividus.size()]))));
			}

			if (array.isEmpty()) {
				return "rien (???)";
			}
			else {
				return array.stream().collect(Collectors.joining(", "));
			}
		}
	}

	public void setFsDirectory(String fsDirectory) throws IOException {
		if (StringUtils.isNotBlank(fsDirectory)) {
			this.fsDirectory = new File(fsDirectory).toPath();

			if (Files.exists(this.fsDirectory, LinkOption.NOFOLLOW_LINKS)) {
				// des fois que cela serait un lien symbolique...
				Files.createDirectories(this.fsDirectory.toRealPath());
			}
			else {
				Files.createDirectories(this.fsDirectory);
			}
		}
	}

	public void setProgressMonitor(ProgressMeasurementProbe progressMonitor) {
		this.progressMonitor = progressMonitor;
	}

	private File buildNewFile() {
		final int newIndex = index.incrementAndGet();
		final String filename = String.format("%08d.data", newIndex);
		return fsDirectory.resolve(filename).toFile();

	}

	@Override
	public void feed(Worker worker) throws Exception {

		// on va traiter les fichiers dans l'ordre où ils ont été entrés
		final Pattern fileNamePattern = Pattern.compile("[0-9]{8}\\.data");
		final File[] files = fsDirectory.toFile().listFiles((FilenameFilter) new RegexFileFilter(fileNamePattern));
		Arrays.sort(files, Comparator.comparing(File::getName));

		// on envoie chaque graphe dans le worker
		final GentilIterator<File> fileIterator = new GentilIterator<>(Arrays.asList(files));
		while (fileIterator.hasNext()) {
			final File file = fileIterator.next();
			final Graphe graphe = deserialize(file);
			worker.onGraphe(graphe);

			// un peu de monitoring
			if (fileIterator.isAtNewPercent()) {
				progressMonitor.setPercentProgress(fileIterator.getPercent());
			}

			// en cas de demande d'arrêt du programme, le thread de feed est interrompu
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
		}
	}

	/**
	 * @param is un flux d'entrée
	 * @return le graphe contenu dans ce flux sous forme sérialisée
	 * @throws IOException en cas de souci lors de l'accès au flux
	 * @throws ClassNotFoundException si les données sérialisées ne sont pas récupérables...
	 */
	public static Graphe deserialize(InputStream is) throws IOException, ClassNotFoundException {
		try (BufferedInputStream bis = new BufferedInputStream(is);
		     ObjectInputStream ois = new ObjectInputStream(bis)) {

			return (Graphe) ois.readObject();
		}
	}

	/**
	 * @param file un fichier sur le disque
	 * @return le graphe contenu dans ce fichier sous forme sérialisée
	 * @throws IOException en cas de souci lors de l'accès au fichier
	 * @throws ClassNotFoundException si les données sérialisées ne sont pas récupérables...
	 */
	public static Graphe deserialize(File file) throws IOException, ClassNotFoundException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return deserialize(fis);
		}
	}

	/**
	 * @param graphe un graphe à sérialiser
	 * @param os un flux de sortie
	 * @throws IOException en cas de souci lors de l'accès au flux
	 */
	public static void serialize(Graphe graphe, OutputStream os) throws IOException {
		try (BufferedOutputStream bos = new BufferedOutputStream(os);
		     ObjectOutputStream oos = new ObjectOutputStream(bos)) {

			oos.writeObject(graphe);
			oos.flush();
		}
	}

	/**
	 * @param graphe un graphe à sérialiser
	 * @param file un fichier sur le disque (sera écrasé)
	 * @throws IOException en cas de souci lors de l'accès au fichier
	 */
	public static void serialize(Graphe graphe, File file) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
		     serialize(graphe, fos);
		}
	}

	@Override
	public void onGraphe(Graphe graphe) throws IOException {
		// sérialization du graphe dans un fichier local
		final File file = buildNewFile();
		serialize(graphe, file);

		// on garde trace de tout ça pour savoir ce que contient chaque fichier dumpé
		fileMap.put(file, new Ids(graphe));

		// [SIFISC-18274] on conserve les données de l'appariement des entreprises
		final Map<Long, Set<Long>> map = graphe.getEntreprises().values().stream()
				.filter(e -> e.getNumeroCantonal() != null)
				.collect(Collectors.toMap(RegpmEntreprise::getNumeroCantonal,
				                          e -> Collections.singleton(e.getId()),
				                          (s1, s2) -> Stream.concat(s1.stream(), s2.stream()).collect(Collectors.toSet()),
				                          TreeMap::new));
		map.entrySet().forEach(entry -> {
			final Set<Long> set = idCantonalMap.computeIfAbsent(entry.getKey(), key -> new TreeSet<>());
			set.addAll(entry.getValue());
		});
	}

	@Override
	public void feedingOver() throws IOException {
		// dump d'un fichier d'index pour s'y retrouver un peu si nécessaire
		final File indexFile = fsDirectory.resolve("index.txt").toFile();
		try (FileOutputStream fos = new FileOutputStream(indexFile);
		     PrintStream ps = new PrintStream(fos, false, "UTF-8")) {

			// dump de toutes les entrées
			fileMap.entrySet().stream()
					.map(entry -> String.format("%s : %s", entry.getKey().getName(), entry.getValue()))
					.forEach(ps::println);
		}

		// et dump d'un fichier des appariements d'entreprises
		final File appariementFile = fsDirectory.resolve("appariements.csv").toFile();
		try (FileOutputStream fos = new FileOutputStream(appariementFile);
			 PrintStream ps = new PrintStream(fos, false, "UTF-8")) {

			// dump des noms des colonnes, d'abord...
			ps.println("NO_CANTONAL;NB_ENTREPRISES;NOS_ENTREPRISES");

			// dump de tous les appariements, sur trois colonnes
			idCantonalMap.entrySet().stream()
					.map(entry -> String.format("%d;%d;%s", entry.getKey(), entry.getValue().size(), CollectionsUtils.toString(entry.getValue(), String::valueOf, ",")))
					.forEach(ps::println);
		}
	}
}
