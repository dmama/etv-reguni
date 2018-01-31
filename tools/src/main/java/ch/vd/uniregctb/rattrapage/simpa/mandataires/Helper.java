package ch.vd.uniregctb.rattrapage.simpa.mandataires;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

public abstract class Helper {

	public interface Parser<T> {
		T parse(String string) throws ParseException;
	}

	public static <T> List<T> loadFile(String filename, Reader reader, Parser<T> parser) throws IOException {
		final List<T> resultat = new LinkedList<>();
		try (BufferedReader br = new BufferedReader(reader)) {
			String line;
			while ((line = br.readLine()) != null) {
				try {
					// on ignore les commentaires... (= lignes qui commencent par un '#')
					if (!line.startsWith("#")) {
						resultat.add(parser.parse(line));
					}
				}
				catch (Exception e) {
					System.err.println("Fichier " + filename + " : ligne '" + line + "' ignorée (" + e.getMessage() + ").");
				}
			}
		}
		return resultat;
	}

}
