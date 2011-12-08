package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantAbstractResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurMenagesResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurPersonnesPhysiquesResults;

/**
 * Rapport PDF d'exécution du batch de correction des flags "habitant"
 */
public class PdfCorrectionFlagHabitantRapport extends PdfRapport {           

	public void write(final CorrectionFlagHabitantSurPersonnesPhysiquesResults pp, final CorrectionFlagHabitantSurMenagesResults mc,
	                  String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal(String.format("Rapport de correction des flags 'habitant' sur les personnes physiques\n%s", formatTimestamp(dateGeneration)));

		// Résultats
		addEntete1("Résultats pour les personnes physiques seules");
		{
			if (pp.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de contribuables inspectés :", String.valueOf(pp.getNombreElementsInspectes()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(pp.getErreurs().size()));
					table.addLigne("Nombre de nouveaux habitants :", String.valueOf(pp.getNouveauxHabitants().size()));
					table.addLigne("Nombre de nouveaux non-habitants :", String.valueOf(pp.getNouveauxNonHabitants().size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(pp));
				}
			});
		}

		// Résultats
		addEntete1("Résultats pour les ménages communs");
		{
			if (mc.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de contribuables inspectés :", String.valueOf(mc.getNombreElementsInspectes()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(mc.getErreurs().size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(mc));
				}
			});
		}

		// Nouveaux habitants
		{
			final String filename = "nouveaux_habitants.csv";
			final String contenu = genererListeModifications(pp.getNouveauxHabitants(), filename, status);
			final String titre = "Liste des nouveaux habitants";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, pp.getNouveauxHabitants().size(), titre, listVide, filename, contenu);
		}

		// Nouveaux non-habitants
		{
			final String filename = "nouveaux_non_habitants.csv";
			final String contenu = genererListeModifications(pp.getNouveauxNonHabitants(), filename, status);
			final String titre = "Liste des nouveaux non-habitants";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, pp.getNouveauxNonHabitants().size(), titre, listVide, filename, contenu);
		}

		// Erreurs sur les personnes physiques
		{
			final String filename = "erreurs_pp.csv";
			final String contenu = genererListeErreurs(pp.getErreurs(), filename, status);
			final String titre = "Liste des personnes physiques en erreur";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, pp.getErreurs().size(), titre, listVide, filename, contenu);
		}

		// Erreurs sur les ménages communs
		{
			final String filename = "erreurs_mc.csv";
			final String contenu = genererListeErreurs(mc.getErreurs(), filename, status);
			final String titre = "Liste des ménages communs en erreur";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, mc.getErreurs().size(), titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String genererListeErreurs(List<CorrectionFlagHabitantAbstractResults.ContribuableErreur> erreurs, String filename, StatusManager status) {

		String contenu = null;
		if (erreurs != null && !erreurs.isEmpty()) {
			contenu = CsvHelper.asCsvFile(erreurs, filename, status, new CsvHelper.FileFiller<CorrectionFlagHabitantAbstractResults.ContribuableErreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("DESCRIPTION").append(COMMA);
					b.append("COMPLEMENT");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, CorrectionFlagHabitantAbstractResults.ContribuableErreur erreur) {
					b.append(erreur.getNoCtb()).append(COMMA);
					b.append(escapeChars(erreur.getMessage().getLibelle())).append(COMMA);
					b.append(CsvHelper.asCsvField(escapeChars(erreur.getComplementInfo())));
					return true;
				}
			});
		}
		return contenu;
	}

	private String genererListeModifications(List<CorrectionFlagHabitantAbstractResults.ContribuableInfo> modifications, String filename, StatusManager status) {

		String contenu = null;
		if (modifications != null && !modifications.isEmpty()) {
			contenu = CsvHelper.asCsvFile(modifications, filename, status, new CsvHelper.FileFiller<CorrectionFlagHabitantAbstractResults.ContribuableInfo>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, CorrectionFlagHabitantAbstractResults.ContribuableInfo elt) {
					b.append(elt.getNoCtb());
					return true;
				}
			});
		}
		return contenu;
	}
}
