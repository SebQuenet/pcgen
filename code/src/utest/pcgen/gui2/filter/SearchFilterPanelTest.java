package pcgen.gui2.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class SearchFilterPanelTest
{
	@Test
	void typingCoalescesTreeRebuilds() throws Exception
	{
		AtomicInteger rebuilds = new AtomicInteger();
		CountDownLatch rebuilt = new CountDownLatch(1);
		SearchFilterPanel panel = new SearchFilterPanel();
		panel.setFilterHandler(new FilterHandler()
		{
			@Override
			public void refilter()
			{
				rebuilds.incrementAndGet();
				rebuilt.countDown();
			}

			@Override
			public void scrollToTop()
			{
			}

			@Override
			public void setSearchEnabled(boolean enabled)
			{
			}
		});

		SwingUtilities.invokeAndWait(() -> {
			for (Component component : panel.getComponents())
			{
				if (component instanceof JTextField field)
				{
					field.setText("d");
					field.setText("do");
					field.setText("don");
				}
			}
			assertEquals(0, rebuilds.get(), "Typing must not rebuild the tree on the event thread");
		});
		assertTrue(rebuilt.await(2, TimeUnit.SECONDS));
		assertEquals(1, rebuilds.get());
	}
}
