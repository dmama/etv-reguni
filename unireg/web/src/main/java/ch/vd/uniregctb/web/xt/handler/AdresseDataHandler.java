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
import org.springmodules.xt.ajax.action.RemoveContentAction;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.Container;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.manager.AdresseDataManager;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.web.xt.component.SimpleText;

/**
 * Cet ajax handler permet d'afficher les information civil d'un individu lorsque la souris est positionner sur ses nom/Prénoms.
 *
 * @author Baba Issa Ngom <babab-issa.ngom@vd.ch>
 */
public class AdresseDataHandler extends AbstractAjaxHandler implements ApplicationContextAware {

	private AdresseDataManager adresseDataManager;

	private MessageSourceAccessor messageSourceAccessor;

	private ServiceInfrastructureService serviceInfra;

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setAdresseDataManager(AdresseDataManager adresseDataManager) {
		this.adresseDataManager = adresseDataManager;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.messageSourceAccessor = new MessageSourceAccessor(applicationContext);
	}

	public AjaxResponse showAdresseData(AjaxActionEvent event) throws AdressesResolutionException, InfrastructureException {

		final Long tiersId = Long.valueOf(event.getParameters().get("tiersId"));

		// on récupère les infos concernant le tiers
		AdresseView adresseView = adresseDataManager.geAdresseViewFromTiers(tiersId);

		AjaxResponse response = new AjaxResponseImpl();
		List<Component> components = new ArrayList<Component>();

		// on affiche un post-it que si c'est nécessaire
		if (adresseView != null) {
			components.add(new FicheAdresse(adresseView));
		}
		else {
			components.add(new FicheAdresse());
		}

		final String elementId = event.getParameters().get("elementId");
		response.addAction(new RemoveContentAction(elementId));
		response.addAction(new ReplaceContentAction(elementId, components));

		return response;
	}

	private class FicheAdresse extends Container {

		private final String rue;

		private final String complements;

		private final String localite;

		private final Integer paysOFS;

		/**
		 * Source de l'adresse (civile ou fiscale)
		 */
		private AdresseGenerique.Source source;

		public FicheAdresse(AdresseView adresseView) throws InfrastructureException {

			super(Type.DIV);
			Assert.isTrue(adresseView != null);
			this.complements = adresseView.getComplements();
			this.rue = adresseView.getRue();
			this.localite = adresseView.getLocalite();
			this.paysOFS = adresseView.getPaysOFS();
			this.source = adresseView.getSource();

			this.addComponent(new BoldComponent("Dernière adresse Vaudoise"));
			this.addComponent(new BreakLineComponent());
			this.addComponent(new SimpleText("Source : "));
			this.addComponent(new BoldComponent((source.name())));

			this.addComponent(new SimpleText(complements));
			this.addComponent(new BreakLineComponent());
			this.addComponent(new SimpleText(rue));
			this.addComponent(new BreakLineComponent());
			this.addComponent(new SimpleText(localite));
			this.addComponent(new BreakLineComponent());
			String pays = serviceInfra.getPays(paysOFS).getNomMinuscule();
			this.addComponent(new SimpleText(pays));


		}

		public FicheAdresse() {

			super(Type.DIV);

			this.rue = null;

			this.complements = null;

			this.localite = null;

			this.paysOFS = null;

			this.addComponent(new SimpleText(messageSourceAccessor.getMessage("label.nonHabitant.adresse.vaudoise.inconnue")));

		}

	}

}
