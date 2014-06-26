package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.securite.model.Operateur;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.droits.ListeDroitsAccesResults;

/**
 * Rapport PDF contenant les résultats de l'exécution du job de listing des droits d'accès
 */
public class PdfListeDroitsAccesRapport extends PdfRapport {

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    public void write(final ListeDroitsAccesResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

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
		    addTableSimple(2, new PdfRapport.TableSimpleCallback() {
			    @Override
			    public void fillTable(PdfTableSimple table) throws DocumentException {
				    table.addLigne("Date valeur :", RegDateHelper.dateToDisplayString(results.getDateValeur()));
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

            addTableSimple(2, new TableSimpleCallback() {
                @Override
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de droits d'accès:", String.valueOf(results.droitsAcces.size()));
                    table.addLigne("Nombre d'erreurs:", String.valueOf(results.erreurs.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // CTBs traités
        {
            String filename = "droits_acces.csv";
            String contenu = droitsAccesAsCsvFile(results.droitsAcces, filename, status);
            String titre = "Liste des droits d'accès";
            String listVide = "(aucun droit d'accès)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        // CTBs en erreurs
        {
            String filename = "erreurs.csv";
            String contenu = asCsvFile(results.erreurs, filename, status);
            String titre = "Liste des erreurs";
            String listVide = "(aucune erreur)";
            addListeDetaillee(writer, titre, listVide, filename, contenu);
        }

        close();

        status.setMessage("Génération du rapport terminée.");
    }

	private String droitsAccesAsCsvFile(List<ListeDroitsAccesResults.InfoDroitAcces> list, String filename, StatusManager status) {
		return CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<ListeDroitsAccesResults.InfoDroitAcces>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA);
				b.append("OID").append(COMMA);
				b.append("NOMS_PRENOMS").append(COMMA);
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
				b.append(asCsvField(elt.getNomsPrenoms())).append(COMMA);
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