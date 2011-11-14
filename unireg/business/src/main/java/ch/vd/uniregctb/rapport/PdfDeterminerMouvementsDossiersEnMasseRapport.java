package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.mouvement.DeterminerMouvementsDossiersEnMasseResults;

/**
 * Rapport PDF du job de détermination des mouvements de dossiers en masse
 */
public class PdfDeterminerMouvementsDossiersEnMasseRapport extends PdfRapport {

	public void write(final DeterminerMouvementsDossiersEnMasseResults results, String nom, String description, Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal(String.format("Rapport de détermination des mouvements de dossiers en masse\n%s", formatTimestamp(dateGeneration)));

		// Paramètres
		addEntete1("Paramètres");
		{
		    addTableSimple(2, new PdfRapport.TableSimpleCallback() {
		        @Override
		        public void fillTable(PdfTableSimple table) throws DocumentException {
		            table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
			        table.addLigne("Archives seulements :", String.valueOf(results.archivesSeulement));
		        }
		    });
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de contribuables inspectés :", String.valueOf(results.getNbContribuablesInspectes()));
					table.addLigne("Nombre de contribuables ignorés :", String.valueOf(results.ignores.size()));
					table.addLigne("Nombre de mouvements créés :", String.valueOf(results.mouvements.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				}
			});
		}

		// Mouvements
		{
			final String filename = "mouvements.csv";
			final String contenu = genererListeMouvements(results.mouvements, filename, status);
			final String titre = "Liste des mouvements générés";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.mouvements.size(), titre, listVide, filename, contenu);
		}

		// Contribuables ignorés
		{
			final String filename = "ignores.csv";
			final String contenu = genererListeDossiersNonTraites(results.ignores, filename, status);
			final String titre = "Liste des dossiers ignorés";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.ignores.size(), titre, listVide, filename, contenu);
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = genererListeDossiersNonTraites(results.erreurs, filename, status);
			final String titre = "Liste des erreurs rencontrées";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, results.erreurs.size(), titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String genererListeDossiersNonTraites(List<DeterminerMouvementsDossiersEnMasseResults.NonTraite> nonTraites, String filename, StatusManager status) {

		String contenu = null;
		if (nonTraites != null && nonTraites.size() > 0) {
			contenu = CsvHelper.asCsvFile(nonTraites, filename, status, new CsvHelper.FileFiller<DeterminerMouvementsDossiersEnMasseResults.NonTraite>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("RAISON").append(COMMA);
					b.append("COMPLEMENT");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DeterminerMouvementsDossiersEnMasseResults.NonTraite nonTraite) {
					b.append(nonTraite.noCtb).append(COMMA);
					b.append(escapeChars(nonTraite.getTypeInformation())).append(COMMA);
					b.append(asCsvField(nonTraite.complement.split("\n")));
					return true;
				}
			});
		}
		return contenu;
	}

	private String genererListeMouvements(List<DeterminerMouvementsDossiersEnMasseResults.Mouvement> mouvements, String filename, StatusManager status) {

		String contenu = null;
		if (mouvements != null && mouvements.size() > 0) {
			contenu = CsvHelper.asCsvFile(mouvements, filename, status, new CsvHelper.FileFiller<DeterminerMouvementsDossiersEnMasseResults.Mouvement>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_CTB").append(COMMA);
					b.append("TYPE_MVT").append(COMMA);
					b.append("OID").append(COMMA);
					b.append("OID_DEST");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DeterminerMouvementsDossiersEnMasseResults.Mouvement mvt) {
					b.append(mvt.noCtb).append(COMMA);
					b.append(escapeChars(mvt.getTypeInformation())).append(COMMA);
					b.append(mvt.oidActuel).append(COMMA);
					if (mvt instanceof DeterminerMouvementsDossiersEnMasseResults.MouvementOid) {
						b.append(((DeterminerMouvementsDossiersEnMasseResults.MouvementOid) mvt).oidDestination);
					}
					return true;
				}
			});
		}
		return contenu;
	}
}
