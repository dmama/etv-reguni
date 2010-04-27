package ch.vd.uniregctb.rapport;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.externe.EtatEvenementExterne;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.stats.evenements.StatistiqueEvenementInfo;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsExternesResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsIdentificationContribuableResults;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Rapport des statistiques des événements reçus par Unireg
 */
public class PdfStatistiquesEvenementsRapport extends PdfRapport {

	public void write(final StatsEvenementsCivilsResults civils, final StatsEvenementsExternesResults externes,
	                  final StatsEvenementsIdentificationContribuableResults identCtb,
	                  final RegDate dateReference, String nom, String description, final Date dateGeneration, OutputStream os,
	                  StatusManager status) throws Exception{

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Statistiques des événements");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Evénements civils:", String.valueOf(civils != null));
					table.addLigne("Evénements externes:", String.valueOf(externes != null));
					table.addLigne("Evénements identification:", String.valueOf(identCtb != null));
					table.addLigne("Date de référence:", RegDateHelper.dateToDisplayString(dateReference));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		if (civils != null) {

			// Evénements civils : états
			addEntete1("Etats des événements civils");
			{
				addTableSimple(3, new PdfRapport.TableSimpleCallback() {
					public void fillTable(PdfTableSimple table) throws DocumentException {

						table.addLigne("Etat", "Total", "Depuis " + RegDateHelper.dateToDisplayString(dateReference));
						table.setHeaderRows(1);

						final Map<EtatEvenementCivil, BigDecimal> etats = civils.getEtats();
						final Map<EtatEvenementCivil, BigDecimal> etatsNouveaux = civils.getEtatsNouveaux();
						for (EtatEvenementCivil etat : EtatEvenementCivil.values()) {
							final BigDecimal nombre = etats.get(etat);
							final BigDecimal nombreNouveaux = etatsNouveaux.get(etat);

							final String total = nombre != null ? nombre.toPlainString() : "0";
							final String totalNouveaux = nombreNouveaux != null ? nombreNouveaux.toPlainString() : "0";
							table.addLigne(String.format("%s", etat.toString()), total, totalNouveaux);
						}
					}
				});
			}

			// événements civils : types en erreur
			addEntete1("Erreurs par type d'événements civils");
			{
				addTableSimple(new float[] {80f, 20f}, new PdfRapport.TableSimpleCallback() {
					public void fillTable(PdfTableSimple table) throws DocumentException {

						final Map<TypeEvenementCivil, BigDecimal> erreurs = civils.getErreursParType();
						for (TypeEvenementCivil type : TypeEvenementCivil.values()) {
							final BigDecimal nombre = erreurs.get(type);
							if (nombre != null && nombre.signum() > 0) {
								table.addLigne(String.format("%s :", type.getDescription()), nombre.toPlainString());
							}
						}
					}
				});
			}

			// toutes les erreurs
			{
				String filename = "erreurs_evts_civils.csv";
				String contenu = asCsvFile(civils.getToutesErreurs(), filename, status);
				String titre = "Erreurs des événements civils";
				String listVide = "(aucune)";
				addListeDetaillee(writer, civils.getToutesErreurs().size(), titre, listVide, filename, contenu);
			}

			// manipulations manuelles
			addEntete1("Manipulations manuelles d'événements civils depuis le " + RegDateHelper.dateToDisplayString(dateReference));
			{
				addTableSimple(2, new PdfRapport.TableSimpleCallback() {
					public void fillTable(PdfTableSimple table) throws DocumentException {
						final List<StatsEvenementsCivilsResults.EvenementCivilTraiteManuellementInfo> manips = civils.getManipulationsManuelles();
						final int nbManips = manips != null ? manips.size() : 0;
						table.addLigne("Nombre :", Integer.toString(nbManips));
					}
				});
			}

			// manipulations manuelles
			{
				String filename = "manipulations_evts_civils.csv";
				String contenu = asCsvFile(civils.getManipulationsManuelles(), filename, status);
				String titre = "Manipulations manuelles des événements civils";
				String listVide = "(aucune)";
				addListeDetaillee(writer, civils.getManipulationsManuelles().size(), titre, listVide, filename, contenu);
			}

			// événements ignorés
			addEntete1("Evenements civils ignorés depuis le " + RegDateHelper.dateToDisplayString(dateReference));
			{
				addTableSimple(2, new TableSimpleCallback() {
					public void fillTable(PdfTableSimple table) throws DocumentException {
						final Map<Integer, BigDecimal> ignores = civils.getIgnores();

						if (ignores.size() > 0) {
							table.addLigne("Code reçu", "Nombre");
							table.setHeaderRows(1);

							final List<Integer> codes = new ArrayList<Integer>(ignores.keySet());
							Collections.sort(codes);
							for (int code : codes) {
								final BigDecimal nb = ignores.get(code);
								table.addLigne(String.valueOf(code), nb != null ? nb.toPlainString() : "0");
							}
						}
						else {
							table.addLigne("(aucun)", "");
						}
					}
				});
			}
		}

		// événements externes
		if (externes != null) {

			// événements externes : états
			addEntete1("Etats des événements externes");
			{
				addTableSimple(2, new PdfRapport.TableSimpleCallback() {
					public void fillTable(PdfTableSimple table) throws DocumentException {

						final Map<EtatEvenementExterne, BigDecimal> etats = externes.getEtats();
						for (EtatEvenementExterne etat : EtatEvenementExterne.values()) {
							final BigDecimal nombre = etats.get(etat);
							table.addLigne(String.format("Etat %s :", etat.toString()), nombre != null ? nombre.toPlainString() : "0");
						}
					}
				});
			}

			// événements externes : erreurs
			{
				String filename = "erreurs_evts_externes.csv";
				String contenu = asCsvFile(externes.getErreurs(), filename, status);
				String titre = "Erreurs des événements externes";
				String listVide = "(aucune)";
				addListeDetaillee(writer, externes.getErreurs().size(), titre, listVide, filename, contenu);
			}
		}

		// événements d'identification de contribuable
		if (identCtb != null) {

			// événements d'identification de contribuable : états
			addEntete1("Etats des événements d'identification de contribuable");
			{
				addTableSimple(new float[] {60f, 20f, 20f}, new PdfRapport.TableSimpleCallback() {
					public void fillTable(PdfTableSimple table) throws DocumentException {

						table.addLigne("Etat", "Total", "Depuis " + RegDateHelper.dateToDisplayString(dateReference));
						table.setHeaderRows(1);

						final Map<IdentificationContribuable.Etat, BigDecimal> etats = identCtb.getEtats();
						final Map<IdentificationContribuable.Etat, BigDecimal> etatsNouveaux = identCtb.getEtatsNouveaux();
						for (IdentificationContribuable.Etat etat : IdentificationContribuable.Etat.values()) {
							final BigDecimal nombre = etats.get(etat);
							final BigDecimal nombreNouveaux = etatsNouveaux.get(etat);

							final String total = nombre != null ? nombre.toPlainString() : "0";
							final String totalNouveaux = nombreNouveaux != null ? nombreNouveaux.toPlainString() : "0";
							table.addLigne(String.format("%s", etat.toString()), total, totalNouveaux);
						}
					}
				});
			}

			// événements d'identification de contribuable : restant à traiter
			{
				String filename = "identification_ctb_a_traiter.csv";
				String contenu = asCsvFile(identCtb.getATraiter(), filename, status);
				String titre = "Evénements d'identification à traiter";
				String listVide = "(aucun)";
				addListeDetaillee(writer, identCtb.getATraiter().size(), titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static String asCsvLine(String[] elements, boolean withNewLine) {
		final StringBuilder b = new StringBuilder();
		for (int i = 0 ; i < elements.length ; ++ i) {
			if (i > 0) {
				b.append(COMMA);
			}
			final String string = escapeChars(elements[i]);
			final boolean needsQuote = string.contains("\n");
			if (needsQuote) {
				b.append('\"');
			}
			b.append(string);
			if (needsQuote) {
				b.append('\"');
			}
		}
		if (withNewLine) {
			b.append("\n");
		}
		return b.toString();
	}

	private static <T extends StatistiqueEvenementInfo> String asCsvFile(List<T> elements, String fileName, StatusManager statusManager) {
		String contenu = null;
		if (elements != null && elements.size() > 0) {

			final String message = String.format("Génération du fichier %s", fileName);
			statusManager.setMessage(message, 0);
			final StringBuilder b = new StringBuilder();

			// les noms des colonnes
			b.append(asCsvLine(elements.get(0).getNomsColonnes(), true));

			// les valeurs, ligne par ligne
			final GentilIterator<T> iterator = new GentilIterator<T>(elements);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					statusManager.setMessage(message, iterator.getPercent());
				}
				final T element = iterator.next();
				final String[] valeurs = element.getValeursColonnes();
				b.append(asCsvLine(valeurs, !iterator.isLast()));
			}

			contenu = b.toString();
		}
		return contenu;
	}
}
