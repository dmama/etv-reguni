package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des DIs.
 */
public class PdfEchoirDIsRapport extends PdfRapport {

	public void write(final EchoirDIsResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de passage des DIs sommées à l'état échu");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
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

			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total de déclarations inspectées:", String.valueOf(results.nbDIsTotal));
					table.addLigne("Nombre de déclarations passées dans l'état échu:", String.valueOf(results.disEchues.size()));
					table.addLigne("Nombre de déclarations ignorées:", String.valueOf(results.disIgnorees.size()));
					table.addLigne("Nombre de déclarations en erreur:", String.valueOf(results.disEnErrors.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// DIs échues
		{
			String filename = "dis_echues.csv";
			String contenu = disEchuesAsCsvFile(results.disEchues, filename, status);
			String titre = "Liste des déclarations nouvellement échues";
			String listVide = "(aucun déclaration échue)";
			addListeDetaillee(writer, results.disEchues.size(), titre, listVide, filename, contenu);
		}

		// DIs ignorées
		{
			String filename = "dis_ignorees.csv";
			String contenu = asCsvFile(results.disIgnorees, filename, status);
			String titre = "Liste des déclarations ignorées";
			String listVide = "(aucun déclaration ignorée)";
			addListeDetaillee(writer, results.disIgnorees.size(), titre, listVide, filename, contenu);
		}

		// DIs en erreur
		{
			String filename = "dis_en_erreur.csv";
			String contenu = asCsvFile(results.disEnErrors, filename, status);
			String titre = "Liste des déclarations en erreur";
			String listVide = "(aucun déclaration en erreur)";
			addListeDetaillee(writer, results.disEnErrors.size(), titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}


	private String disEchuesAsCsvFile(List<EchoirDIsResults.Echue> disEchues, String filename, StatusManager status) {
		String contenu = null;
		int size = disEchues.size();
		if (size > 0) {

			StringBuilder b = new StringBuilder(AVG_LINE_LEN * disEchues.size());
			b.append("Numéro de l'office d'impôt").append(COMMA).append("Numéro de contribuable").append(COMMA).append(
					"Numéro de la déclaration\n");

			final GentilIterator<EchoirDIsResults.Echue> iter = new GentilIterator<EchoirDIsResults.Echue>(disEchues);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				EchoirDIsResults.Echue info = iter.next();
				StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
				bb.append(info.officeImpotID).append(COMMA);
				bb.append(info.ctbId).append(COMMA);
				bb.append(info.diId);
				bb.append('\n');

				b.append(bb);
			}
			contenu = b.toString();
		}
		return contenu;
	}

}