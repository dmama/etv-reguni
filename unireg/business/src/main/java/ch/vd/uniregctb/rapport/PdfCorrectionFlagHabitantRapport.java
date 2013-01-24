package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantResults;

/**
 * Rapport PDF d'exécution du batch de correction des flags "habitant"
 */
public class PdfCorrectionFlagHabitantRapport extends PdfRapport {

	public void write(final CorrectionFlagHabitantResults res, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal(String.format("Rapport de correction des flags 'habitant' sur les personnes physiques\n%s", formatTimestamp(dateGeneration)));

		// Résultats
		addEntete1("Résultats");
		{
			if (res.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de personnes physiques inspectées :", String.valueOf(res.getNombrePPInspectees()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(res.getErreurs().size()));
					table.addLigne("Nombre de nouveaux habitants :", String.valueOf(res.getNouveauxHabitants().size()));
					table.addLigne("Nombre de nouveaux non-habitants :", String.valueOf(res.getNouveauxNonHabitants().size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(res));
				}
			});
		}

		// Nouveaux habitants
		{
			final String filename = "nouveaux_habitants.csv";
			final String contenu = genererListeModifications(res.getNouveauxHabitants(), filename, status);
			final String titre = "Liste des nouveaux habitants";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// Nouveaux non-habitants
		{
			final String filename = "nouveaux_non_habitants.csv";
			final String contenu = genererListeModifications(res.getNouveauxNonHabitants(), filename, status);
			final String titre = "Liste des nouveaux non-habitants";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = genererListeErreurs(res.getErreurs(), filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}
		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String genererListeErreurs(List<CorrectionFlagHabitantResults.ContribuableErreur> erreurs, String filename, StatusManager status) {

		String contenu = null;
		if (erreurs != null && !erreurs.isEmpty()) {
			contenu = CsvHelper.asCsvFile(erreurs, filename, status, new CsvHelper.FileFiller<CorrectionFlagHabitantResults.ContribuableErreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("DESCRIPTION").append(COMMA);
					b.append("COMPLEMENT");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, CorrectionFlagHabitantResults.ContribuableErreur erreur) {
					b.append(erreur.getNoCtb()).append(COMMA);
					b.append(escapeChars(erreur.getMessage().getLibelle())).append(COMMA);
					b.append(CsvHelper.asCsvField(escapeChars(erreur.getComplementInfo())));
					return true;
				}
			});
		}
		return contenu;
	}

	private String genererListeModifications(List<CorrectionFlagHabitantResults.ContribuableInfo> modifications, String filename, StatusManager status) {

		String contenu = null;
		if (modifications != null && !modifications.isEmpty()) {
			contenu = CsvHelper.asCsvFile(modifications, filename, status, new CsvHelper.FileFiller<CorrectionFlagHabitantResults.ContribuableInfo>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, CorrectionFlagHabitantResults.ContribuableInfo elt) {
					b.append(elt.getNoCtb());
					return true;
				}
			});
		}
		return contenu;
	}
}
