package com.craftinginterpreters.lox;

enum TokenType{
  // 单字符
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

  // 单、双目运算符
  BANG, BANG_EQUAL,
  EQUAL, EQUAL_EQUAL,
  GREATER, GREATER_EQUAL,
  LESS, LESS_EQUAL,

  // 三目运算符
  Question, Colon,

  // 字面量
  IDENTIFIER, STRING, NUMBER,

  // 关键字
  AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
  PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, EOF,
  BREAK
}
