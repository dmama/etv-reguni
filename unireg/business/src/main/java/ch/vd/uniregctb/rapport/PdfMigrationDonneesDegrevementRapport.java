package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.foncier.migration.ici.MigrationDDImporterResults;

/**
 * Rapport PDF contenant les résultats du job de migration des données de dégrèvement de SIMPA-PM.
 */
public class PdfMigrationDonneesDegrevementRapport extends PdfRapport {

	public void write(final MigrationDDImporterResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la migration des données de dégrèvement de SIMPA-PM.");

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
				table.addLigne("Nombre de lignes lues :", String.valueOf(results.getNbLignes()));
				table.addLigne("Nombre de lignes en erreur :", String.valueOf(results.getLignesEnErreur().size()));
				table.addLigne("Nombre de demandes traitées :", String.valueOf(results.getNbDemandesTraitees()));
				table.addLigne("Nombre de données ignorées :", String.valueOf(results.getDonneesIgnorees().size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results.endTime - results.startTime));
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

		// Demandes ignorées
		{
			final String filename = "donnees_ignorees.csv";
			final String titre = " Liste des données ignorées";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = ignoresAsCsvFile(results.getDonneesIgnorees(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Demandes en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des erreurs";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile lignesAsCsvFile(List<MigrationDDImporterResults.LigneInfo> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<MigrationDDImporterResults.LigneInfo>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("INDEX").append(COMMA);
					b.append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MigrationDDImporterResults.LigneInfo elt) {
					b.append(elt.getIndex()).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.getMessage()));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile ignoresAsCsvFile(List<MigrationDDImporterResults.Ignore> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<MigrationDDImporterResults.Ignore>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("MESSAGE").append(COMMA);
					b.append("LIEN_CTB_IMMEUBLE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MigrationDDImporterResults.Ignore elt) {
					b.append(CsvHelper.asCsvField(elt.getMessage())).append(COMMA);
					b.append(elt.getKey().toString());
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<MigrationDDImporterResults.Erreur> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<MigrationDDImporterResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MigrationDDImporterResults.Erreur elt) {
					b.append(CsvHelper.asCsvField(elt.getMessage()));
					return true;
				}
			});
		}
		return contenu;
	}
}