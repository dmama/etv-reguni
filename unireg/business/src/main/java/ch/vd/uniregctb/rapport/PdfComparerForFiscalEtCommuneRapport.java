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
import ch.vd.uniregctb.metier.ComparerForFiscalEtCommuneResults;

/**
 * Rapport PDF d'exécution du batch de comparaison du dernier for fiscal et de la commune de résidence
 */
public class PdfComparerForFiscalEtCommuneRapport extends PdfRapport {

	public void write(final ComparerForFiscalEtCommuneResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la comparaison du dernier for fiscal et de la commune de résidence ");

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
					table.addLigne("Nombre de contribuables analysés :", String.valueOf(results.nbCtbTotal));
					table.addLigne("Nombre de communes différentes  :", String.valueOf(results.listeCommunesDifferentes.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}



		// adresses resolues
			{
				final String filename = "for_et_commune_differente.csv";
				final String contenu = getCsvForFiscalEtCommuneDifferente(results.listeCommunesDifferentes, filename, status);
				final String titre = "Liste des communes différentes";
				final String listVide = "(aucune)";
				addListeDetaillee(writer, results.listeCommunesDifferentes.size(), titre, listVide, filename, contenu);
			}



		// erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = asCsvFile(results.erreurs, filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, results.erreurs.size(), titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	

	private <T extends ComparerForFiscalEtCommuneResults.CommunesDifferentes> String getCsvForFiscalEtCommuneDifferente(List<T> liste, String filename, StatusManager status) {
		String contenu = null;
		if (liste != null && liste.size() > 0) {

			final StringBuilder b = new StringBuilder(liste.size() * 100);
			b.append("FOR FISCAL ID ").append(COMMA).append("NUMERO CTB").append(COMMA).append("COMMUNE FOR").
					append(COMMA).append("DATE DEBUT VALIDITE").append(COMMA).append("COMMUNE ADRESSE RESIDENCE").append(COMMA)
					.append("DATE DEBUT ADRESSE RESIDENCE\n");

			final GentilIterator<T> iter = new GentilIterator<T>(liste);
			while (iter.hasNext()) {

				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				final T info = iter.next();
				b.append(info.id).append(COMMA);
				b.append(info.numeroContribuable).append(COMMA);
				b.append(info.nomCommuneFor).append(COMMA);
				b.append(info.dateDebutFor).append(COMMA);
				b.append(info.nomCommuneAdresse).append(COMMA);
				b.append(info.dateDebutAdresse).append(COMMA);
				b.append("\n");


			}

			contenu = b.toString();
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends ComparerForFiscalEtCommuneResults.Erreur> String asCsvFile(List<T> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {

			StringBuilder b = new StringBuilder(AVG_LINE_LEN * list.size());
			b.append("numero Contribuable").append(COMMA).append("Message d'erreur\n");

			final GentilIterator<T> iter = new GentilIterator<T>(list);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				T info = iter.next();
				StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
				bb.append(info.id).append(COMMA);
				bb.append(info.message);
				bb.append('\n');

				b.append(bb);
			}
			contenu = b.toString();
		}
		return contenu;
	}


}