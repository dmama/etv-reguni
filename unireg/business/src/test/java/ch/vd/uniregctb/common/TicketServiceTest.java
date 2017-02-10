package ch.vd.uniregctb.common;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

public class TicketServiceTest extends WithoutSpringTest {

	private TicketService ticketService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		ticketService = new TicketServiceImpl();
	}

	@Test(timeout = 1000)
	public void testAcquisitionAndRelease() throws Exception {
		final Long keyAnswer = 42L;
		final Long keyOther = 777L;

		final TicketService.Ticket ticket1 = ticketService.getTicket(keyAnswer, null);
		Assert.assertNotNull(ticket1);

		final TicketService.Ticket ticket2 = ticketService.getTicket(keyOther, null);
		Assert.assertNotNull(ticket2);

		ticket1.release();
		ticket2.release();
	}

	@Test(timeout = 1000)
	public void testConcurrency() throws Exception {
		final Long key = 42L;

		// je prends le ticket
		final TicketService.Ticket ticket = ticketService.getTicket(key, null);
		Assert.assertNotNull(ticket);

		// le ticket n'est pas ré-entrant...
		final long ts1 = System.nanoTime();
		try {
			ticketService.getTicket(key, Duration.ofMillis(300));
			Assert.fail("Aurait dû échouer...");
		}
		catch (TicketTimeoutException e) {
			// c'est bien ce qu'on attend...
		}

		final long ts2 = System.nanoTime();
		Assert.assertTrue(Long.toString(ts2 - ts1), ts2 - ts1 > TimeUnit.MILLISECONDS.toNanos(300));
	}

	@Test(timeout = 1000)
	public void testNullKey() throws Exception {
		try {
			ticketService.getTicket(null, Duration.ofMillis(2000));
			Assert.fail("Aurait dû échouer...");
		}
		catch (NullPointerException e) {
			Assert.assertEquals("key", e.getMessage());
		}
	}

	@Test(timeout = 1000)
	public void testDoubleRelease() throws Exception {

		final Long key = 42L;

		// je prends le ticket
		final TicketService.Ticket ticket = ticketService.getTicket(key, null);
		Assert.assertNotNull(ticket);

		// je le relâche
		ticket.release();

		// ... et encore une fois
		try {
			ticket.release();
			Assert.fail("Aurait dû échouer...");
		}
		catch (IllegalStateException e) {
			Assert.assertEquals("Already released!", e.getMessage());
		}
	}
}
