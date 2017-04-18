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
import ch.vd.uniregctb.declaration.ordinaire.pp.EchoirDIsPPResults;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des DIs PP.
 */
public class PdfEchoirDIsPPRapport extends PdfRapport {

	public void write(final EchoirDIsPPResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de passage des DIs PP sommées à l'état échu");

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
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// DIs échues
		{
			String filename = "dis_echues.csv";
			String titre = "Liste des déclarations nouvellement échues";
			String listVide = "(aucun déclaration échue)";
			try (TemporaryFile contenu = disEchuesAsCsvFile(results.disEchues, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// DIs en erreur
		{
			String filename = "dis_en_erreur.csv";
			String titre = "Liste des déclarations en erreur";
			String listVide = "(aucun déclaration en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.disEnErrors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}


	private TemporaryFile disEchuesAsCsvFile(List<EchoirDIsPPResults.Echue> disEchues, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		final int size = disEchues.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(disEchues, filename, status, new CsvHelper.FileFiller<EchoirDIsPPResults.Echue>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("OID").append(COMMA).append("CTB_ID").append(COMMA).append("DI_ID").append(COMMA).append("DEBUT_PERIODE").append(COMMA).append("FIN_PERIODE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EchoirDIsPPResults.Echue info) {
					b.append(info.officeImpotID).append(COMMA);
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

}