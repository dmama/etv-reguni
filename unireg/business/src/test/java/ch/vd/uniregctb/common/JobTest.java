package ch.vd.uniregctb.common;

import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_JOBS,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_JOBS
})
public abstract class JobTest extends BusinessTest {
}
