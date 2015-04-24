package ch.vd.uniregctb.migration.pm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.migration.pm.regpm.WithLongId;

/**
 * Intermédiaire de sérialisation
 */
public class SerializationIntermediary implements Worker, Feeder {

	private Path fsDirectory;
	private final AtomicInteger index = new AtomicInteger(0);
	private final Map<File, Ids> map = new TreeMap<>();

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
			Files.createDirectories(this.fsDirectory);
		}
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
		for (File file : files) {
			final Graphe graphe;
			try (FileInputStream fis = new FileInputStream(file);
			     BufferedInputStream bis = new BufferedInputStream(fis);
			     ObjectInputStream ois = new ObjectInputStream(bis)) {

				graphe = (Graphe) ois.readObject();
			}
			worker.onGraphe(graphe);
		}
	}

	@Override
	public void onGraphe(Graphe graphe) throws IOException {
		// sérialization du graphe dans un fichier local
		final File file = buildNewFile();
		try (FileOutputStream fos = new FileOutputStream(file);
		     BufferedOutputStream bos = new BufferedOutputStream(fos);
		     ObjectOutputStream oos = new ObjectOutputStream(bos)) {

			oos.writeObject(graphe);
			oos.flush();
		}

		// on garde trace de tout ça
		map.put(file, new Ids(graphe));
	}

	@Override
	public void feedingOver() throws IOException {
		// dump d'un fichier d'index pour s'y retrouver un peu si nécessaire
		final File indexFile = fsDirectory.resolve("index.txt").toFile();
		try (FileOutputStream fos = new FileOutputStream(indexFile);
		     PrintStream ps = new PrintStream(fos, false, "UTF-8")) {

			// dump de toutes les entrées
			map.entrySet().stream()
					.map(entry -> String.format("%s : %s", entry.getKey().getName(), entry.getValue()))
					.forEach(ps::println);
		}
	}
}
