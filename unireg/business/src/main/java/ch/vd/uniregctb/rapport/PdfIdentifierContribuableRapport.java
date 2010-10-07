package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.source.DeterminerLRsEchuesResults;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableResults;

/**
 * Rapport PDF d'exécution du batch d'échéance des LRs
 */
public class PdfIdentifierContribuableRapport extends PdfRapport {

	public void write(final IdentifierContribuableResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la relance de l'identification automatique ");

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

			addTableSimple(new float[] {70f, 30f}, new TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de messages identifiés :", String.valueOf(results.identifies.size()));
					table.addLigne("Nombre de messages non identifiés :", String.valueOf(results.nonIdentifies.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}

		// Messages identifiés
		{
			final String filename = "messages_identifies.csv";
			final String contenu = getCsvMessageIdentifes(results.identifies, filename, status);
			final String titre = "Liste des messages identifiés";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.identifies.size(), titre, listVide, filename, contenu);
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

	private <T extends IdentifierContribuableResults.Identifie> String getCsvMessageIdentifes(List<T> liste, String filename, StatusManager status) {
		String contenu = null;
		if (liste != null && liste.size() > 0) {

			final StringBuilder b = new StringBuilder(liste.size() * 100);
			b.append("Business-ID Message").append(COMMA).append("PRENOMS").append(COMMA).append("NOM").append(COMMA).append("NUMERO CTB").append(COMMA).append("NUMERO MENAGE\n");

			final GentilIterator<T> iter = new GentilIterator<T>(liste);
			while (iter.hasNext()) {

				if (iter.isAtNewPercent()) {
				    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				final T info = iter.next();
				b.append(info.businessID).append(COMMA);
				b.append(info.prenom).append(COMMA);
				b.append(info.nom).append(COMMA);
				b.append(info.noCtb).append(COMMA);
				if (info.noCtbMenage!= null) {
					b.append(info.noCtbMenage).append("\n");
				}
				else{
					b.append("\n");
				}
			
			}

			contenu = b.toString();
		}
		return contenu;
	}

		/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends IdentifierContribuableResults.Erreur> String asCsvFile(List<T> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {

			StringBuilder b = new StringBuilder(AVG_LINE_LEN * list.size());
			b.append("id du message").append(COMMA).append("Message d'erreur\n");

			final GentilIterator<T> iter = new GentilIterator<T>(list);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				T info = iter.next();
				StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
				bb.append(info.messageId).append(COMMA);
				bb.append(info.raison);
				bb.append('\n');

				b.append(bb);
			}
			contenu = b.toString();
		}
		return contenu;
	}


}