package ch.vd.uniregctb.web.xt.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springmodules.xt.ajax.AbstractAjaxHandler;
import org.springmodules.xt.ajax.AjaxActionEvent;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxResponseImpl;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.Component;
import org.springmodules.xt.ajax.component.Option;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper.TypeFor;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

/**
 * Cet ajax handler permet de mettre-à-jour la liste des motifs d'ouverture et de fermeture des fors fiscaux en fonction du type de for, du
 * type de rattachement et du genre d'impôt.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MotifsForHandler extends AbstractAjaxHandler implements ApplicationContextAware {

	private MessageSourceAccessor messageSourceAccessor;

	private TiersDAO tiersDAO;
	private PlatformTransactionManager transactionManager;

	/**
	 * Met-à-jour la liste déroulante Html des motifs d'ouverture de fors.
	 *
	 * @param event l'événement Ajax
	 * @return une réponse Ajax
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public AjaxResponse updateMotifsOuverture(AjaxActionEvent event) {

		final Map<String, String> parameters = event.getParameters();
		final String motifsOuvertureSelectId = parameters.get("motifsOuvertureSelectId");
		final MotifFor motifCourant = getMotif(parameters.get("motifCourant"));

		final TypeFor typeFor = extractTypeFor(parameters);
		final List<MotifFor> motifs;
		if (typeFor != null) {
			motifs = MotifsForHelper.getMotifsOuverture(typeFor);
		}
		else {
			motifs = Collections.emptyList();
		}

		final List<Component> components = asComponents(motifs, motifCourant, false, true);
		final AjaxResponse response = new AjaxResponseImpl();
		response.addAction(new ReplaceContentAction(motifsOuvertureSelectId, components));
		return response;
	}

	private static MotifFor getMotif(String string) {
		if (StringUtils.isEmpty(string)) {
			return null;
		}
		try {
			return MotifFor.valueOf(string);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Met-à-jour la liste déroulante Html des motifs de fermeture de fors.
	 *
	 * @param event l'événement Ajax
	 * @return une réponse Ajax
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public AjaxResponse updateMotifsFermeture(AjaxActionEvent event) {

		final Map<String, String> parameters = event.getParameters();
		final String motifsFermetureSelectId = parameters.get("motifsFermetureSelectId");
		final MotifFor motifCourant = getMotif(parameters.get("motifCourant"));
		final TypeFor typeFor = extractTypeFor(parameters);

		final List<MotifFor> motifs;
		if (typeFor != null) {
			motifs = MotifsForHelper.getMotifsFermeture(typeFor);
		}
		else {
			motifs = Collections.emptyList();
		}

		final List<Component> components = asComponents(motifs, motifCourant, true, false);
		final AjaxResponse response = new AjaxResponseImpl();
		response.addAction(new ReplaceContentAction(motifsFermetureSelectId, components));
		return response;
	}

	/**
	 * Extrait le type de for à partir des paramètres spécifiés dans la requête ajax
	 *
	 * @param parameters les paramètres de la requête
	 * @return null si le numéro de contribuable ne correspond à rien de connu
	 */
	@SuppressWarnings({"UnnecessaryLocalVariable"})
	private TypeFor extractTypeFor(Map<String, String> parameters) {
		final String numeroCtbString = parameters.get("numeroCtb");
		final String genreImpotString = parameters.get("genreImpot");
		final String rattachementString = parameters.get("rattachement");

		final GenreImpot genreImpot = GenreImpot.valueOf(genreImpotString);
		final MotifRattachement rattachement = MotifRattachement.valueOf(rattachementString);
		final Long numeroCtb = Long.valueOf(numeroCtbString);

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final TypeFor typeFor = template.execute(new TransactionCallback<TypeFor>() {
			@Override
			public TypeFor doInTransaction(TransactionStatus status) {
				final Tiers tiers = tiersDAO.get(numeroCtb);
				if (tiers == null) {
					return null;
				}
				final NatureTiers natureTiers = tiers.getNatureTiers();
				return new TypeFor(natureTiers, genreImpot, rattachement);
			}
		});

		return typeFor;
	}

	/**
	 * Convertir une liste de motifs de for en composants 'option' d'une list-box Html.
	 *
	 * @param motifs la liste des motifs
	 * @param motifCourant le motif couramment sélectionné, ou <b>null</b> si aucun motif ne l'est.
	 * @param needEmptyOption si <b>vrai</b> ajoute dans tous les cas un motif vide en première position.
	 * @param ouverture si <b>vrai</b>, les libellés sont les libellés d'ouverture, sinon ce sont les libellés de fermeture
	 * @return une liste de composants 'option' à partir des motifs spécifiés.
	 */
	private List<Component> asComponents(final List<MotifFor> motifs, MotifFor motifCourant, boolean needEmptyOption, boolean ouverture) {
		final int size = motifs.size();
		final List<Component> components = new ArrayList<Component>(size);

		// on autorise l'option vide dans le cas où :
		// 1. elle est explicitement demandée (voir needEmptyOption), ou
		// 2. elle n'est pas demandée, mais le motif courant (= ancienne valeur) n'est pas mappable sur les nouveaux motifs disponibles (s'il y en a plusieurs)
		if (needEmptyOption || ((motifCourant == null || !motifs.contains(motifCourant)) && size > 1)) {
			components.add(new Option("", "")); // motif non-sélectionné
		}
		for (MotifFor m : motifs) {
			final String key = String.format("%s%s", ouverture ? ApplicationConfig.masterKeyMotifOuverture : ApplicationConfig.masterKeyMotifFermeture, m.name());
			final String description = messageSourceAccessor.getMessage(key);
			final Option option = new Option(m.name(), description);
			if (m == motifCourant || (!needEmptyOption && size == 1)) {
				option.setSelected(true);
			}
			components.add(option);
		}
		return components;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.messageSourceAccessor = new MessageSourceAccessor(applicationContext);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
