package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.tache.ListeTachesEnInstanceParOID;


/**
 * Rapport PDF contenant la liste des taches en instance par OID.
 */
public class PdfListeTacheEnInstanceParOIDRapport extends PdfRapport {

	public void write(final ListeTachesEnInstanceParOID results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

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
		document.addTableSimple(2, table -> {
			table.addLigne("Date de traitement: ", RegDateHelper.dateToDisplayString(results.dateTraitement));
		});
		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.interrompu) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						                    + "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(2, table -> {
				DecimalFormat df = new DecimalFormat("###,###,###.#");
				table.addLigne("Nombre moyen de tâche par contribuable: ", df.format(results.getNombreTacheMoyen()) + " tâche(s)");
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport: ", formatTimestamp(dateGeneration));
			});
		}
		{
			String filename = "tachesenInstance_par_OID.csv";
			String titre = "Liste des tâches en instance par OID";
			String listVide = "(aucune tâche traitée)";
			try (TemporaryFile contenu = asCsvFile(results, filename, status)) {
				document.addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile asCsvFile(ListeTachesEnInstanceParOID results, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		List<ListeTachesEnInstanceParOID.LigneTacheInstance> list = results.getLignes();
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<ListeTachesEnInstanceParOID.LigneTacheInstance>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de l'OID").append(COMMA);
					b.append("Nom de l'OID ou de la collectivité").append(COMMA);
					b.append("Type de tâche").append(COMMA);
					b.append("Nombre de tâches");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, ListeTachesEnInstanceParOID.LigneTacheInstance ligne) {
					b.append(ligne.getNumeroOID()).append(COMMA);
					b.append(ligne.getNomCollectivite()).append(COMMA);
					b.append(ligne.getTypeTache()).append(COMMA);
					b.append(ligne.getNombreTache());
					return true;
				}
			});
		}
		return contenu;

	}
}
