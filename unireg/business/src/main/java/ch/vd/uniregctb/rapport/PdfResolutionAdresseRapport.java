package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.ResolutionAdresseResults;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableResults;

/**
 * Rapport PDF d'exécution du batch d'échéance des LRs
 */
public class PdfResolutionAdresseRapport extends PdfRapport {

	public void write(final ResolutionAdresseResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la résolution des adresses ");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new TableSimpleCallback() {
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
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre d'adresses traitées :", String.valueOf(results.nbAdresseTotal));
					table.addLigne("Nombre d'adresses résolues :", String.valueOf(results.listeAdresseResolues.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}



		// adresses resolues
			{
				final String filename = "adresses_resolues.csv";
				final String contenu = getCsvAdresseResolue(results.listeAdresseResolues, filename, status);
				final String titre = "Liste des adresses résolues";
				final String listVide = "(aucune)";
				addListeDetaillee(writer, results.listeAdresseResolues.size(), titre, listVide, filename, contenu);
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

	

	private <T extends ResolutionAdresseResults.InfoAdresse> String getCsvAdresseResolue(List<T> liste, String filename, StatusManager status) {
		String contenu = null;
		if (liste != null && liste.size() > 0) {

			final StringBuilder b = new StringBuilder(liste.size() * 100);
			b.append("NUMERO TIERS ").append(COMMA).append("NUMERO ADRESSE").append(COMMA).append("NUMERO RUE").append(COMMA).append("NOM RUE").append(COMMA).append("NOM LOCALITE").append(COMMA)
					.append("NUMERO ORDRE POSTAL\n");

			final GentilIterator<T> iter = new GentilIterator<T>(liste);
			while (iter.hasNext()) {

				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				final T info = iter.next();
				b.append(info.tiersId).append(COMMA);
				b.append(info.adresseId).append(COMMA);
				b.append(info.rueId).append(COMMA);
				b.append(info.nomRue).append(COMMA);
				b.append(info.localite).append(COMMA);
				b.append(info.numeroOrdrePostal).append(COMMA);
				b.append("\n");


			}

			contenu = b.toString();
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends ResolutionAdresseResults.Erreur> String asCsvFile(List<T> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {

			StringBuilder b = new StringBuilder(AVG_LINE_LEN * list.size());
			b.append("id de l'adresse").append(COMMA).append("Message d'erreur\n");

			final GentilIterator<T> iter = new GentilIterator<T>(list);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				T info = iter.next();
				StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
				bb.append(info.id).append(COMMA);
				bb.append(info.raison);

				if (!iter.isLast()) {
					bb.append("\n");
				}

				b.append(bb);
			}
			contenu = b.toString();
		}
		return contenu;
	}


}