package calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import calendar.controller.CommandTokenizer;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.Test;

/**
 * Tests the {@link CommandTokenizer} utility class.
 */
public class CommandTokenizerTest {

  /**
   * Tests a simple command with no quotes.
   */
  @Test
  public void testTokenizeSimple() {
    String cmd = "create event Test on 2025-01-01";
    List<String> expected = List.of("create", "event", "Test", "on", "2025-01-01");
    assertEquals(expected, CommandTokenizer.tokenize(cmd));
  }

  /**
   * Tests a command with a double-quoted string.
   */
  @Test
  public void testTokenizeWithQuotes() {
    String cmd = "create event \"Team Meeting\" from 2025-01-01T10:00 to 2025-01-01T11:00";
    List<String> expected = List.of("create", "event", "Team Meeting", "from",
        "2025-01-01T10:00", "to", "2025-01-01T11:00");
    assertEquals(expected, CommandTokenizer.tokenize(cmd));
  }

  /**
   * Tests empty and whitespace-only input.
   */
  @Test
  public void testTokenizeEmpty() {
    assertEquals(List.of(), CommandTokenizer.tokenize(""));
    assertEquals(List.of(), CommandTokenizer.tokenize("    "));
  }

  /**
   * Tests an empty double-quoted string ("").
   */
  @Test
  public void testTokenizeEmptyQuote() {
    String cmd = "edit event subject \"Old\" with \"\"";
    List<String> expected = List.of("edit", "event", "subject", "Old", "with", "");
    assertEquals(expected, CommandTokenizer.tokenize(cmd));
  }

  /**
   * Tests mixed quoted and unquoted tokens.
   */
  @Test
  public void testTokenizeMixedTokens() {
    String cmd = "a \"b c\" d";
    List<String> expected = List.of("a", "b c", "d");
    assertEquals(expected, CommandTokenizer.tokenize(cmd));
  }

  /**
   * Tests the private constructor for 100% code coverage.
   *
   * @throws Exception if reflection fails
   */
  @Test
  public void testTokenizerConstructor() throws Exception {
    Constructor<CommandTokenizer> constructor = CommandTokenizer.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    constructor.newInstance();
    assertNotNull(constructor);
  }
}