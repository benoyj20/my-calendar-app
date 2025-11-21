package calendar.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizes a command string. Text is converted into double quotes is treated as one token.
 */
public final class CommandTokenizer {

  private CommandTokenizer() {
  }

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("\"([^\"]*)\"|(\\S+)");

  /**
   * Splits the input into tokens.
   *
   * @param input raw command line
   * @return list of tokens
   */
  public static List<String> tokenize(final String input) {
    List<String> tokens = new ArrayList<>();
    Matcher matcher = TOKEN_PATTERN.matcher(input);

    while (matcher.find()) {
      if (matcher.group(1) != null) {
        tokens.add(matcher.group(1));
      } else {
        tokens.add(matcher.group(2));
      }
    }
    return tokens;
  }
}