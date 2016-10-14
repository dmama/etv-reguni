package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.mutable.MutableInt;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.documentfiscal.EnvoiLettresBienvenueResults;
import ch.vd.uniregctb.type.TypeLettreBienvenue;

public class PdfEnvoiLettresBienvenueRapport extends PdfRapport {

	public void write(final EnvoiLettresBienvenueResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de l'envoi des lettres de bienvenue");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(new float[]{.7f, .3f}, table -> {
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
				table.addLigne("Délai de carence (jours) :", Integer.toString(results.delaiCarence));
				table.addLigne("Date seuil pour les nouveaux assujettissements :", RegDateHelper.dateToDisplayString(results.dateOrigine));
				table.addLigne("Taille minimale (jours) du trou d'assujettissement pour une nouvelle lettre :", Integer.toString(results.tailleMinimaleTrouAssujettissement));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre total d'entreprises inspectées :", String.valueOf(results.getErreurs().size() + results.getTraites().size() + results.getIgnores().size()));
				table.addLigne("Nombre de lettres envoyées :", String.valueOf(results.getTraites().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Nombre d'entreprises ignorées :", String.valueOf(results.getIgnores().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// Détails de types de lettres envoyées
		if (!results.getTraites().isEmpty()) {
			addEntete1("Détail des envois");

			final Map<TypeLettreBienvenue, MutableInt> map = buildStatsEnvois(results.getTraites());
			addTableSimple(2, table -> {
				for (Map.Entry<TypeLettreBienvenue, MutableInt> entry : map.entrySet()) {
					table.addLigne(entry.getKey().name(), entry.getValue().toString());
				}
			});
		}

		// Lettres envoyées
		{
			String filename = "envois.csv";
			String titre = "Liste des lettres de bienvenue envoyées";
			String listVide = "(aucune)";
			try (TemporaryFile contenu = asCsvFileEnvois(results.getTraites(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			String filename = "erreurs.csv";
			String titre = "Liste des erreurs";
			String listVide = "(aucune erreur)";
			try (TemporaryFile contenu = asCsvFileErreurs(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Ignorés
		{
			String filename = "ignorees.csv";
			String titre = "Liste des entreprise ignorées";
			String listVide = "(aucune)";
			try (TemporaryFile contenu = asCsvFileIgnorees(results.getIgnores(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static Map<TypeLettreBienvenue, MutableInt> buildStatsEnvois(List<EnvoiLettresBienvenueResults.Traite> traites) {
		final Map<TypeLettreBienvenue, MutableInt> map = new EnumMap<>(TypeLettreBienvenue.class);
		for (EnvoiLettresBienvenueResults.Traite traite : traites) {
			MutableInt compteur = map.get(traite.typeLettreEnvoyee);
			if (compteur == null) {
				compteur = new MutableInt();
				map.put(traite.typeLettreEnvoyee, compteur);
			}
			compteur.increment();
		}
		return map;
	}

	private TemporaryFile asCsvFileEnvois(List<EnvoiLettresBienvenueResults.Traite> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<EnvoiLettresBienvenueResults.Traite>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("TYPE_LETTRE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, EnvoiLettresBienvenueResults.Traite elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.typeLettreEnvoyee);
				return true;
			}
		});
	}

	private TemporaryFile asCsvFileIgnorees(List<EnvoiLettresBienvenueResults.Ignore> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<EnvoiLettresBienvenueResults.Ignore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, EnvoiLettresBienvenueResults.Ignore elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.raison.libelle));
				return true;
			}
		});
	}

	private TemporaryFile asCsvFileErreurs(List<EnvoiLettresBienvenueResults.EnErreur> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<EnvoiLettresBienvenueResults.EnErreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("ERREUR");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, EnvoiLettresBienvenueResults.EnErreur elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.msg));
				return true;
			}
		});
	}
}
