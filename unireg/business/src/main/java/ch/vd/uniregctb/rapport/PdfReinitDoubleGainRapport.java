package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * Rapport PDF contenant les résultats de la réinitialisation des barèmes double-gain.
 */
public class PdfReinitDoubleGainRapport extends PdfRapport {

	public void write(final ReinitialiserBaremeDoubleGainResults results, String nom, String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de réinitialisation des barèmes double-gain");

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
					table.addLigne("Nombre total de situations de familles inspectées:", String.valueOf(results.nbSituationsTotal));
					table.addLigne("Nombre de situations réinitialisées:", String.valueOf(results.situationsTraitees.size()));
					table.addLigne("Nombre de situations en erreur:", String.valueOf(results.situationsEnErrors.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// Situations réinitialisées
		{
			String filename = "situations_reinitialisees.csv";
			String contenu = situationsTraiteesAsCsvFile(results.situationsTraitees, filename, status);
			String titre = "Liste des situations réinitialisées";
			String listVide = "(aucun situation réinitialisée)";
			addListeDetaillee(writer, results.situationsTraitees.size(), titre, listVide, filename, contenu);
		}

		// Situations en erreur
		{
			String filename = "situations_en_erreur.csv";
			String contenu = asCsvFile(results.situationsEnErrors, filename, status);
			String titre = "Liste des situations en erreur";
			String listVide = "(aucun situation en erreur)";
			addListeDetaillee(writer, results.situationsEnErrors.size(), titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String situationsTraiteesAsCsvFile(List<ReinitialiserBaremeDoubleGainResults.Situation> situations, String filename, StatusManager status) {
		String contenu = null;
		int size = situations.size();
		if (size > 0) {

			StringBuilder b = new StringBuilder(AVG_LINE_LEN * situations.size());
			b.append("Numéro de l'office d'impôt").append(COMMA).append("Numéro de contribuable").append(COMMA).append(
					"Numéro de l'ancienne situation de famille").append(COMMA).append("Numéro de la nouvelle situation de famille\n");

			final GentilIterator<ReinitialiserBaremeDoubleGainResults.Situation> iter = new GentilIterator<ReinitialiserBaremeDoubleGainResults.Situation>(situations);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				ReinitialiserBaremeDoubleGainResults.Situation situation = iter.next();
				StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
				bb.append(situation.officeImpotID).append(COMMA);
				bb.append(situation.ctbId).append(COMMA);
				bb.append(situation.ancienneId).append(COMMA);
				bb.append(situation.nouvelleId);
				bb.append('\n');

				b.append(bb);
			}
			contenu = b.toString();
		}
		return contenu;
	}

}
