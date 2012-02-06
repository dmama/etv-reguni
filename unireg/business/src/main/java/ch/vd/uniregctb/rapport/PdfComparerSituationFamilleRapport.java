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
import ch.vd.uniregctb.situationfamille.ComparerSituationFamilleResults;

/**
 * Rapport PDF d'exécution du batch de comparaison des situations de famille
 */
public class PdfComparerSituationFamilleRapport extends PdfRapport {

	public void write(final ComparerSituationFamilleResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la comparaison des situations des familles enregistrées dans UNIREG et celles du civil ");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
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

			addTableSimple(new float[]{70f, 30f}, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de situations analysées :", String.valueOf(results.nbSituationTotal));
					table.addLigne("Nombre de situations différentes  :", String.valueOf(results.listeSituationsDifferentes.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}



		// adresses resolues
			{
				final String filename = "situations_differentes.csv";
				final String contenu = getCsvSituationsDifferentes(results.listeSituationsDifferentes, filename, status);
				final String titre = "Liste des situations différentes";
				final String listVide = "(aucune)";
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}



		// erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = asCsvFile(results.erreurs, filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	

	private <T extends ComparerSituationFamilleResults.SituationsDifferentes> String getCsvSituationsDifferentes(List<T> liste, String filename, StatusManager status) {
		String contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("SITUATION ID ").append(COMMA);
					b.append("NUMERO CTB").append(COMMA);
					b.append("ETAT UNIREG").append(COMMA);
					b.append("DATE DEBUT UNIREG").append(COMMA);
					b.append("ETAT HOST").append(COMMA);
					b.append("DATE DEBUT HOST");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.id).append(COMMA);
					b.append(info.numeroContribuable).append(COMMA);
					b.append(info.etatCivil).append(COMMA);
					b.append(info.dateDebutEtatCivil).append(COMMA);
					b.append(info.etatCivilHost).append(COMMA);
					b.append(info.dateDebutEtatCivilHost);
					return true;
				}
			});
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends ComparerSituationFamilleResults.Erreur> String asCsvFile(List<T> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("id de la situation").append(COMMA).append("Message d'erreur");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.id).append(COMMA);
					b.append(escapeChars(info.message));
					return true;
				}
			});
		}
		return contenu;
	}


}