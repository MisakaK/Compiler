package com.craftinginterpreters.lox;

enum TokenType{
  // 单字符标记
  // (            )         {               }
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  // ,    .     -      +      ;        /      *
  COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

  // 单、双字符标记
  BANG, BANG_EQUAL,
  EQUAL, EQUAL_EQUAL,
  GREATER, GREATER_EQUAL,
  LESS, LESS_EQUAL,

  // 文字
  IDENTIFIER, STRING, NUMBER,

  // 关键词
  AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
  PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, EOF
}
