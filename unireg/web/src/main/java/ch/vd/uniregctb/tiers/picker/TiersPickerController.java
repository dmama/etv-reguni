package ch.vd.uniregctb.tiers.picker;

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

import ch.vd.uniregctb.common.CommonSimpleFormController;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.TiersCriteria;

public class TiersPickerController extends CommonSimpleFormController implements AjaxHandler {

	private GlobalTiersSearcher searcher;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSearcher(GlobalTiersSearcher searcher) {
		this.searcher = searcher;
	}

	public AjaxResponse handle(AjaxEvent event) {
		final AjaxResponse response = new AjaxResponseImpl();
		final List<Component> components = new ArrayList<Component>(1);

		final String query = event.getParameters().get("query");
		final String buttonId = event.getParameters().get("buttonId");
		if (query == null || query.trim().length() < 3) {
			components.add(new SimpleText("Veuillez saisir au minium 3 caractères."));
		}
		else {
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setTypeRechercheDuNom(TiersCriteria.TypeRecherche.CONTIENT);
			criteria.setNomRaison(query);
			final List<TiersIndexedData> list = searcher.search(criteria);
			if (list != null && !list.isEmpty()) {
				components.add(new TiersPickerResultsTable(list, buttonId));
			}
			else {
				components.add(new SimpleText("Aucun tiers trouvé."));
			}
		}

		response.addAction(new ReplaceContentAction("tiers-picker-results", components));
		return response;
	}

	public boolean supports(AjaxEvent event) {
		if (!(event instanceof AjaxActionEvent)) {
			return false;
		}
		final String id = event.getEventId();
		return "updateTiersPickerSearch".equals(id);
	}
}