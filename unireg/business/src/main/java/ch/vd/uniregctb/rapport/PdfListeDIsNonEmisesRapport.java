package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.declaration.ordinaire.pp.ListeDIsPPNonEmises;


/**
 * Rapport PDF contenant la liste des DIs non émises.
 */
public class PdfListeDIsNonEmisesRapport extends PdfRapport {

	public void write(final ListeDIsPPNonEmises results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfListeDIsNonEmisesRapport document = new PdfListeDIsNonEmisesRapport();
		PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal("Liste des DIs non émises pour l'année " + results.annee);

		// Paramètres
		document.addEntete1("Paramètres");
		document.addTableSimple(2, table -> {
			table.addLigne("Période fiscale considérée: ", String.valueOf(results.annee));
			table.addLigne("Date de traitement: ", RegDateHelper.dateToDisplayString(results.dateTraitement));
		});
		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.interrompu) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(2, table -> {
				table.addLigne("Nombre total de contribuables sans DI: ", String.valueOf(results.getNombreDeDIsNonEmises()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport: ", formatTimestamp(dateGeneration));
			});
		}
		{
			String filename = "contribuables_sans_DI.csv";
			String titre = "Liste des contribuables traités";
			String listVide = "(aucun contribuable traité)";
			try (TemporaryFile contenu = asCsvFile(results, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile asCsvFile(ListeDIsPPNonEmises results, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		List<ListeDIsPPNonEmises.LigneRapport> list = results.getLignes();
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<ListeDIsPPNonEmises.LigneRapport>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de contribuale").append(COMMA);
					b.append("Date de début").append(COMMA);
					b.append("Date de fin").append(COMMA);
					b.append("Raison").append(COMMA);
					b.append("Détails");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ListeDIsPPNonEmises.LigneRapport ligne) {
					b.append(ligne.getNbCtb()).append(COMMA);
					b.append(ligne.getDateDebut()).append(COMMA);
					b.append(ligne.getDateFin()).append(COMMA);
					b.append(escapeChars(ligne.getRaison())).append(COMMA);
					b.append(escapeChars(ligne.getDetails()));
					return true;
				}
			});
		}
		return contenu;
	}
}
