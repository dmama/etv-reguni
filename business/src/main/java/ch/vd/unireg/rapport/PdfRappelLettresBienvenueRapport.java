package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.documentfiscal.RappelLettresBienvenueResults;

public class PdfRappelLettresBienvenueRapport extends PdfRapport {

	public void write(final RappelLettresBienvenueResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

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
				table.addLigne("Nombre total de lettres inspectées :", String.valueOf(results.getErreurs().size() + results.getTraites().size() + results.getIgnores().size()));
				table.addLigne("Nombre de rappels envoyés :", String.valueOf(results.getTraites().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Nombre de lettres ignorées :", String.valueOf(results.getIgnores().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// Rappels envoyés
		{
			String filename = "rappels.csv";
			String titre = "Liste des rappels de lettres de bienvenue envoyés";
			String listVide = "(aucun)";
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
			String titre = "Liste des lettres de bienvenue ignorées";
			String listVide = "(aucune)";
			try (TemporaryFile contenu = asCsvFileIgnorees(results.getIgnores(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile asCsvFileEnvois(List<RappelLettresBienvenueResults.Traite> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<RappelLettresBienvenueResults.Traite>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("DATE_ENVOI_INITIAL");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RappelLettresBienvenueResults.Traite elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.dateEnvoiLettre);
				return true;
			}
		});
	}

	private TemporaryFile asCsvFileIgnorees(List<RappelLettresBienvenueResults.Ignore> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<RappelLettresBienvenueResults.Ignore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("DATE_ENVOI_INITIAL").append(COMMA);
				b.append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RappelLettresBienvenueResults.Ignore elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.dateEnvoi).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.raison.libelle));
				return true;
			}
		});
	}

	private TemporaryFile asCsvFileErreurs(List<RappelLettresBienvenueResults.EnErreur> traites, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<RappelLettresBienvenueResults.EnErreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("ID_LETTRE").append(COMMA);
				b.append("ERREUR");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, RappelLettresBienvenueResults.EnErreur elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.idLettre).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.msg));
				return true;
			}
		});
	}
}
