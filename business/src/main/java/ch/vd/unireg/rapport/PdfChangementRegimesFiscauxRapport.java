package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.regimefiscal.changement.ChangementRegimesFiscauxJobResults;

/**
 * Classe de génèration du rapport PDF contenant les résultats de l'exécution du job de changement des régimes fiscaux.
 */
public class PdfChangementRegimesFiscauxRapport extends PdfRapport {

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
	public void write(final ChangementRegimesFiscauxJobResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de changement des régimes fiscaux");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Ancien type:", results.getAncienType().getLibelleAvecCode());
				table.addLigne("Nouveau type:", results.getNouveauType().getLibelleAvecCode());
				table.addLigne("Date de changement:", RegDateHelper.dateToDisplayString(results.getDateChangement()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n" + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre total d'entreprises inspectées:", String.valueOf(results.getTotal()));
				table.addLigne("Nombre d'entreprise traitées:", String.valueOf(results.getTraites().size()));
				table.addLigne("Nombre d'erreurs:", String.valueOf(results.getErreurs().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// Questionnaires échus
		{
			String filename = "traites.csv";
			String titre = "Liste des entreprises traitées";
			String listVide = "(aucune entreprise traitée)";
			try (TemporaryFile contenu = traitesAsCsvFile(results.getTraites(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Questionnaires en erreur
		{
			String filename = "erreurs.csv";
			String titre = "Liste des questionnaires en erreur";
			String listVide = "(aucun questionnaire en erreur)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile traitesAsCsvFile(List<ChangementRegimesFiscauxJobResults.TraiteInfo> traites, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = traites.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<ChangementRegimesFiscauxJobResults.TraiteInfo>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ENTREPRISE_ID").append(COMMA).append("RAISON_SOCIALE").append(COMMA).append("DATE_CREATION");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ChangementRegimesFiscauxJobResults.TraiteInfo info) {
					b.append(info.entrepriseId).append(COMMA);
					b.append(escapeChars(info.raisonSociale)).append(COMMA);
					b.append(info.dateDeCreation == null ? "" : info.dateDeCreation.index());
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<ChangementRegimesFiscauxJobResults.ErreurInfo> traites, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = traites.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(traites, filename, status, new CsvHelper.FileFiller<ChangementRegimesFiscauxJobResults.ErreurInfo>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ENTREPRISE_ID").append(COMMA).append("RAISON_SOCIALE").append(COMMA).append("DATE_CREATION").append(COMMA).append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ChangementRegimesFiscauxJobResults.ErreurInfo info) {
					b.append(info.entrepriseId).append(COMMA);
					b.append(escapeChars(info.raisonSociale)).append(COMMA);
					b.append(info.dateDeCreation == null ? "" : info.dateDeCreation.index()).append(COMMA);
					b.append(escapeChars(info.message));
					return true;
				}
			});
		}
		return contenu;
	}
}