package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantAbstractResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurMenagesResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurPersonnesPhysiquesResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.lang.StringUtils;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

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
		if (erreurs != null && erreurs.size() > 0) {

			final StringBuilder builder = new StringBuilder((erreurs.size() + 1) * 100);
			builder.append("NO_CTB" + COMMA + "DESCRIPTION" + COMMA + "COMPLEMENT" + "\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final GentilIterator<CorrectionFlagHabitantAbstractResults.ContribuableErreur> iterator = new GentilIterator<CorrectionFlagHabitantAbstractResults.ContribuableErreur>(erreurs);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final CorrectionFlagHabitantAbstractResults.ContribuableErreur erreur = iterator.next();
				builder.append(erreur.getNoCtb()).append(COMMA);
				builder.append(erreur.getMessage().getLibelle()).append(COMMA);
				if (!StringUtils.isEmpty(erreur.getComplementInfo())) {
					builder.append("\"");
					builder.append(escapeChars(erreur.getComplementInfo()));
					builder.append("\"");
				}
				builder.append('\n');
			}
			contenu = builder.toString();
		}
		return contenu;
	}

	private String genererListeModifications(List<CorrectionFlagHabitantAbstractResults.ContribuableInfo> modifications, String filename, StatusManager status) {

		String contenu = null;
		if (modifications != null && modifications.size() > 0) {

			final StringBuilder builder = new StringBuilder((modifications.size() + 1) * 10);
			builder.append("NO_CTB\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final GentilIterator<CorrectionFlagHabitantAbstractResults.ContribuableInfo> iterator = new GentilIterator<CorrectionFlagHabitantAbstractResults.ContribuableInfo>(modifications);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final CorrectionFlagHabitantAbstractResults.ContribuableInfo modification = iterator.next();
				builder.append(modification.getNoCtb());
				builder.append('\n');
			}

			contenu = builder.toString();
		}
		return contenu;
	}
}
