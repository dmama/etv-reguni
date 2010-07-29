package ch.vd.uniregctb.web.xt.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springmodules.xt.ajax.AbstractAjaxHandler;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.Table;
import org.springmodules.xt.ajax.component.TableData;
import org.springmodules.xt.ajax.component.TableRow;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.tache.TacheService;
import ch.vd.uniregctb.web.xt.component.SimpleText;

/**
 * Cet ajax handler permet de mettre-à-jour le post-it affiché sur la page principal.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class PostItHandler extends AbstractAjaxHandler implements ApplicationContextAware {

	private TacheService tacheService;

	private MessageSourceAccessor messageSourceAccessor;

	/**
	 * Affiche ou cache le post-it des tâches et dossiers en instance.
	 *
	 * @param event l'événement Ajax
	 * @return une réponse Ajax qui mettra-à-jour le post-it
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public AjaxResponse updatePostIt(AjaxActionEvent event) {

		/*
		 * il est arrivé qu'une requête Ajax n'ait pas l'autentification correcte, on blinde le truc pour éviter de faire péter
		 * l'application à cause du post-it...
		 */
		final Integer oid;
		if (AuthenticationHelper.getAuthentication() != null) {
			oid = AuthenticationHelper.getCurrentOID();
		}
		else {
			oid = -1;
		}

		// on récupère les infos concernant les tâches et les dossiers
		int tachesEnInstanceCount = tacheService.getTachesEnInstanceCount(oid);
		int dossiersEnInstanceCount = tacheService.getDossiersEnInstanceCount(oid);

		AjaxResponse response = new AjaxResponseImpl();
		List<Component> components = new ArrayList<Component>();

		// on affiche un post-it que si c'est nécessaire
		if (tachesEnInstanceCount != 0 || dossiersEnInstanceCount != 0) {
			components.add(new PostIt(tachesEnInstanceCount, dossiersEnInstanceCount));
		}

		response.addAction(new ReplaceContentAction("postit", components));
		return response;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.messageSourceAccessor = new MessageSourceAccessor(applicationContext);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	private class PostIt extends Table {

		private static final long serialVersionUID = -4067295312065472045L;

		public PostIt(int tachesEnInstanceCount, int dossiersEnInstanceCount) {

			addAttribute("border", "0");
			addAttribute("cellpadding", "0");
			addAttribute("cellspacing", "0");
			addAttribute("class", "postit");

			TableRow topRow = new TableRow();
			{
				topRow.addAttribute("class", "top");
				TableData data = new TableData(new SimpleText(""));
				topRow.addTableData(data);
			}
			addTableRow(topRow);

			TableRow middleRow = new TableRow();
			{
				middleRow.addAttribute("class", "middle");
				TableData data = new TableData(new PostItBody(tachesEnInstanceCount, dossiersEnInstanceCount));
				middleRow.addTableData(data);
			}
			addTableRow(middleRow);

			TableRow bottomRow = new TableRow();
			{
				bottomRow.addAttribute("class", "bottom");
				TableData data = new TableData(new SimpleText(""));
				bottomRow.addTableData(data);
			}
			addTableRow(bottomRow);
		}

	}

	private class PostItBody implements Component {

		private static final long serialVersionUID = -1855101701145761719L;

		private final int tachesEnInstanceCount;
		private final int dossiersEnInstanceCount;

		public PostItBody(int tachesEnInstanceCount, int dossiersEnInstanceCount) {
			Assert.isTrue(tachesEnInstanceCount > 0 || dossiersEnInstanceCount > 0);
			this.tachesEnInstanceCount = tachesEnInstanceCount;
			this.dossiersEnInstanceCount = dossiersEnInstanceCount;
		}

		public String render() {

			StringBuilder b = new StringBuilder();

			b.append(messageSourceAccessor.getMessage("label.postit.bonjour"));
			b.append("<br/>");
			b.append(messageSourceAccessor.getMessage("label.postit.il.y.a"));

			if (tachesEnInstanceCount > 0) {
				b.append(" <a href=\"../tache/list.do\">");
				b.append(messageSourceAccessor.getMessage("label.postit.taches", new Object[] {
					String.valueOf(tachesEnInstanceCount)
				}));
				b.append("</a> ");
			}

			if (tachesEnInstanceCount > 0 && dossiersEnInstanceCount > 0) {
				b.append(messageSourceAccessor.getMessage("label.postit.et"));
			}

			if (dossiersEnInstanceCount > 0) {
				b.append(" <a href=\"../tache/list-nouveau-dossier.do\">");
				b.append(messageSourceAccessor.getMessage("label.postit.dossiers", new Object[] {
					String.valueOf(dossiersEnInstanceCount)
				}));
				b.append("</a> ");
			}

			b.append(messageSourceAccessor.getMessage("label.postit.en.instance"));

			return b.toString();
		}
	}
}
