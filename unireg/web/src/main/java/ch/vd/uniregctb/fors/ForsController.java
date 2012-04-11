package ch.vd.uniregctb.fors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.uniregctb.common.ApplicationConfig;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

@Controller
@RequestMapping(value = "/fors")
public class ForsController {

	private TiersDAO tiersDAO;
	private MessageSourceAccessor messageSource;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = new MessageSourceAccessor(messageSource);
	}

	@RequestMapping(value = "/motifsOuverture.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public List<MotifForView> motifsOuverture(@RequestParam(value = "tiersId", required = true) Long tiersId,
	                                      @RequestParam(value = "genreImpot", required = true) GenreImpot genreImpot,
	                                      @RequestParam(value = "rattachement", required = false) MotifRattachement rattachement) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			return Collections.emptyList();
		}

		final NatureTiers natureTiers = tiers.getNatureTiers();
		final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, genreImpot, rattachement);

		final List<MotifForView> list = new ArrayList<MotifForView>();
		for (MotifFor motifFor : MotifsForHelper.getMotifsOuverture(typeFor)) {
			list.add(new MotifForView(motifFor, getLabelOuverture(motifFor)));
		}
		
		return list;
	}

	private String getLabelOuverture(MotifFor motifFor) {
		final String key = String.format("%s%s", ApplicationConfig.masterKeyMotifOuverture, motifFor.name());
		return messageSource.getMessage(key);
	}

	@RequestMapping(value = "/motifsFermeture.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public List<MotifForView> motifsFermeture(@RequestParam(value = "tiersId", required = true) Long tiersId,
	                                      @RequestParam(value = "genreImpot", required = true) GenreImpot genreImpot,
	                                      @RequestParam(value = "rattachement", required = false) MotifRattachement rattachement) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			return Collections.emptyList();
		}

		final NatureTiers natureTiers = tiers.getNatureTiers();
		final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, genreImpot, rattachement);

		final List<MotifForView> list = new ArrayList<MotifForView>();
		for (MotifFor motifFor : MotifsForHelper.getMotifsFermeture(typeFor)) {
			list.add(new MotifForView(motifFor, getLabelFermeture(motifFor)));
		}

		return list;
	}

	private String getLabelFermeture(MotifFor motifFor) {
		final String key = String.format("%s%s", ApplicationConfig.masterKeyMotifFermeture, motifFor.name());
		return messageSource.getMessage(key);
	}

}
