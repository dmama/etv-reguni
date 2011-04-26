package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.DeterminationDIsResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des DIs.
 */
public class PdfDeterminationDIsRapport extends PdfRapport {

	public void write(final DeterminationDIsResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de détermination des DIs à émettre pour l'année " + results.annee);

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Période fiscale considérée:", String.valueOf(results.annee));
					table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
				}
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
					table.addLigne("Nombre de contribuables traités:", String.valueOf(results.traites.size()));
					table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ignores.size()));
					table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// CTBs traités
		{
			String filename = "contribuables_traites.csv";
			String contenu = traitesAsCsvFile(results.traites, filename, status);
			String titre = "Liste des contribuables traités";
			String listVide = "(aucun contribuable traité)";
			addListeDetaillee(writer, results.traites.size(), titre, listVide, filename, contenu);
		}

		// CTBs ignorés
		{
			String filename = "contribuables_ignores.csv";
			String contenu = asCsvFile(results.ignores, filename, status);
			String titre = "Liste des contribuables ignorés";
			String listVide = "(aucun contribuable ignoré)";
			addListeDetaillee(writer, results.ignores.size(), titre, listVide, filename, contenu);
		}

		// CTBs en erreurs
		{
			String filename = "contribuables_en_erreur.csv";
			String contenu = asCsvFile(results.erreurs, filename, status);
			String titre = "Liste des contribuables en erreur";
			String listVide = "(aucun contribuable en erreur)";
			addListeDetaillee(writer, results.erreurs.size(), titre, listVide, filename, contenu);
		}

		// HeaderFooter header = new HeaderFooter(new Phrase("This is a header."), false);
		// setHeader(header);
		// setFooter(new HeaderFooter(new Phrase("Rapport du job de détermination des DIs à émettre - Page"), new Phrase(
		// " - généré le " + new SimpleDateFormat("dd.MM.yyy k:m:s").format(new Date()))));

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String traitesAsCsvFile(List<DeterminationDIsResults.Traite> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {

		    StringBuilder b = new StringBuilder(AVG_LINE_LEN * list.size());
			b.append("Numéro de l'office d'impôt");
			b.append(COMMA);
			b.append("Numéro de contribuable");
			b.append(COMMA);
			b.append("Début de la période");
			b.append(COMMA);
			b.append("Fin de la période");
			b.append(COMMA);
			b.append("Raison\n");

			final GentilIterator<DeterminationDIsResults.Traite> iter = new GentilIterator<DeterminationDIsResults.Traite>(list);
		    while (iter.hasNext()) {
		        if (iter.isAtNewPercent()) {
		            status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
		        }

		        DeterminationDIsResults.Traite info = iter.next();
		        StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
		        bb.append(info.officeImpotID).append(COMMA);
		        bb.append(info.noCtb).append(COMMA);
		        bb.append(info.dateDebut).append(COMMA);
			    bb.append(info.dateFin).append(COMMA);
		        bb.append(info.raison.description());
		        bb.append('\n');

		        b.append(bb);
		    }
		    contenu = b.toString();
		}
		return contenu;
	}
}