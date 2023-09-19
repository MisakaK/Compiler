package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  // start指向被扫描词素的第一个字符，current指向正在处理的字符
  private int start = 0;
  private int current = 0;
  // 现在位于第几行？
  private int line = 1;
  // 标识符是否为关键字
  private static final Map<String, TokenType> keywords;
  static {
    keywords = new HashMap<>();
    keywords.put("and",    AND);
    keywords.put("class",  CLASS);
    keywords.put("else",   ELSE);
    keywords.put("false",  FALSE);
    keywords.put("for",    FOR);
    keywords.put("fun",    FUN);
    keywords.put("if",     IF);
    keywords.put("nil",    NIL);
    keywords.put("or",     OR);
    keywords.put("print",  PRINT);
    keywords.put("return", RETURN);
    keywords.put("super",  SUPER);
    keywords.put("this",   THIS);
    keywords.put("true",   TRUE);
    keywords.put("var",    VAR);
    keywords.put("while",  WHILE);
  }

  Scanner(String source) {
    this.source = source;
  }

  List <Token> scanTokens(){
    // 只要还未到代码段尾部，就循环扫描，每次扫描得到一个token
    while (!isAtEnd()){
      start = current;
      scanToken();
    }
    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  // 是否已消费完所有字符
  private boolean isAtEnd(){
    return current >= source.length();
  }

  private void scanToken(){
    // 即使是非法字符，仍然会被advance消费
    char c = advance();
    // 这些词素只包含一个字符
    switch (c){
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break;
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case'=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;
      case '/':
        if (match('/')){
          // 这是注释，这里没有用match函数的原因是：
          // peek函数并不消耗字符，那么可以让'\n'在下次扫描时被检测到
          while (peek() != '\n' && !isAtEnd()){
            advance();
          }
        }
        else if (match('*')) {
          handleComment();
        }
        else {
          addToken(SLASH);
        }
        break;
      case ' ':
      case '\r':
      case '\t':
        break;
      case '\n':
        line++;
        break;
      case '"':
        string();
        break;
      default:
        if (isDigit(c)){
          number();
        }
        else if (isAlpha(c)){
          identifier();
        }
        else {
          lox.error(line, "Unexpected character.");
        }
        break;
    }
  }

  private void handleComment() {
    int level = 1, pre = line;
    while (level > 0) {
      if (match('*')) {
        if (match('/')){
          level--;
        }
        else {
          advance();
        }
      }
      else if (match('/')) {
        if (match('*')) {
          level++;
        }
        else {
          advance();
        }
      }
      else if (match('\n')) {
        pre++;
      }
      else if (isAtEnd()) {
        lox.error(line, "Unclosed comment.");
        return;
      }
      else {
        advance();
      }
    }
    line = pre;
  }

  private void identifier(){
    while (isAlphaNumeric(peek())){
      advance();
    }
    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    // 如果map中没有text的映射，说明text为用户自定义的标识符
    if (type == null){
      type = IDENTIFIER;
    }
    addToken(type);
  }

  private void number(){
    while (isDigit(peek())){
      advance();
    }
    // 寻找小数点，lox不支持形如123.这种字面量，因此还需要peeknext
    // 保证小数点后至少有一个数字
    if (peek() == '.' && isDigit(peekNext())){
      // 消耗掉小数点
      advance();
      while (isDigit(peek())){
        advance();
      }
    }
    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private void string(){
    while (peek() != '"' && !isAtEnd()){
      if (peek() == '\n'){
        line++;
      }
      advance();
    }
    if (isAtEnd()){
      lox.error(line, "Unterminated string.");
      return;
    }
    // 处理右引号
    advance();
    // 剥离引号，current指向右引号的下一个字符，而start指向左引号
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  private boolean match(char expected){
    if (isAtEnd()){
      return false;
    }
    if (source.charAt(current) != expected){
      return false;
    }
    current++;
    return true;
  }

  private char peek(){
    if (isAtEnd()){
      return '\0';
    }
    return source.charAt(current);
  }

  private char peekNext(){
    if (current + 1 >= source.length()){
      return '\0';
    }
    return source.charAt(current + 1);
  }

  private boolean isAlpha(char c){
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  private boolean isAlphaNumeric(char c){
    return isAlpha(c) || isDigit(c);
  }

  private boolean isDigit(char c){
    return c >= '0' && c <= '9';
  }

  private char advance(){
    current++;
    return source.charAt(current - 1);
  }
  //
  private void addToken(TokenType type){
    addToken(type, null);
  }
  private void addToken(TokenType type, Object literal){
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}
