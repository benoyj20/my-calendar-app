package calendar;

import static org.junit.Assert.assertEquals;

import calendar.controller.CommandTokenizer;
import java.util.List;
import org.junit.Test;

/**
 * Ensures the CommandTokenizer properly splits raw input strings into a list of tokens,
 * respecting quotes for multi-word arguments.
 */
public class CommandTokenizerTest {

  @Test
  public void testTokenizingEmptyQuotes() {
    String cmd = "edit event subject \"Old\" with \"\"";
    List<String> expected = List.of("edit", "event", "subject", "Old", "with", "");
    assertEquals(expected, CommandTokenizer.tokenize(cmd));
  }

  @Test
  public void testTokenizingBasicCommand() {
    String cmd = "create event Lunch on 2025-05-01";
    List<String> expected = List.of("create", "event", "Lunch", "on", "2025-05-01");
    assertEquals(expected, CommandTokenizer.tokenize(cmd));
  }

  @Test
  public void testTokenizingMixedContent() {
    String cmd = "copy \"Project Alpha\" to \"Backup Drive\"";
    List<String> expected = List.of("copy", "Project Alpha", "to", "Backup Drive");
    assertEquals(expected, CommandTokenizer.tokenize(cmd));
  }

  @Test
  public void testTokenizingQuotedStrings() {
    String cmd = "create event \"Weekly Sync\" from 2025-01-01T09:00 to 2025-01-01T10:00";
    List<String> expected = List.of("create", "event", "Weekly Sync", "from",
        "2025-01-01T09:00", "to", "2025-01-01T10:00");
    assertEquals(expected, CommandTokenizer.tokenize(cmd));
  }

  @Test
  public void testTokenizingEmptyInput() {
    assertEquals(List.of(), CommandTokenizer.tokenize(""));
    assertEquals(List.of(), CommandTokenizer.tokenize("    "));
  }
}