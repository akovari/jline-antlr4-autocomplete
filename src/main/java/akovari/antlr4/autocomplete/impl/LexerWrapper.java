package akovari.antlr4.autocomplete.impl;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.javatuples.Triplet;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LexerWrapper {
  private final LexerFactory lexerFactory;
  private Lexer cachedLexer;
  private final Map<AtomTransition, String> transitionNameCache = new HashMap<>();
  final Map<Triplet<String, ATNState, String>, Set<String>> tokenSuggestionCache = new HashMap<>();

  public static class TokenizationResult {
    public List<? extends Token> tokens;
    public String untokenizedText = "";
  }

  public LexerWrapper(LexerFactory lexerFactory) {
    super();
    this.lexerFactory = lexerFactory;
  }

  public TokenizationResult tokenizeNonDefaultChannel(String input) {
    TokenizationResult result = this.tokenize(input);
    result.tokens = result.tokens.stream().filter(t -> t.getChannel() == Token.DEFAULT_CHANNEL).collect(Collectors.toUnmodifiableList());
    return result;
  }

  public String[] getRuleNames() {
    return getCachedLexer().getRuleNames();
  }

  public ATNState findStateByRuleNumber(int ruleNumber) {
    return getCachedLexer().getATN().ruleToStartState[ruleNumber];
  }

  public Vocabulary getVocabulary() {
    return getCachedLexer().getVocabulary();
  }

  private Lexer getCachedLexer() {
    if (cachedLexer == null) {
      cachedLexer = createLexer("");
    }
    return cachedLexer;
  }

  private TokenizationResult tokenize(String input) {
    Lexer lexer = this.createLexer(input);
    lexer.removeErrorListeners();
    final TokenizationResult result = new TokenizationResult();
    ANTLRErrorListener newErrorListener = new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                              int charPositionInLine, String msg, RecognitionException e) throws ParseCancellationException {
        result.untokenizedText = input.substring(charPositionInLine); // intended side effect
      }
    };
    lexer.addErrorListener(newErrorListener);
    result.tokens = lexer.getAllTokens();
    return result;
  }

  private Lexer createLexer(CharStream input) {
    return this.lexerFactory.createLexer(input);
  }

  private Lexer createLexer(String lexerInput) {
    return this.createLexer(toCharStream(lexerInput));
  }

  private static CharStream toCharStream(String text) {
    CharStream inputStream;
    try {
      inputStream = CharStreams.fromReader(new StringReader(text));
    } catch (IOException e) {
      throw new IllegalStateException("Unexpected while reading input string", e);
    }
    return inputStream;
  }

  public boolean isValidSuggestion(String suggestion) {
    return lexerFactory.isValidSuggestion(suggestion);
  }

  public String getAddedTextFor(AtomTransition transition) {
    return transitionNameCache.computeIfAbsent(transition, (t) -> new String(Character.toChars(t.label)));
  }

  public String stateToString(ATNState atnState) {
    return getRuleNames()[atnState.ruleIndex];
  }
}
