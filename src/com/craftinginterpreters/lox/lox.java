package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class lox {
  private static final Interpreter interpreter = new Interpreter();
  static boolean hadError = false;
  static boolean hadRuntimeError = false;
  static boolean isPrompt = true;

  public static void main(String[] args) throws IOException{
    // args是命令行参数
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1){
      isPrompt = false;
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
    // 发生错误
    if (hadError){
      System.exit(65);
    }
    if (hadRuntimeError) {
      System.exit(70);
    }
  }

  private static void runPrompt() throws IOException{
    // 把字节输入流转化为字符输入流
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;){
      System.out.print("> ");
      // 读取用户在命令行上的一行输入，若用户输入Control-D，则终止程序
      String line = reader.readLine();
      if (line == null){
        break;
      }
      run(line);
      // 即使用户输入有误，也不终止会话
      hadError = false;
    }
  }

  static void run(String source){
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();
    Parser parser = new Parser(tokens);
    List<Stmt> statements = parser.parse();
    if (hadError) {
      return;
    }
    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);
    if (hadError) {
      return;
    }
//    for (Token token : tokens){
//      System.out.println(token);
//    }
//    System.out.println(new AstPrinter().print(expression));
    interpreter.interpret(statements);
  }
  // 错误报告函数
  static void error(int line, String message){
    report(line, "", message);
  }

  private static void report(int line, String where, String message){
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    }
    else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }
}
