package ch.vd.uniregctb.perfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

/**
 * Celle classe interprète le fichier de log d'accès de host-interface et en extrait les ids des contribuables ainsi que - optionnellement -
 * les temps d'accès. Elle permet donc d'avoir les données nécessaires pour simuler une "charge" de production sur les web-services
 * d'Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PerfsAccessFile {

	/**
	 * Représente un appel à host-interface.
	 */
	public static class Call {

		/**
		 * le temps d'accès (relatif au premier accès de la liste)
		 */
		private long millisecondes;

		/**
		 * l'id du contribuable accédé
		 */
		private final long id;

		public Call(long millisecondes, long id) {
			this.millisecondes = millisecondes;
			this.id = id;
		}

		public void subMillisecondes(long ms) {
			millisecondes -= ms;
		}

		public long getMillisecondes() {
			return millisecondes;
		}

		public long getId() {
			return id;
		}
	}

	private final ArrayList<Call> calls = new ArrayList<Call>();

	/**
	 * Charge et interprète un fichier de log d'accès de host-interface.
	 * <p>
	 * Le format de ce fichier d'accès est : <b>[timestamp]\t[id]\t[duration]</b>. Exemple:
	 *
	 * <pre>
	 * 2008.12.02 00:10	13107801	405
	 * 2008.12.02 00:10	36106605	146
	 * 2008.12.02 00:10	36421407	382
	 * 2008.12.02 00:10	72124804	260
	 * 2008.12.02 00:10	90308101	388
	 * 2008.12.02 00:10	34002111	266
	 * </pre>
	 *
	 * La partie <i>duration</i> correspondant au temps d'exécution de host-interface et est ignorée.
	 *
	 * Optionnellement, le fichier peut simplement contenir une liste d'ids de contribuables (un par ligne). Exemple:
	 *
	 * <pre>
	 * 13107801
	 * 36106605
	 * 36421407
	 * 72124804
	 * 90308101
	 * 34002111
	 * </pre>
	 *
	 *
	 * @param filename
	 *            le chemin complet vers le fichier
	 */
	public PerfsAccessFile(String filename) throws FileNotFoundException, java.text.ParseException {

		File file = new File(filename);
		if (!file.exists()) {
			throw new FileNotFoundException("Le fichier '" + filename + "' n'existe pas.");
		}
		if (!file.canRead()) {
			throw new FileNotFoundException("Le fichier '" + filename + "' n'est pas lisible.");
		}

		SimpleDateFormat f = new SimpleDateFormat("yyyy.MM.dd HH:mm");

		// on parse le fichier
		Scanner s = new Scanner(file);
		try {
			while (s.hasNextLine()) {

				final String line = s.nextLine();
				if (line.trim().equals("")) {
					continue;
				}

				long time;
				long id;

				String[] tokens = line.split("\t");
				if (tokens.length == 1) {
					// format avec un numéro de contribuable par ligne
					String idAsString = tokens[0];

					time = 0;
					id = Long.valueOf(idAsString);
				}
				else {
					// format avec timestamp + numéro de contribuable par ligne
					String dateAsString = tokens[0];
					String idAsString = tokens[1];
					java.util.Date date = f.parse(dateAsString);

					time = date.getTime();
					id = Long.valueOf(idAsString);
				}

				calls.add(new Call(time, id));
			}
		}
		finally {
			s.close();
		}

		// les temps d'accès dans le fichier ne sont pas forcément triés, on le fait maintenant
		Collections.sort(calls, new Comparator<Call>() {
			public int compare(Call o1, Call o2) {
				if (o1.getMillisecondes() < o2.getMillisecondes()) {
					return -1;
				}
				else if (o1.getMillisecondes() > o2.getMillisecondes()) {
					return +1;
				}
				else {
					return 0;
				}
			}
		});

		// décale tous les temps d'accès de manière à ce que le premier accès soit à 0 millisecondes
		if (!calls.isEmpty()) {
			Call first = calls.get(0);
			long start = first.getMillisecondes();

			for (Call c : calls) {
				c.subMillisecondes(start);
			}
		}
	}

	/**
	 * Crée un fichier d'accès ne contenant qu'un seul contribuable.
	 *
	 * @param ctbId
	 */
	public PerfsAccessFile(long ctbId) {
		calls.add(new Call(0, ctbId));
	}

	public ArrayList<Call> getCalls() {
		return calls;
	}
}
