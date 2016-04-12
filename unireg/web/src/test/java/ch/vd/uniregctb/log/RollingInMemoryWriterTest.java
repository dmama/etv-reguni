package ch.vd.uniregctb.log;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.log.RollingInMemoryWriter;

public class RollingInMemoryWriterTest extends WithoutSpringTest {

	@Test
	public void testEmptyWriter() {
		RollingInMemoryWriter w = new RollingInMemoryWriter(10);
		assertEquals("", w.getBuffer());
	}

	@Test
	public void testNotFullWriter() throws Exception {
		RollingInMemoryWriter w = new RollingInMemoryWriter(10);
		w.write("Line 1\n");
		w.write("Line 2\n");
		w.write("Line 3\n");
		w.write("Line 4\n");
		w.write("Line 5\n");
		w.write("Line 6");
		assertEquals("Line 1\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6", w.getBuffer());
	}

	@Test
	public void testJustFullWriter() throws Exception {
		RollingInMemoryWriter w = new RollingInMemoryWriter(6);
		w.write("Line 1\n");
		w.write("Line 2\n");
		w.write("Line 3\n");
		w.write("Line 4\n");
		w.write("Line 5\n");
		w.write("Line 6");
		assertEquals("Line 1\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6", w.getBuffer());
	}

	@Test
	public void testRollingWriter() throws Exception {
		RollingInMemoryWriter w = new RollingInMemoryWriter(4);
		w.write("Line 1\n");
		w.write("Line 2\n");
		w.write("Line 3\n");
		w.write("Line 4\n");
		w.write("Line 5\n");
		w.write("Line 6");
		assertEquals("Line 3\nLine 4\nLine 5\nLine 6", w.getBuffer());
	}

	@Test
	public void testSmallestWriter() throws Exception {
		RollingInMemoryWriter w = new RollingInMemoryWriter(1);
		w.write("Line 1\n");
		w.write("Line 2\n");
		w.write("Line 3\n");
		w.write("Line 4\n");
		w.write("Line 5\n");
		w.write("Line 6");
		assertEquals("Line 6", w.getBuffer());
	}
}
