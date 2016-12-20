package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.degrevement.migration.MigrationDDImporterResults;

/**
 * Rapport PDF contenant les résultats du job de migration des demandes de dégrèvement de SIMPA-PM.
 */
public class PdfMigrationDemandesDegrevementRapport extends PdfRapport {

	public void write(final MigrationDDImporterResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la migration des demandes de dégrèvement de SIMPA-PM.");

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
				table.addLigne("Nombre de lignes en erreur :", String.valueOf(results.getLignesEnErreurs().size()));
				table.addLigne("Nombre de demandes extraites :", String.valueOf(results.getNbDemandesExtraites()));
				table.addLigne("Nombre de demandes traitées :", String.valueOf(results.getNbDemandesTraitees()));
				table.addLigne("Nombre de demandes ignorées :", String.valueOf(results.getDemandesIgnorees().size()));
				table.addLigne("Nombre de demandes en erreurs :", String.valueOf(results.getDemandesEnErreurs().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results.endTime - results.startTime));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Lignes en erreur
		{
			final String filename = "lignes_erreurs.csv";
			final String titre = " Liste des lignes en erreur";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = lignesAsCsvFile(results.getLignesEnErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Demandes ignorées
		{
			final String filename = "demandes_ignorees.csv";
			final String titre = " Liste des demandes ignorées";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = infosAsCsvFile(results.getDemandesIgnorees(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Demanded en erreur
		{
			final String filename = "demandes_erreurs.csv";
			final String titre = " Liste des demandes en erreur";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = infosAsCsvFile(results.getDemandesEnErreurs(), filename, status)) {
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

	private TemporaryFile infosAsCsvFile(List<MigrationDDImporterResults.DemandeInfo> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<MigrationDDImporterResults.DemandeInfo>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("MESSAGE").append(COMMA);
					b.append("DEMANDE_DEGREVEMENT");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MigrationDDImporterResults.DemandeInfo elt) {
					b.append(CsvHelper.asCsvField(elt.getMessage())).append(COMMA);
					b.append(elt.getDd().toString());
					return true;
				}
			});
		}
		return contenu;
	}
}