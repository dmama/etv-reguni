package ch.vd.uniregctb.web.xt.handler;

import java.util.ArrayList;
import java.util.Date;
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
import org.springmodules.xt.ajax.component.Table;
import org.springmodules.xt.ajax.component.TableData;
import org.springmodules.xt.ajax.component.TableRow;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.individu.IndividuView;
import ch.vd.uniregctb.tiers.manager.CivilDataManager;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.web.xt.component.SimpleText;

/**
 * Cet ajax handler permet d'afficher les information civil d'un individu lorsque la souris est positionner sur ses nom/Prénoms.
 *
 * @author Baba Issa Ngom <babab-issa.ngom@vd.ch>
 */
public class CivilDataHandler extends AbstractAjaxHandler implements ApplicationContextAware {

	private CivilDataManager civilDataManager;

	private MessageSourceAccessor messageSourceAccessor;

	public void setCivilDataManager(CivilDataManager civilDataManager) {
		this.civilDataManager = civilDataManager;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.messageSourceAccessor = new MessageSourceAccessor(applicationContext);
	}

	public AjaxResponse showCivilData(AjaxActionEvent event) throws AdressesResolutionException, InfrastructureException {

		final Long tiersId = Long.valueOf(event.getParameters().get("tiersId"));
		IndividuView individuView = civilDataManager.getIndividuView(tiersId);
		// on récupère les infos concernant le tiers

		AjaxResponse response = new AjaxResponseImpl();
		List<Component> components = new ArrayList<Component>();

		// on affiche un post-it que si c'est nécessaire

		if (individuView != null) {

			components.add((new FicheCivile(individuView)));
		}
		else {
			components.add(new FicheCivile());
		}

		final String elementId = event.getParameters().get("elementId");
		response.addAction(new RemoveContentAction(elementId));
		response.addAction(new ReplaceContentAction(elementId, components));

		return response;
	}

	


	private class FicheCivile extends Container {

		private static final long serialVersionUID = 4554836788791187830L;



		/**
		 * Numero individu
		 */
		private final Long numeroIndividu;

		/**
		 * Nom.
		 */
		private final String nom;

		/**
		 * Nom de naissance
		 */
		private final String nomNaissance;

		/**
		 * Prenom
		 */
		private final String prenom;

		/**
		 * Autres prenoms
		 */
		private final String autresPrenoms;

		/**
		 * Date a laquelle la personne est nee. Il arrive que seule l annee, voire l annee et le mois soient connus.
		 */
		private final Date dateNaissance;

		/**
		 * Le sexe
		 */
		private Sexe sexe;

		/**
		 * Etat civil
		 */
		private final String etatCivil;

		/**
		 * Date dernier changement etat civil
		 */
		private Date dateDernierChgtEtatCivil;

		/**
		 * Numero d'assure social
		 */
		private final String numeroAssureSocial;

		/**
		 * L'ancien numero AVS
		 */
		private final String ancienNumeroAVS;

		/**
		 * Numero de registre des etrangers
		 */
		private String numeroRCE;

		/**
		 * Origine
		 */
		private String origine;

		/**
		 * Nationalite
		 */
		private final String nationalite;

		public FicheCivile(IndividuView individuView) {

			super(Type.DIV);
			Assert.isTrue(individuView != null);
			this.numeroIndividu = individuView.getNumeroIndividu();
			this.nom = individuView.getNom();
			this.nomNaissance = individuView.getNomNaissance();
			this.prenom = individuView.getPrenom();
			this.autresPrenoms = individuView.getAutresPrenoms();
			this.dateNaissance = individuView.getDateNaissance();
			this.nationalite = individuView.getNationalite();
			this.etatCivil = individuView.getEtatCivil();
			this.sexe = individuView.getSexe();
			this.numeroAssureSocial = individuView.getNumeroAssureSocial();
			this.ancienNumeroAVS = individuView.getAncienNumeroAVS();



			createline(messageSourceAccessor.getMessage("label.numero.individu"), String
					.valueOf(numeroIndividu));


			createline(messageSourceAccessor.getMessage("label.nom"), nom);


			createline(messageSourceAccessor.getMessage("label.nom.naissance"), nomNaissance);


			createline(messageSourceAccessor.getMessage("label.prenom"), prenom);


			createline(messageSourceAccessor.getMessage("label.autres.prenoms"),autresPrenoms);


			createline(messageSourceAccessor.getMessage("label.sexe"), sexe.name());


			createline(messageSourceAccessor.getMessage("label.date.naissance"),
					RegDateHelper.dateToDisplayString(RegDate.get(dateNaissance)));


			createline(messageSourceAccessor.getMessage("label.etat.civil"), etatCivil);


			createline(messageSourceAccessor.getMessage("label.nouveau.numero.avs") ,	numeroAssureSocial);


			createline(messageSourceAccessor.getMessage("label.ancien.numero.avs"),ancienNumeroAVS);


			createline(messageSourceAccessor.getMessage("label.nationalite") , nationalite);


		}

		private void createline(String name, String value) {

			this.addComponent(new SimpleText(name+" : "));
			this.addComponent(new BoldComponent(value));
			this.addComponent(new BreakLineComponent());


		}

		public FicheCivile() {
			super(Type.DIV);
			addAttribute("width", "auto");
			this.numeroIndividu = null;
			this.nom = null;
			this.nomNaissance = null;
			this.prenom = null;
			this.autresPrenoms = null;
			this.dateNaissance = null;
			this.nationalite = null;
			this.etatCivil = null;
			this.numeroAssureSocial = null;
			this.ancienNumeroAVS = null;




			this.addComponent(new SimpleText(messageSourceAccessor.getMessage("label.nonHabitant")));


		}





	}
}
