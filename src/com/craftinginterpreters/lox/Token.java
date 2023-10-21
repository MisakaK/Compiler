package com.craftinginterpreters.lox;

class Token{
  final TokenType type;
  final String lexeme;
  // 字面量 例如字符串"abc"，常量123
  final Object literal;
  final int line;

  Token(TokenType type, String lexeme, Object literal, int line){
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString(){
    return type + " " + lexeme + " " + literal;
  }
}