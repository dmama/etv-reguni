package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.foncier.migration.ifonc.MigrationExoIFONCImporterResults;

/**
 * Rapport PDF contenant les résultats du job de migration des demandes de dégrèvement de SIMPA-PM.
 */
public class PdfMigrationExonerationsIFONCRapport extends PdfRapport {

	public void write(final MigrationExoIFONCImporterResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la migration des exonérations IFONC de SIMPA-PM.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Nombre de threads :", String.valueOf(results.getNbThreads()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				table.addLigne("Nombre de lignes lues :", String.valueOf(results.getNbLignesLues()));
				table.addLigne("Nombre de lignes en erreur :", String.valueOf(results.getLignesEnErreur().size()));
				table.addLigne("Nombre d'exonérations traitées :", String.valueOf(results.getNbExonerationsTraitees()));
				table.addLigne("Nombre d'exonérations en erreur :", String.valueOf(results.getExonerationsEnErreur().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results.duration()));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Lignes en erreur
		{
			final String filename = "lignes_erreurs.csv";
			final String titre = " Liste des lignes en erreur";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = lignesAsCsvFile(results.getLignesEnErreur(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Exonérations en erreur
		{
			final String filename = "exonerations_erreurs.csv";
			final String titre = " Liste des exonérations en erreur";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = infosAsCsvFile(results.getExonerationsEnErreur(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile lignesAsCsvFile(List<MigrationExoIFONCImporterResults.LigneInfo> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<MigrationExoIFONCImporterResults.LigneInfo>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("INDEX").append(COMMA);
					b.append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MigrationExoIFONCImporterResults.LigneInfo elt) {
					b.append(elt.index).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.message));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile infosAsCsvFile(List<MigrationExoIFONCImporterResults.ExonerationInfo> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<MigrationExoIFONCImporterResults.ExonerationInfo>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("MESSAGE").append(COMMA);
					b.append("EXONERATION");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MigrationExoIFONCImporterResults.ExonerationInfo elt) {
					b.append(CsvHelper.asCsvField(elt.message)).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.exo.toString()));
					return true;
				}
			});
		}
		return contenu;
	}
}