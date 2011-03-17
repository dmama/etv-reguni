package ch.vd.uniregctb.common;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = {
		"classpath:unireg-norentes-main.xml",
		"classpath:unireg-norentes-scenarios.xml",
		"classpath:unireg-norentes-web.xml"
})
public abstract class NorentesWebTest extends WebTest {

}
