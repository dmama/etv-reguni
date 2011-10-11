package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.registrefoncier.ImportImmeublesResults;

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

			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre d'immeubles lus :", String.valueOf(results.getNbImmeubles()));
					table.addLigne("Nombre d'immeubles importés :", String.valueOf(results.traites.size()));
					table.addLigne("Nombre d'immeubles ignorés :", String.valueOf(results.ignores.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(results));
					table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
				}
			});
		}

		// Cas traités
		{
			final String filename = "immeubles_importes.csv";
			final String contenu = buildContenuTraites(results.traites, status, filename);
			final String titre = "Liste des immeubles importés";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.traites.size(), titre, listVide, filename, contenu);
		}

		// Cas ignorés
		{
			final String filename = "immeubles_ignores.csv";
			final String contenu = buildContenuIgnores(results.ignores, status, filename);
			final String titre = "Liste des contribuables dont le code de segmentation n'a pas été touché";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.ignores.size(), titre, listVide, filename, contenu);
		}

		// Erreurs
		{
			final String filename = "immeubles_en_erreurs.csv";
			final String contenu = buildContenuErreurs(results.erreurs, status, filename);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, results.erreurs.size(), titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private String buildContenuTraites(List<ImportImmeublesResults.Import> traites, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(traites, filename, status, 25, new CsvHelper.Filler<ImportImmeublesResults.Import>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NO_IMMEUBLE").append(COMMA).append("NO_CONTRIBUABLE");
			}

			@Override
			public void fillLine(StringBuilder b, ImportImmeublesResults.Import data) {
				b.append(data.getNoImmeuble()).append(COMMA).append(data.getNoContribuable());
			}
		});
	}

	private String buildContenuIgnores(List<ImportImmeublesResults.Ignore> ignores, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(ignores, filename, status, 100, new CsvHelper.Filler<ImportImmeublesResults.Ignore>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NO_IMMEUBLE").append(COMMA).append("RAISON");
			}

			@Override
			public void fillLine(StringBuilder b, ImportImmeublesResults.Ignore elt) {
				b.append(elt.noImmeuble).append(COMMA).append(CsvHelper.escapeChars(elt.raison.description()));
			}
		});
	}

	private String buildContenuErreurs(List<ImportImmeublesResults.Erreur> erreurs, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(erreurs, filename, status, 100, new CsvHelper.Filler<ImportImmeublesResults.Erreur>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NO_IMMEUBLE").append(COMMA).append("ERREUR").append(COMMA).append("DETAILS");
			}

			@Override
			public void fillLine(StringBuilder b, ImportImmeublesResults.Erreur elt) {
				b.append(elt.noImmeuble).append(COMMA).append(CsvHelper.escapeChars(elt.raison.description())).append(COMMA);
				b.append(CsvHelper.escapeChars(elt.details));
			}
		});
	}
}
