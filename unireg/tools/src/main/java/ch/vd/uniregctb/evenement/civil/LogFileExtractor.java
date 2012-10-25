package ch.vd.uniregctb.evenement.civil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Classe capable d'interprêter les logs d'unireg pour en extraire les événements civils reçus par l'application
 * et ainsi être peut-être les rejouer...
 */
public class LogFileExtractor {

	public static final class EvenementRecu {
		final long id;
		final RegDate dateEvement;
		final TypeEvenementCivil type;
		final long noIndividu;
		final int ofsAnnonce;

		public EvenementRecu(long id, RegDate dateEvement, TypeEvenementCivil type, long noIndividu, int ofsAnnonce) {
			this.id = id;
			this.dateEvement = dateEvement;
			this.type = type;
			this.noIndividu = noIndividu;
			this.ofsAnnonce = ofsAnnonce;
		}
	}

	private final List<EvenementRecu> evenementsRecus = new LinkedList<EvenementRecu>();

	public LogFileExtractor(String fileName) throws FileNotFoundException {

		/*
		 * Dans un fichier de log (partie web), les événements civils qui arrivent par la queue sont
		 * indiqués de deux manières :
		 * 1. pour les événements ignorés
		 *      [unireg] INFO  [2010-09-06 13:37:56.083] [tainer-1] Audit.logAuditLine(160) | [AUDIT] Arrivée d'un message JMS ignoré (id 3970479, code 0)
		 *      [unireg] INFO  [2010-09-06 13:38:18.771] [tainer-1] Audit.logAuditLine(160) | [AUDIT] Arrivée d'un message JMS ignoré (id 3970480, code 0)
		 *      [unireg] INFO  [2010-09-06 13:38:48.744] [tainer-1] Audit.logAuditLine(160) | [AUDIT] Arrivée d'un message JMS ignoré (id 3970481, code 0)
		 *
		 * 2. pour les événements civils pris en compte
		 *      [unireg] INFO  [2010-09-06 13:40:51.473] [tainer-1] Audit.logAuditLine(160) | [AUDIT] L'événement civil 3970486 est inséré en base de données {id=3970486, type=CORREC_DATE_NAISSANCE, date=1962-10-30, no individu=741055, OFS commune=0}.
		 *      [unireg] INFO  [2010-09-06 13:46:34.451] [tainer-1] Audit.logAuditLine(160) | [AUDIT] L'événement civil 3970487 est inséré en base de données {id=3970487, type=CORREC_DATE_NAISSANCE, date=1999-07-27, no individu=531947, OFS commune=0}.
		 *      [unireg] INFO  [2010-09-06 13:49:23.331] [tainer-1] Audit.logAuditLine(160) | [AUDIT] L'événement civil 3970488 est inséré en base de données {id=3970488, type=CORREC_DATE_NAISSANCE, date=1945-01-02, no individu=710741, OFS commune=0}.
		 *      [unireg] INFO  [2010-09-06 13:50:50.732] [tainer-1] Audit.logAuditLine(160) | [AUDIT] L'événement civil 3970489 est inséré en base de données {id=3970489, type=CORREC_DATE_NAISSANCE, date=2007-06-21, no individu=852906, OFS commune=0}.
		 */

		final File file = new File(fileName);
		if (!file.exists()) {
			throw new FileNotFoundException(String.format("Fichier introuvable : '%s'", fileName));
		}
		if (!file.canRead()) {
			throw new FileNotFoundException(String.format("Le fichier '%s' n'est pas accessible en lecture", fileName));
		}

		final Pattern evenementCivilIgnorePattern = Pattern.compile(".*Arrivée d'un message JMS ignoré .id ([0-9]+), code ([0-9]+).*");
		final Pattern evenementCivilTraitePattern = Pattern.compile(".*id=([0-9]+), type=([A-Z_]+), date=([0-9]{4})-([0-9]{2})-([0-9]{2}), no individu=([0-9]+), OFS commune=([0-9]+).*");

		final Scanner scanner = new Scanner(file);
		try {
			while (scanner.hasNextLine()) {
				final String line = scanner.nextLine();
				final Matcher matcherIgnore = evenementCivilIgnorePattern.matcher(line);
				if (matcherIgnore.matches()) {
					final long id = Long.parseLong(matcherIgnore.group(1));
					final int codeType = Integer.parseInt(matcherIgnore.group(2));
					final TypeEvenementCivil type = TypeEvenementCivil.valueOf(codeType);

					final EvenementRecu evt = new EvenementRecu(id, RegDate.get(), type, 1234567890L, 0);
					evenementsRecus.add(evt);
				}
				else {
					final Matcher matcherTraite = evenementCivilTraitePattern.matcher(line);
					if (matcherTraite.matches()) {
						final long id = Long.parseLong(matcherTraite.group(1));
						final String stringType = matcherTraite.group(2);
						final int dateIndex = Integer.parseInt(String.format("%s%s%s", matcherTraite.group(3), matcherTraite.group(4), matcherTraite.group(5)));
						final long noIndividu = Long.parseLong(matcherTraite.group(6));
						final int ofsCommune = Integer.parseInt(matcherTraite.group(7));
						final TypeEvenementCivil type = TypeEvenementCivil.valueOf(stringType);

						final EvenementRecu evt = new EvenementRecu(id, RegDate.fromIndex(dateIndex, false), type, noIndividu, ofsCommune);
						evenementsRecus.add(evt);
					}
				}
			}
		}
		finally {
			scanner.close();
		}
	}

	public List<EvenementRecu> getEvenementsRecus() {
		return evenementsRecus;
	}
}
