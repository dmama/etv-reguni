package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;

/**
 * Rapport PDF du batch qui liste les contribuables résidents suisses ou permis C sans for vaudois
 */
public class PdfListeContribuablesResidentsSansForVaudoisRapport extends PdfRapport {

	public void write(final ListeContribuablesResidentsSansForVaudoisResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job qui listes les contribuables résidents suisses ou titulaires d'un permis C sans for vaudois");

		// Résultats
		addEntete1("Résultats");
		{
		    if (results.isInterrompu()) {
		        addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
		                + "les valeurs ci-dessous sont donc incomplètes.");
		    }

		    addTableSimple(2, table -> {
		        table.addLigne("Nombre total de contribuables inspectés :", String.valueOf(results.getNombreContribuablesInspectes()));
		        table.addLigne("Nombre de contribuables identifiés :", String.valueOf(results.getContribuablesIdentifies().size()));
		        table.addLigne("Nombre de contribuables ignorés :", String.valueOf(results.getContribuablesIgnores().size()));
		        table.addLigne("Nombre d'erreurs :", String.valueOf(results.getListeErreurs().size()));
			    table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
		        table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
		    });
		}

		// contribuables cibles de ce job
		{
		    String filename = "ctbs_identifies.csv";
		    String titre = "Liste des contribuables identifiés";
			String listVide = "(aucun)";
			try (TemporaryFile contenu = buildListeContribuablesIdentifies(results.getContribuablesIdentifies(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// contribuables ignorés
		{
			String filename = "ctbs_ignores.csv";
			String titre = "Liste des contribuables ignorés";
			String listVide = "(aucun)";
			try (TemporaryFile contenu = buildContribuablesIgnores(results.getContribuablesIgnores(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// erreurs
		{
		    String filename = "ctbs_en_erreur.csv";
		    String titre = "Liste des erreurs";
			String listVide = "(aucune)";
			try (TemporaryFile contenu = buildErreurs(results.getListeErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile buildListeContribuablesIdentifies(List<Long> ctbIds, String filename, StatusManager status) {

		TemporaryFile contenu = null;
		if (!ctbIds.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(ctbIds, filename, status, new CsvHelper.FileFiller<Long>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, Long elt) {
					b.append(elt);
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile buildContribuablesIgnores(List<ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore> liste, String filename, StatusManager status) {

		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro").append(COMMA).append("Raison");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore info) {
					b.append(info.ctbId).append(COMMA);
					b.append(escapeChars(info.cause.getDescription()));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile buildErreurs(List<ListesResults.Erreur> liste, String filename, StatusManager status) {

		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<ListesResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro").append(COMMA);
					b.append("Raison").append(COMMA);
					b.append("Complément");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ListesResults.Erreur ligne) {
					b.append(ligne.noCtb).append(COMMA);
					b.append(escapeChars(ligne.getDescriptionRaison())).append(COMMA);
					b.append(escapeChars(ligne.details));
					return true;
				}
			});
		}
		return contenu;
	}
}
