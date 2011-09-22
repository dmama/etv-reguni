package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ListeNoteResults;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Rapport PDF d'exécution du batch de production de la liste des contribuables avec note
 */
public class PdfListeNoteRapport extends PdfRapport {

	public void write(final ListeNoteResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

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
			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
					table.addLigne("Période fiscale :", String.valueOf(results.getPeriode()));
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
					table.addLigne("Nombre de contribuables analysés :", String.valueOf(results.nbContribuable));
					table.addLigne("Nombre de fors succeptibles de déclencher une note  :", String.valueOf(results.listeContribuableAvecNote.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}


		// adresses resolues
		{
			final String filename = "contribuables_note.csv";
			final String contenu = getCsvContribuablesNotes(results.listeContribuableAvecNote, filename, status, results.periode);
			final String titre = "Liste des contribuables avec note pour la période fiscale " + results.getPeriode();
			final String listVide = "(aucune)";
			addListeDetaillee(writer, results.listeContribuableAvecNote.size(), titre, listVide, filename, contenu);
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


	private <T extends ListeNoteResults.InfoContribuableAvecNote> String getCsvContribuablesNotes(List<T> liste, String filename, StatusManager status, int periode) {
		String contenu = null;
		if (liste != null && liste.size() > 0) {

			final StringBuilder b = new StringBuilder(liste.size() * 100);
			b.append("NUMERO CTB").append(COMMA).append("NOM INDIVIDU 1").append(COMMA).
					append("PRENOM INDIVIDU 1").append(COMMA).append("NOM INDIVIDU 2").append(COMMA).
					append("PRENOM INDIVIDU 2").append(COMMA).append("NAVS 13 INDIVIDU 1").append(COMMA).
					append("NAVS 13 INDIVIDU 2").append(COMMA).append("ADRESSE").append(COMMA).
					append("OID DE GESTION EN FIN D'ACTIVITE").append(COMMA).append("COMMUNE HC EN FIN D'ACTIVITE").append(COMMA).
					append("CANTON COMMUNE HC").append(COMMA).append("COMMUNE FOR SECONDAIRE").append(COMMA).
					append("DATE FERMETURE FOR SECONDAIRE").append(COMMA).append("MOTIF FIN FOR SECONDAIRE").append(COMMA).
					append("COMMUNE VD OU HC AU 31.12.").append(periode).append(COMMA).append("PAYS AU 31.12.").append(periode).append(COMMA).
					append("CANTON").append(COMMA).append("MOTIF OUVERTURE\n");

			final GentilIterator<T> iter = new GentilIterator<T>(liste);
			while (iter.hasNext()) {

				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				final T info = iter.next();

				final long noCtb = info.noCtb;
				final List<NomPrenom> noms = info.getNomsPrenoms();
				final List<String> nosAvs = info.getNosAvs();
				final String[] adresse = info.getAdresseEnvoi();

				final int sizeNoms = noms.size();
				Assert.isEqual(sizeNoms, nosAvs.size());

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
					communeHC = info.getCommuneHC().getNomMinuscule();

				}
				final String nomCommuneHC = emptyInsteadNull(communeHC);
				final String nomCantonHC = emptyInsteadNull(info.getNomCantonHC());

				final String nomCommuneVaudoise = info.getCommuneVaudoise().getNomMinuscule();
				final String dateFermetureSecondaire = RegDateHelper.dateToDisplayString(info.getDateFermetureSecondaire());
				final String motifFin = info.getMotifFinSecondaire().name();

				String nomCommFin = null;

				String nomPaysFin = null;

				if (info.getCommuneFinPeriode() != null) {
					nomCommFin = info.getCommuneFinPeriode().getNomMinuscule();

				}

				if (info.getPaysFinPeriode() != null) {
					nomPaysFin = info.getPaysFinPeriode().getNomMinuscule();
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
				b.append(motifOuvertureFinPeriode).append("\n");


			}

			contenu = b.toString();
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends ListeNoteResults.Erreur> String asCsvFile(List<T> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {

			StringBuilder b = new StringBuilder(AVG_LINE_LEN * list.size());
			b.append("id du contribuable").append(COMMA).append("Message d'erreur\n");

			final GentilIterator<T> iter = new GentilIterator<T>(list);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}

				T info = iter.next();
				StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
				bb.append(info.noCtb).append(COMMA);
				bb.append(info.message);
				bb.append('\n');

				b.append(bb);
			}
			contenu = b.toString();
		}
		return contenu;
	}

	private static String emptyInsteadNull(String str) {
		return StringUtils.isBlank(str) ? "" : str;
	}

}