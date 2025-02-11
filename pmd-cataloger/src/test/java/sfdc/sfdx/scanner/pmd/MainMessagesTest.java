package sfdc.sfdx.scanner.pmd;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.salesforce.messaging.EventKey;
import com.salesforce.messaging.MessagePassableException;
import com.salesforce.messaging.Message;
import com.salesforce.messaging.CliMessager;

import java.util.List;

import static org.mockito.Mockito.*;

public class MainMessagesTest {

	@Before
	@After
	public void clearMessages() {
		CliMessager.getInstance().resetMessages();
	}

	@Test
	public void verifySfdxScannerExceptionsToMessages() {
		final EventKey expectedEventKey = EventKey.ERROR_INTERNAL_UNEXPECTED;
		final String[] expectedArgs = {"dummy arg"};
		final MessagePassableException exception = new MessagePassableException(expectedEventKey, expectedArgs);

		// Setup mock
		final Main.Dependencies dependencies = setupMockToThrowException(exception);

		// Execute
		new Main(dependencies).mainInternal(new String[]{"apex=blah"});

		// Validate
		final List<Message> messages = getMessages();
		assertEquals("Unexpected count of messages", 1, messages.size());
		final Message actualMessage = messages.get(0);

		// Validate message
		assertEquals("Unexpected eventKey in message", expectedEventKey.getMessageKey(), actualMessage.getMessageKey());
		assertEquals("Unexpected args in message", actualMessage.getArgs().get(0), expectedArgs[0]);
	}

	@Test
	public void verifyAnyThrowableAddedToMessages() {
		final RuntimeException exception = new RuntimeException("Some dummy message");
		final Main.Dependencies dependencies = setupMockToThrowException(exception);

		// Execute
		new Main(dependencies).mainInternal(new String[]{"apex=blah"});

		// Validate
		List<Message> messages = getMessages();
		assertEquals("Unexpected count of messages", 1, messages.size());
		final Message actualMessage = messages.get(0);

		// Validate message
		assertEquals("Unexpected eventKey in message when handling uncaught exception", EventKey.ERROR_INTERNAL_UNEXPECTED.getMessageKey(), actualMessage.getMessageKey());
		final String actualLog = actualMessage.getInternalLog();
		assertTrue("log field of message should contain message from actual exception", actualLog.contains(exception.getMessage()));
	}

	private Main.Dependencies setupMockToThrowException(Exception exception) {
		Main.Dependencies dependencies = mock(Main.Dependencies.class);
		final PmdRuleCataloger prc = mock(PmdRuleCataloger.class);
		doThrow(exception).when(prc).catalogRules();
		doReturn(prc).when(dependencies).getPmdRuleCataloger(any());
		return dependencies;
	}

	private List<Message> getMessages() {
		final String messagesInJson = CliMessager.getInstance().getAllMessages();
		assertNotNull(messagesInJson);

		// Deserialize JSON to verify further
		final List<Message> messages = new Gson().fromJson(messagesInJson, new TypeToken<List<Message>>() {
		}.getType());
		assertNotNull(messages);
		return messages;
	}
}
