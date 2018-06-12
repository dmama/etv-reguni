package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.snc.EchoirQuestionnairesSNCResults;

/**
 * Classe de génèration du rapport PDF contenant les résultats de l'exécution du job d'échéance des questionnaires SNC.
 */
public class PdfEchoirQSNCRapport extends PdfRapport {

	/**
	 * Génère le fichier PDF dans le {@link OutputStream} spécifié.
	 *
	 * @param results        les résultats du job
	 * @param nom            le nom du fichier (méta-info)
	 * @param description    une description du fichier (méta-info)
	 * @param dateGeneration la date de génération
	 * @param os             le stream dans lequel le PDF doit être généré
	 * @param status         le status manager de l'exécution du job
	 */
	public void write(final EchoirQuestionnairesSNCResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de passage des questionnaires SNC à l'état échu");

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
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre total de questionnaires inspectés:", String.valueOf(results.getTotal()));
				table.addLigne("Nombre de questionnaires passés dans l'état échu:", String.valueOf(results.getTraites().size()));
				table.addLigne("Nombre de questionnaires en erreur:", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// Questionnaires échus
		{
			String filename = "traites.csv";
			String titre = "Liste des questionnaires traités";
			String listVide = "(aucun questionnaire traité)";
			try (TemporaryFile contenu = qsncEchusAsCsvFile(results.getTraites(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Questionnaires en erreur
		{
			String filename = "erreurs.csv";
			String titre = "Liste des questionnaires en erreur";
			String listVide = "(aucun questionnaire en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}


	private TemporaryFile qsncEchusAsCsvFile(List<EchoirQuestionnairesSNCResults.Traite> disEchues, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = disEchues.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(disEchues, filename, status, new CsvHelper.FileFiller<EchoirQuestionnairesSNCResults.Traite>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("CTB_ID").append(COMMA).append("QSNC_ID").append(COMMA).append("DEBUT_PERIODE").append(COMMA).append("FIN_PERIODE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EchoirQuestionnairesSNCResults.Traite info) {
					b.append(info.ctbId).append(COMMA);
					b.append(info.sncId).append(COMMA);
					b.append(info.dateDebut.index()).append(COMMA);
					b.append(info.dateFin.index());
					return true;
				}
			});
		}
		return contenu;
	}
}