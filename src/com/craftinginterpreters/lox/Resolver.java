package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
  private final Stack<Map<String, Variable>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  private enum FunctionType {
    NONE,
    FUNCTION
  }

  private static class Variable {
    final Token name;
    VariableState state;

    private Variable(Token name, VariableState state) {
      this.name = name;
      this.state = state;
    }
  }

  // 变量的三种状态
  private enum VariableState {
    DECLARED,
    DEFINED,
    READ
  }

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    declare(stmt.name);
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);

    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) {
      resolve(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      lox.error(stmt.keyword, "Can't return from top-level code.");
    }
    if (stmt.value != null) {
      resolve(stmt.value);
    }
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    // 解析右侧表达式
    resolve(expr.value);
    resolveLocal(expr, expr.name, false);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);
    for (Expr argument : expr.arguments) {
      resolve(argument);
    }
    return null;
  }

  @Override
  public Void visitCommaExpr(Expr.Comma expr) {
    for (Expr commaExpr : expr.commaList) {
      resolve(commaExpr);
    }
    return null;
  }

  @Override
  public Void visitConditionalExpr(Expr.Conditional expr) {
    resolve(expr.condition);
    resolve(expr.falseBranch);
    resolve(expr.trueBranch);
    return null;
  }


  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() && scopes.peek().containsKey(expr.name.lexeme)
        &&scopes.peek().get(expr.name.lexeme).state == VariableState.DECLARED) {
      lox.error(expr.name, "Can't read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name, true);
    return null;
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;
    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
  }

  // 解析器中，使用栈实现词法作用域
  private void beginScope() {
    scopes.push(new HashMap<String, Variable>());
  }

  private void endScope() {
    Map<String, Variable> scope = scopes.pop();

    for (Map.Entry<String, Variable> entry : scope.entrySet()) {
      if (entry.getValue().state == VariableState.DEFINED) {
        lox.error(entry.getValue().name, "Local variable is not used.");
      }
    }
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) {
      return;
    }

    Map<String, Variable> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      lox.error(name, "Already variable with this name in this scope.");
    }
    // false表示此处还未完成变量声明
    scope.put(name.lexeme, new Variable(name, VariableState.DECLARED));
  }

  private void define(Token name) {
    if (scopes.isEmpty()) {
      return;
    }
    // 变量已经完全初始化
    scopes.peek().get(name.lexeme).state = VariableState.DEFINED;
  }

  private void resolveLocal(Expr expr, Token name, boolean isRead) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);

        // 标记变量已读
        if (isRead) {
          scopes.get(i).get(name.lexeme).state = VariableState.READ;
        }
        return;
      }
    }
    // 如果走到这里，就假设为全局变量
  }
}