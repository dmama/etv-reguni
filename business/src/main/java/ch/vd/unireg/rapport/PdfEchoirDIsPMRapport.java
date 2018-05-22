package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.ordinaire.pm.EchoirDIsPMResults;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des DIs PM.
 */
public class PdfEchoirDIsPMRapport extends PdfRapport {

	public void write(final EchoirDIsPMResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de passage des DIs PM sommées à l'état échu");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre total de déclarations inspectées:", String.valueOf(results.nbDIsTotal));
				table.addLigne("Nombre de déclarations passées dans l'état échu:", String.valueOf(results.disEchues.size()));
				table.addLigne("Nombre de déclarations en erreur:", String.valueOf(results.disEnErrors.size()));
				table.addLigne("Nombre de déclarations ignorées:", String.valueOf(results.disIgnorees.size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// DIs échues
		{
			final String filename = "dis_echues.csv";
			final String titre = "Liste des déclarations nouvellement échues";
			final String listVide = "(aucun déclaration échue)";
			try (TemporaryFile contenu = disEchuesAsCsvFile(results.disEchues, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// DIs ignorées
		{
			final String filename = "dis_ignorees.csv";
			final String titre = "Liste des déclarations ignorées";
			final String listVide = "(aucun déclaration ignorée)";
			try (TemporaryFile contenu = disIgnoreesAsCsvFile(results.disIgnorees, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// DIs en erreur
		{
			final String filename = "dis_en_erreur.csv";
			final String titre = "Liste des déclarations en erreur";
			final String listVide = "(aucun déclaration en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.disEnErrors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}


	private TemporaryFile disEchuesAsCsvFile(List<EchoirDIsPMResults.Echue> disEchues, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = disEchues.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(disEchues, filename, status, new CsvHelper.FileFiller<EchoirDIsPMResults.Echue>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("CTB_ID").append(COMMA).append("DI_ID").append(COMMA).append("DEBUT_PERIODE").append(COMMA).append("FIN_PERIODE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EchoirDIsPMResults.Echue info) {
					b.append(info.ctbId).append(COMMA);
					b.append(info.diId).append(COMMA);
					b.append(info.dateDebut.index()).append(COMMA);
					b.append(info.dateFin.index());
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile disIgnoreesAsCsvFile(List<EchoirDIsPMResults.Ignoree> disIgnorees, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = disIgnorees.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(disIgnorees, filename, status, new CsvHelper.FileFiller<EchoirDIsPMResults.Ignoree>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("CTB_ID").append(COMMA).append("DI_ID").append(COMMA).append("DEBUT_PERIODE").append(COMMA).append("FIN_PERIODE").append(COMMA).append("MOTIF");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EchoirDIsPMResults.Ignoree info) {
					b.append(info.ctbId).append(COMMA);
					b.append(info.diId).append(COMMA);
					b.append(info.dateDebut.index()).append(COMMA);
					b.append(info.dateFin.index()).append(COMMA);
					b.append(CsvHelper.escapeChars(info.motif.getDescription()));
					return true;
				}
			});
		}
		return contenu;
	}
}