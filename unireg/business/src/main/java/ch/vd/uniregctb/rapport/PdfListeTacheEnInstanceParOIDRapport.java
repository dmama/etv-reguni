package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tache.ListeTachesEnInstanceParOID;


/**
 * Rapport PDF contenant la liste des taches en instance par OID.
 */
public class PdfListeTacheEnInstanceParOIDRapport extends PdfRapport {

	public void write(final ListeTachesEnInstanceParOID results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfListeTacheEnInstanceParOIDRapport document = new PdfListeTacheEnInstanceParOIDRapport();
		PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal("Liste des tâches en instances par OID ");

		// Paramètres
		document.addEntete1("Paramètres");
		document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
			@Override
			public void fillTable(PdfTableSimple table) throws DocumentException {
				table.addLigne("Date de traitement: ", RegDateHelper.dateToDisplayString(results.dateTraitement));
			}
		});
		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.interrompu) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					DecimalFormat df = new DecimalFormat("###,###,###.#");
					table.addLigne("Nombre moyen de tâche par contribuable: ", df.format(results.getNombreTacheMoyen()) + " tâche(s)");
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport: ", formatTimestamp(dateGeneration));
				}
			});
		}
		{
			String filename = "tachesenInstance_par_OID.csv";
			String contenu = asCsvFile(results, filename, status);
			String titre = "Liste des tâches en instance par OID";
			String listVide = "(aucune tâche traitée)";
			document.addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String asCsvFile(ListeTachesEnInstanceParOID results, String filename, StatusManager status) {
		String contenu = null;
		List<ListeTachesEnInstanceParOID.LigneTacheInstance> list = results.getLignes();
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<ListeTachesEnInstanceParOID.LigneTacheInstance>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de l'OID").append(COMMA);
					b.append("Type de tâche").append(COMMA);
					b.append("Nombre de tâches");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ListeTachesEnInstanceParOID.LigneTacheInstance ligne) {
					b.append(ligne.getNumeroOID()).append(COMMA);
					b.append(ligne.getTypeTache()).append(COMMA);
					b.append(ligne.getNombreTache());
					return true;
				}
			});
		}
		return contenu;

	}
}
