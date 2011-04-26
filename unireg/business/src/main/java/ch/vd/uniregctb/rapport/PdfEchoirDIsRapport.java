package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults;

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
		final int size = disEchues.size();
		if (size > 0) {

			final StringBuilder b = new StringBuilder(100 * size);
			b.append("OID").append(COMMA).append("CTB_ID").append(COMMA).append("DI_ID").append(COMMA).append("DEBUT_PERIODE").append(COMMA).append("FIN_PERIODE").append('\n');

			final GentilIterator<EchoirDIsResults.Echue> iter = new GentilIterator<EchoirDIsResults.Echue>(disEchues);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				final EchoirDIsResults.Echue info = iter.next();
				b.append(info.officeImpotID).append(COMMA);
				b.append(info.ctbId).append(COMMA);
				b.append(info.diId).append(COMMA);
				b.append(info.dateDebut.index()).append(COMMA);
				b.append(info.dateFin.index()).append(COMMA);
				b.append('\n');
			}
			contenu = b.toString();
		}
		return contenu;
	}

}