package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.rf.ImportImmeublesResults;

public class PdfImportImmeublesRapport extends PdfRapport {

	public void write(final ImportImmeublesResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'import des immeubles du registre foncier");

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre d'immeubles lus :", String.valueOf(results.getNbImmeubles()));
				table.addLigne("Nombre d'immeubles importés :", String.valueOf(results.traites.size()));
				table.addLigne("Nombre d'immeubles ignorés :", String.valueOf(results.ignores.size()));
				table.addLigne("Nombre d'immeubles à vérifier :", String.valueOf(results.averifier.size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
				table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
			});
		}

		// Cas traités
		{
			final String filename = "immeubles_importes.csv";
			final String titre = "Liste des immeubles importés";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = buildContenuTraites(results.traites, status, filename)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas ignorés
		{
			final String filename = "immeubles_ignores.csv";
			final String titre = "Liste des immeubles ignorés";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = buildContenuIgnores(results.ignores, status, filename)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// A vérifier
		{
			final String filename = "immeubles_a_verifier.csv";
			final String titre = "Liste des immeubles qui doivent être vérifiés dans le registre foncier";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = buildContenuAVerifier(results.averifier, status, filename)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "immeubles_en_erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = buildContenuErreurs(results.erreurs, status, filename)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile buildContenuTraites(List<ImportImmeublesResults.Import> traites, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<ImportImmeublesResults.Import>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_IMMEUBLE").append(COMMA).append("NO_CONTRIBUABLE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ImportImmeublesResults.Import data) {
				b.append(data.getNoImmeuble()).append(COMMA).append(data.getNoContribuable());
				return true;
			}
		});
	}

	private TemporaryFile buildContenuIgnores(List<ImportImmeublesResults.Ignore> ignores, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(ignores, filename, status, new CsvHelper.FileFiller<ImportImmeublesResults.Ignore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_IMMEUBLE").append(COMMA).append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ImportImmeublesResults.Ignore elt) {
				b.append(elt.noImmeuble).append(COMMA).append(CsvHelper.escapeChars(elt.raison.description()));
				return true;
			}
		});
	}

	private TemporaryFile buildContenuAVerifier(List<ImportImmeublesResults.AVerifier> averifiers, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(averifiers, filename, status, new CsvHelper.FileFiller<ImportImmeublesResults.AVerifier>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_IMMEUBLE").append(COMMA).append("RAISON").append(COMMA).append("DETAILS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ImportImmeublesResults.AVerifier elt) {
				b.append(elt.noImmeuble).append(COMMA).append(CsvHelper.escapeChars(elt.raison.description())).append(COMMA).append(CsvHelper.escapeChars(elt.details));
				return true;
			}
		});
	}

	private TemporaryFile buildContenuErreurs(List<ImportImmeublesResults.Erreur> erreurs, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(erreurs, filename, status, new CsvHelper.FileFiller<ImportImmeublesResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_IMMEUBLE").append(COMMA).append("ERREUR").append(COMMA).append("DETAILS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ImportImmeublesResults.Erreur elt) {
				b.append(elt.noImmeuble).append(COMMA).append(CsvHelper.escapeChars(elt.raison.description())).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.details));
				return true;
			}
		});
	}
}
