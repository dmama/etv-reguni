package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.ordinaire.pp.ListeNoteResults;
import ch.vd.unireg.type.MotifFor;

/**
 * Rapport PDF d'exécution du batch de production de la liste des contribuables avec note
 */
public class PdfListeNoteRapport extends PdfRapport {

	public void write(final ListeNoteResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la production de la liste des contribuables dont la DI est transformée en note ");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
				table.addLigne("Période fiscale :", String.valueOf(results.getPeriode()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{70f, 30f}, table -> {
				table.addLigne("Nombre de contribuables analysés :", String.valueOf(results.nbContribuable));
				table.addLigne("Nombre de fors succeptibles de déclencher une note  :", String.valueOf(results.listeContribuableAvecNote.size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}


		// adresses resolues
		{
			final String filename = "contribuables_note.csv";
			final String titre = "Liste des contribuables avec note pour la période fiscale " + results.getPeriode();
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = getCsvContribuablesNotes(results.listeContribuableAvecNote, filename, status, results.periode)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}


		// erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = asCsvErrorFile(results.erreurs, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}


	private <T extends ListeNoteResults.InfoContribuableAvecNote> TemporaryFile getCsvContribuablesNotes(List<T> liste, String filename, StatusManager status, final int periode) {
		TemporaryFile contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NUMERO CTB").append(COMMA);
					b.append("NOM INDIVIDU 1").append(COMMA);
					b.append("PRENOM INDIVIDU 1").append(COMMA);
					b.append("NOM INDIVIDU 2").append(COMMA);
					b.append("PRENOM INDIVIDU 2").append(COMMA);
					b.append("NAVS 13 INDIVIDU 1").append(COMMA);
					b.append("NAVS 13 INDIVIDU 2").append(COMMA);
					b.append("ADRESSE").append(COMMA);
					b.append("OID DE GESTION EN FIN D'ACTIVITE").append(COMMA);
					b.append("COMMUNE HC EN FIN D'ACTIVITE").append(COMMA);
					b.append("CANTON COMMUNE HC").append(COMMA);
					b.append("COMMUNE FOR SECONDAIRE").append(COMMA);
					b.append("DATE FERMETURE FOR SECONDAIRE").append(COMMA);
					b.append("MOTIF FIN FOR SECONDAIRE").append(COMMA);
					b.append("COMMUNE VD OU HC AU 31.12.").append(periode).append(COMMA);
					b.append("PAYS AU 31.12.").append(periode).append(COMMA);
					b.append("CANTON").append(COMMA);
					b.append("MOTIF OUVERTURE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					final long noCtb = info.noCtb;
					final List<NomPrenom> noms = info.getNomsPrenoms();
					final List<String> nosAvs = info.getNosAvs();
					final String[] adresse = info.getAdresseEnvoi();

					final int sizeNoms = noms.size();
					if (nosAvs.size() != sizeNoms) {
						throw new IllegalArgumentException();
					}

					// ajout des infos au fichier
					final String nom1 = emptyInsteadNull(sizeNoms > 0 ? noms.get(0).getNom() : null);
					final String prenom1 = emptyInsteadNull(sizeNoms > 0 ? noms.get(0).getPrenom() : null);
					final String nom2 = emptyInsteadNull(sizeNoms > 1 ? noms.get(1).getNom() : null);
					final String prenom2 = emptyInsteadNull(sizeNoms > 1 ? noms.get(1).getPrenom() : null);
					final String numeroAvs1 = emptyInsteadNull(sizeNoms > 0 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(0)) : null);
					final String numeroAvs2 = emptyInsteadNull(sizeNoms > 1 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(1)) : null);
					final String adresseCourrier = asCsvField(adresse);
					final int oidGestion = info.getOidGestion();
					String communeHC = null;

					if (info.getCommuneHC() != null) {
						communeHC = info.getCommuneHC().getNomOfficiel();

					}
					final String nomCommuneHC = emptyInsteadNull(communeHC);
					final String nomCantonHC = emptyInsteadNull(info.getNomCantonHC());

					final String nomCommuneVaudoise = info.getCommuneVaudoise() != null ? info.getCommuneVaudoise().getNomOfficiel() : StringUtils.EMPTY;
					final String dateFermetureSecondaire = RegDateHelper.dateToDisplayString(info.getDateFermetureSecondaire());
					final String motifFin = info.getMotifFinSecondaire() != null ? info.getMotifFinSecondaire().name() : StringUtils.EMPTY;

					String nomCommFin = null;

					String nomPaysFin = null;

					if (info.getCommuneFinPeriode() != null) {
						nomCommFin = info.getCommuneFinPeriode().getNomOfficiel();

					}

					if (info.getPaysFinPeriode() != null) {
						nomPaysFin = info.getPaysFinPeriode().getNomCourt();
					}
					final String nomCommuneFinPeriode = emptyInsteadNull(nomCommFin);
					final String nomPaysFinPeriode = emptyInsteadNull(nomPaysFin);
					final String nomCantonFinPeriode = emptyInsteadNull(info.getNomCantonFinPeriode());
					String motifOuverture = null;
					final MotifFor motifFor = info.getMotifOuvertureForPrincipal();
					if (motifFor != null) {
						motifOuverture = motifFor.name();
					}
					final String motifOuvertureFinPeriode = emptyInsteadNull(motifOuverture);

					b.append(noCtb).append(COMMA);
					b.append(nom1).append(COMMA);
					b.append(prenom1).append(COMMA);
					b.append(nom2).append(COMMA);
					b.append(prenom2).append(COMMA);
					b.append(numeroAvs1).append(COMMA);
					b.append(numeroAvs2).append(COMMA);
					b.append(adresseCourrier).append(COMMA);
					b.append(oidGestion).append(COMMA);
					b.append(nomCommuneHC).append(COMMA);
					b.append(nomCantonHC).append(COMMA);
					b.append(nomCommuneVaudoise).append(COMMA);
					b.append(dateFermetureSecondaire).append(COMMA);
					b.append(motifFin).append(COMMA);
					b.append(nomCommuneFinPeriode).append(COMMA);
					b.append(nomPaysFinPeriode).append(COMMA);
					b.append(nomCantonFinPeriode).append(COMMA);
					b.append(motifOuvertureFinPeriode);
					return true;
				}
			});
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends ListeNoteResults.Erreur> TemporaryFile asCsvErrorFile(List<T> list, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("id du contribuable").append(COMMA).append("Message d'erreur");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.noCtb).append(COMMA);
					b.append(escapeChars(info.message));
					return true;
				}
			});
		}
		return contenu;
	}

	private static String emptyInsteadNull(String str) {
		return StringUtils.trimToEmpty(str);
	}

}