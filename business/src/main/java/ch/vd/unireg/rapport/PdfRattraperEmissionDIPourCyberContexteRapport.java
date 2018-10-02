package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.ordinaire.common.RattraperEmissionDIPourCyberContexteResults;

/**
 * Rapport PDF d'exécution du batch de réémission des événements de mise-à-disposition des DIs dans le contexte de la cyberfiscalité.
 */
public class PdfRattraperEmissionDIPourCyberContexteRapport extends PdfRapport {

	public void write(final RattraperEmissionDIPourCyberContexteResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de réémission des événements de mise-à-disposition des DIs dans le contexte de la cyberfiscalité.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Première période fiscale :", String.valueOf(results.periodeDebut));
				table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre de déclarations trouvées :", String.valueOf(results.nbDIsTotal));
				table.addLigne("Nombre de déclarations ignorées :", String.valueOf(results.ignorees.size()));
				table.addLigne("Nombre de déclarations traitées :", String.valueOf(results.traites.size()));

				table.addLigne("Nombre d'erreurs :", String.valueOf(results.errors.size()));
				table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
			});
		}

		// Déclarations traitées
		{
			String filename = "traitees.csv";
			String titre = "Liste des déclarations traitées";
			String listVide = "(aucune déclaration traitée)";
			try (TemporaryFile contenu = traitesAsCsvFile(results.traites, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Déclarations ignorées
		{
			String filename = "ignorees.csv";
			String titre = "Liste des déclarations ignorées";
			String listVide = "(aucune déclaration ignorées)";
			try (TemporaryFile contenu = ignoreesAsCsvFile(results.ignorees, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			String filename = "erreurs.csv";
			String titre = "Liste des déclarations en erreur";
			String listVide = "(aucune déclaration en erreur)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.errors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile traitesAsCsvFile(List<RattraperEmissionDIPourCyberContexteResults.Traite> traites, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = traites.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<RattraperEmissionDIPourCyberContexteResults.Traite>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("DI_ID").append(COMMA).append("CTB_ID").append(COMMA).append("PERIODE").append(COMMA).append("NO_SEQUENCE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RattraperEmissionDIPourCyberContexteResults.Traite info) {
					b.append(info.diId).append(COMMA);
					b.append(info.ctbId).append(COMMA);
					b.append(info.periode).append(COMMA);
					b.append(info.noSequence);
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile ignoreesAsCsvFile(List<RattraperEmissionDIPourCyberContexteResults.Ignoree> ignorees, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = ignorees.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(ignorees, filename, status, new CsvHelper.FileFiller<RattraperEmissionDIPourCyberContexteResults.Ignoree>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("DI_ID").append(COMMA).append("CTB_ID").append(COMMA).append("PERIODE").append(COMMA).append("NO_SEQUENCE").append(COMMA).append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RattraperEmissionDIPourCyberContexteResults.Ignoree ignoree) {
					b.append(ignoree.diId).append(COMMA);
					b.append(ignoree.ctbId).append(COMMA);
					b.append(ignoree.periode).append(COMMA);
					b.append(ignoree.noSequence).append(COMMA);
					b.append(escapeChars(ignoree.message));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<RattraperEmissionDIPourCyberContexteResults.Erreur> traites, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = traites.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<RattraperEmissionDIPourCyberContexteResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("DI_ID").append(COMMA).append("CTB_ID").append(COMMA).append("PERIODE").append(COMMA).append("NO_SEQUENCE").append(COMMA).append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RattraperEmissionDIPourCyberContexteResults.Erreur erreur) {
					b.append(erreur.diId).append(COMMA);
					b.append(erreur.ctbId).append(COMMA);
					b.append(erreur.periode).append(COMMA);
					b.append(erreur.noSequence).append(COMMA);
					b.append(escapeChars(erreur.message));
					return true;
				}
			});
		}
		return contenu;
	}
}