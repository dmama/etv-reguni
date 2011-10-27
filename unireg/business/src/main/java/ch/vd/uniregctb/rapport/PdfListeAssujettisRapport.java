package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.ListesResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.listes.assujettis.ListeAssujettisResults;

/**
 * Rapport PDF d'exécution du batch d'extraction de la liste des assujettis d'une période fiscale
 */
public class PdfListeAssujettisRapport extends PdfRapport {

	public void write(final ListeAssujettisResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'extraction de la liste des assujettis d'une période fiscale");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
			    @Override
			    public void fillTable(PdfTableSimple table) throws DocumentException {
			        table.addLigne("Période fiscale :", String.valueOf(results.getAnneeFiscale()));
				    table.addLigne("Nombre de threads :", String.valueOf(results.getNombreThreads()));
				    table.addLigne("Avec sourciers purs :", String.valueOf(results.isAvecSourciersPurs()));
				    table.addLigne("Seulement assujettis fin année :", String.valueOf(results.isSeulementAssujettisFinAnnee()));
			        table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
			    }
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de contribuables inspectés :", String.valueOf(results.getNbContribuablesInspectes()));
					table.addLigne("Nombre de contribuables assujettis :", String.valueOf(results.getNbCtbAssujettis()));
					table.addLigne("Nombre de contribuables ignorés :", String.valueOf(results.getNbCtbIgnores()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.getListeErreurs().size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(results));
					table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
				}
			});
		}

		// Assujettis trouvés
		{
			final String filename = "assujettis.csv";
			final String contenu = buildContenuAssujettis(results.getAssujettis(), status, filename);
			final String titre = "Liste des contribuables assujettis et de leurs assujettissements";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.getAssujettis().size(), titre, listVide, filename, contenu);
		}

		// Contribuables ignorés
		{
			final String filename = "ignores.csv";
			final String contenu = buildContenuIgnores(results.getIgnores(), filename, status);
			final String titre = "Liste des contribuables ignorés";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.getIgnores().size(), titre, listVide, filename, contenu);
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = buildContenuErreurs(results.getListeErreurs(), filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, results.getListeErreurs().size(), titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private static String buildContenuAssujettis(List<ListeAssujettisResults.InfoCtbAssujetti> liste, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(liste, filename, status, 70, new CsvHelper.Filler<ListeAssujettisResults.InfoCtbAssujetti>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NO_CTB").append(COMMA).append("TYPE_ASSUJETTISSEMENT").append(COMMA)
						.append("DATE_DEBUT").append(COMMA).append("DATE_FIN").append(COMMA)
						.append("MOTIF_FRAC_DEBUT").append(COMMA).append("MOTIF_FRAC_FIN").append(COMMA);
			}

			@Override
			public void fillLine(StringBuilder b, ListeAssujettisResults.InfoCtbAssujetti elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.typeAssujettissement).append(COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.debutAssujettissement)).append(COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.finAssujettissement)).append(COMMA);
				b.append(elt.motifDebut != null ? elt.motifDebut.getDescription(true) : StringUtils.EMPTY).append(COMMA);
				b.append(elt.motifFin != null ? elt.motifFin.getDescription(false) : StringUtils.EMPTY);
			}
		});
	}

	private static String buildContenuIgnores(List<ListeAssujettisResults.InfoCtbIgnore> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvFile(liste, filename, status, 30, new CsvHelper.Filler<ListeAssujettisResults.InfoCtbIgnore>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NO_CTB").append(COMMA).append("RAISON");
			}

			@Override
			public void fillLine(StringBuilder b, ListeAssujettisResults.InfoCtbIgnore elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(escapeChars(elt.cause.description));
			}
		});
	}

	private static String buildContenuErreurs(List<ListeAssujettisResults.Erreur> results, String filename, StatusManager status) {
		return CsvHelper.asCsvFile(results, filename, status, 100, new CsvHelper.Filler<ListeAssujettisResults.Erreur>() {
			@Override
			public void fillHeader(StringBuilder b) {
				b.append("NO_CTB").append(COMMA).append("RAISON").append(COMMA).append("DETAILS");
			}

			@Override
			public void fillLine(StringBuilder b, ListesResults.Erreur elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.getDescriptionRaison()).append(COMMA);
				b.append(asCsvField(elt.details));
			}
		});
	}
}
