package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.droits.ListeDroitsAccesResults;
import ch.vd.unireg.interfaces.service.host.Operateur;

/**
 * Rapport PDF contenant les résultats de l'exécution du job de listing des droits d'accès
 */
public class PdfListeDroitsAccesRapport extends PdfRapport {

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    public void write(final ListeDroitsAccesResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

	    if (status == null) {
		    throw new IllegalArgumentException();
	    }

	    // Création du document PDF
	    PdfWriter writer = PdfWriter.getInstance(this, os);
	    open();
	    addMetaInfo(nom, description);
	    addEnteteUnireg();

	    // Titre
	    addTitrePrincipal("Rapport d'exécution du job de listing des droits d'accès sur les dossiers protégés");

	    // Paramètres
	    addEntete1("Paramètres");
	    {
		    addTableSimple(2, table -> {
			    table.addLigne("Date valeur :", RegDateHelper.dateToDisplayString(results.getDateValeur()));
		    });
	    }

	    // Résultats
	    addEntete1("Résultats");
	    {
		    if (results.interrompu) {
			    addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
					               + "les valeurs ci-dessous sont donc incomplètes.");
		    }

		    addTableSimple(2, table -> {
			    table.addLigne("Nombre total de droits d'accès:", String.valueOf(results.droitsAcces.size()));
			    table.addLigne("Nombre d'erreurs:", String.valueOf(results.erreurs.size()));
			    table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
			    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
		    });
	    }

	    // CTBs traités
	    {
		    String filename = "droits_acces.csv";
		    String titre = "Liste des droits d'accès";
		    String listVide = "(aucun droit d'accès)";
		    try (TemporaryFile contenu = droitsAccesAsCsvFile(results.droitsAcces, filename, status)) {
			    addListeDetaillee(writer, titre, listVide, filename, contenu);
		    }
	    }

	    // CTBs en erreurs
	    {
		    String filename = "erreurs.csv";
		    String titre = "Liste des erreurs";
		    String listVide = "(aucune erreur)";
		    try (TemporaryFile contenu = asCsvFile(results.erreurs, filename, status)) {
			    addListeDetaillee(writer, titre, listVide, filename, contenu);
		    }
	    }

	    close();

	    status.setMessage("Génération du rapport terminée.");
    }

	private TemporaryFile droitsAccesAsCsvFile(List<ListeDroitsAccesResults.InfoDroitAcces> list, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<ListeDroitsAccesResults.InfoDroitAcces>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("OID").append(COMMA);
				b.append("NOM").append(COMMA);
				b.append("ADRESSE_DOMICILE").append(COMMA);
				b.append("TYPE_DROIT").append(COMMA);
				b.append("NIVEAU_DROIT").append(COMMA);
				b.append("CODE_OPERATEUR").append(COMMA);
				b.append("NOM_OPERATEUR").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ListeDroitsAccesResults.InfoDroitAcces elt) {
				b.append(elt.getNoCtb()).append(COMMA);
				b.append(elt.getOidGestion()).append(COMMA);
				b.append(asCsvField(elt.getNomsRaisonsSociales())).append(COMMA);
				b.append(asCsvField(elt.getAdresseEnvoi())).append(COMMA);
				b.append(elt.getType()).append(COMMA);
				b.append(elt.getNiveau()).append(COMMA);

				final Operateur operateur = elt.getOperateur();
				if (operateur == null) {
					b.append(COMMA).append(COMMA);
				}
				else {
					b.append(operateur.getCode()).append(COMMA);
					b.append(operateur.getPrenom()).append(' ').append(operateur.getNom()).append(COMMA);
				}
				return true;
			}
		});
	}
}