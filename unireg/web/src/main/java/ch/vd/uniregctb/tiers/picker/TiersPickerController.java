package ch.vd.uniregctb.tiers.picker;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxEvent;
import org.springmodules.xt.ajax.AjaxHandler;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.SimpleText;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CommonSimpleFormController;
import ch.vd.uniregctb.indexer.TooManyClausesIndexerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.indexer.tiers.TopList;
import ch.vd.uniregctb.tiers.TiersCriteria;

public class TiersPickerController extends CommonSimpleFormController implements AjaxHandler {

	private GlobalTiersSearcher searcher;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSearcher(GlobalTiersSearcher searcher) {
		this.searcher = searcher;
	}

	public AjaxResponse handle(AjaxEvent event) {

		final String buttonId = event.getParameters().get("buttonId");
		final List<Component> components;

		if ("tiersPickerQuickSearch".equals(event.getEventId())) {

			final String query = event.getParameters().get("query");
			components = quickSearch(buttonId, query);
		}
		else if ("tiersPickerFullSearch".equals(event.getEventId())) {

			final String id = event.getParameters().get("id");
			final String nomraison = event.getParameters().get("nomraison");
			final String localite = event.getParameters().get("localite");
			final String datenaissance = event.getParameters().get("datenaissance");
			final String noavs = event.getParameters().get("noavs");

			components = fullSearch(buttonId, id, nomraison, localite, datenaissance, noavs);
		}
		else {
			throw new IllegalArgumentException("Type d'action ajax inconnue = [" + event.getEventId() + "]");
		}

		final AjaxResponse response = new AjaxResponseImpl();
		response.addAction(new ReplaceContentAction("tiers-picker-results", components));
		return response;
	}

	private List<Component> quickSearch(String buttonId, String query) {

		final List<Component> components = new ArrayList<Component>();

		if (isLessThan3Chars(query)) {
			components.add(new SimpleText("Veuillez saisir au minium 3 caractères."));
		}
		else {
			try {
				final TopList<TiersIndexedData> list = searcher.searchTop(query, 50);
				if (list != null && !list.isEmpty()) {
					components.add(new SimpleText(buildSummary(list)));
					components.add(new TiersPickerResultsTable(list, buttonId));
				}
				else {
					components.add(new SimpleText("Aucun tiers trouvé."));
				}
			}
			catch (TooManyClausesIndexerException e) {
				components.add(new SimpleText("Un ou plusieurs mots-clés sont trop généraux."));
			}
		}

		return components;
	}

	private List<Component> fullSearch(String buttonId, String id, String nomraison, String localite, String datenaissance, String noavs) {

		final List<Component> components = new ArrayList<Component>();

		if (isLessThan3Chars(id) && isLessThan3Chars(nomraison) && isLessThan3Chars(localite) && isLessThan3Chars(datenaissance) && isLessThan3Chars(noavs)) {
			components.add(new SimpleText("Veuillez saisir au minium 3 caractères."));
		}
		else {
			try {
				TiersCriteria criteria = new TiersCriteria();
				criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.CONTIENT);
				criteria.setTypeRechercheDuPaysLocalite(TiersCriteria.TypeRechercheLocalitePays.ALL);
				if (!isLessThan3Chars(id)) {
					criteria.setNumero(Long.valueOf(id));
				}
				if (!isLessThan3Chars(nomraison)) {
					criteria.setNomRaison(nomraison);
				}
				if (!isLessThan3Chars(localite)) {
					criteria.setLocaliteOuPays(localite);
				}
				if (!isLessThan3Chars(datenaissance)) {
					criteria.setDateNaissance(RegDateHelper.displayStringToRegDate(datenaissance, true));
				}
				if (!isLessThan3Chars(noavs)) {
					criteria.setNumeroAVS(noavs);
				}
				final TopList<TiersIndexedData> list = searcher.searchTop(criteria, 50);
				if (list != null && !list.isEmpty()) {
					components.add(new SimpleText(buildSummary(list)));
					components.add(new TiersPickerResultsTable(list, buttonId));
				}
				else {
					components.add(new SimpleText("Aucun tiers trouvé."));
				}
			}
			catch (TooManyClausesIndexerException e) {
				components.add(new SimpleText("Un ou plusieurs mots-clés sont trop généraux."));
			}
			catch (NumberFormatException e) {
				components.add(new SimpleText("Données erronées : " + e.getMessage()));
			}
			catch (ParseException e) {
				components.add(new SimpleText("Données erronées : " + e.getMessage()));
			}
		}

		return components;
	}

	private static boolean isLessThan3Chars(String s) {
		return s == null || s.trim().length() < 3;
	}

	private static String buildSummary(TopList<TiersIndexedData> list) {
		String summary = "Trouvé " + list.getTotalHits() + " tiers";
		if (list.size() < list.getTotalHits()) {
			summary += " (affichage des " + list.size() + " premiers)";
		}
		return summary;
	}

	public boolean supports(AjaxEvent event) {
		if (!(event instanceof AjaxActionEvent)) {
			return false;
		}
		final String id = event.getEventId();
		return "tiersPickerQuickSearch".equals(id) || "tiersPickerFullSearch".equals(id);
	}
}