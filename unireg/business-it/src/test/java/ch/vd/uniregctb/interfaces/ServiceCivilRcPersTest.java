package ch.vd.uniregctb.interfaces;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

import static org.junit.Assert.assertNotNull;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-rcpers.xml"
})
public class ServiceCivilRcPersTest extends AbstractServiceCivilTest {

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(ServiceCivilService.class, "serviceCivilRcPers");
	}

	@Override
	public void testGetIndividu() throws Exception {
		// TODO (msi) on ne teste rien en attendant la correction de SIREF-1480
	}

	@Override
	public void testGetConjoint() throws Exception {
		// TODO (msi) on ne teste rien en attendant le déploiement en intégration de la nouvelle version du XSD qui contient l'historique des relations
	}

	@Override
	public void testGetNumeroIndividuConjoint() {
		// TODO (msi) on ne teste rien en attendant le déploiement en intégration de la nouvelle version du XSD qui contient l'historique des relations
	}

	@Override
	public void testGetIndividuConjoint() {
		// TODO (msi) on ne teste rien en attendant le déploiement en intégration de la nouvelle version du XSD qui contient l'historique des relations
	}

	@Test
	public void dumpTest() throws Exception {

		final Individu jean =
				service.getIndividu(702832, null, AttributeIndividu.ADRESSES, AttributeIndividu.ORIGINE, AttributeIndividu.NATIONALITE, AttributeIndividu.ADOPTIONS, AttributeIndividu.ENFANTS,
						AttributeIndividu.PARENTS, AttributeIndividu.PERMIS, AttributeIndividu.TUTELLE);
		assertNotNull(jean);

		System.out.println(IndividuDumper.dump(jean, true));
	}

	@Test
	public void get100Test() throws Exception {

		List<Long> numeros =
				Arrays.asList(
						59543L, 92607L, 120345L, 122790L, 122804L, 122841L, 122863L, 122868L, 122878L, 122880L, 122894L, 122926L, 122950L, 122958L, 122961L, 122964L, 122989L, 122997L, 123005L,
						123037L, 123041L, 123043L, 123069L, 123076L, 123079L, 123082L, 123090L, 123101L, 123108L, 123118L, 123119L, 123130L, 123151L, 123164L, 123166L, 123174L, 123175L, 123192L,
						123204L, 123212L, 123217L, 123226L, 123227L, 123266L, 123273L, 123275L, 123296L, 123311L, 123327L, 123355L, 123419L, 123464L, 123485L, 123492L, 123505L, 123570L, 123601L,
						123619L, 123640L, 123657L, 123664L, 123670L, 123708L, 123720L, 123787L, 123790L, 123873L, 123882L, 123894L, 123897L, 123920L, 123923L, 123946L, 124006L, 124056L, 124065L,
						124088L, 124098L, 124104L, 124161L, 124184L, 124191L, 124211L, 124216L, 124260L, 124278L, 124383L, 124396L, 124415L, 124447L, 124452L, 124463L, 124497L, 124499L, 124507L,
						124525L, 124575L, 124601L, 124614L, 124617L, 124626L, 124631L, 124632L, 124646L, 124663L, 124666L, 124682L, 124702L, 124710L, 124713L, 124724L, 124726L, 124727L, 124730L,
						124734L, 124769L, 124827L, 124829L, 124830L, 124842L, 124848L, 124857L, 124866L, 124880L, 124885L, 124887L, 124896L, 124900L, 124919L, 124944L, 124947L, 124968L, 124975L,
						124982L, 125023L, 125030L, 125103L, 125109L, 125135L, 125138L, 125140L, 125157L, 125179L, 125187L, 125209L, 125229L, 125235L, 125247L, 125267L, 125272L, 125295L, 125303L,
						125309L, 125313L, 125325L, 125326L, 125358L, 125422L, 125461L, 125481L, 125510L, 125516L, 125519L, 125546L, 125556L, 125562L, 125563L, 125586L, 125587L, 125594L, 125597L,
						125618L, 125646L, 125652L, 125689L, 125693L, 125709L, 125742L, 125768L, 125769L, 125772L, 125787L, 125810L, 125817L, 125823L, 125831L, 125836L, 125887L, 125919L, 125929L,
						125946L, 125956L, 125960L, 125974L, 125986L, 126022L, 126029L, 126033L, 126035L, 126050L
				);

		for (Long numero : numeros) {
			try {
				long start = System.nanoTime();
				service.getIndividu(numero, null, AttributeIndividu.ADRESSES, AttributeIndividu.ORIGINE, AttributeIndividu.NATIONALITE, AttributeIndividu.ADOPTIONS, AttributeIndividu.ENFANTS,
						AttributeIndividu.PARENTS, AttributeIndividu.PERMIS, AttributeIndividu.TUTELLE);
				long end = System.nanoTime();
				long ms = (end - start) / 1000000;
				System.out.println(numero + " OK (" + ms + " ms)");
			}
			catch (Exception e) {
				System.err.println(numero + " Exception : " + e.getMessage());
			}
		}
	}
}
