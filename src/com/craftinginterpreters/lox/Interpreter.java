package com.craftinginterpreters.lox;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;
import static com.craftinginterpreters.lox.lox.isPrompt;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    // 对于逻辑运算符，先计算左操作数，判断是否短路
    if (expr.operator.type == OR) {
      if (isTruthy(left)) {
        return left;
      }
    }
    else {
      if (!isTruthy(left)) {
        return left;
      }
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);

    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have fields.");
    }

    Object value = evaluate(expr.value);
    ((LoxInstance)object).set(expr.name, value);
    return value;
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
    }
    // 不会到达
    return null;
  }

  // 对变量表达式求值
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    Object value = lookUpVariable(expr.name, expr);
    if (value == uninitialized) {
      throw new RuntimeError(expr.name, "Variable must be initialized before use");
    }
    return value;
  }

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }
    return value;
  }

  // 检查操作数
  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) {
      return;
    }
    throw new RuntimeError(operator, "Operand must be s number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    TokenType type = operator.type;
      if (left instanceof Double && right instanceof Double) {
        if (type == SLASH && (double)right == 0) {
          throw new RuntimeError(operator, "Divisor cannot be 0.");
        }
        return;
      }
      if (left instanceof String && right instanceof String) {
        return;
      }
      // 除以0的情况
    throw new RuntimeError(operator, "Operands must be numbers or strings.");
  }

  // 在lox中，false和nil是假的，其他都是真的
  private boolean isTruthy(Object object) {
    if (object == null) {
      return false;
    }
    if (object instanceof Boolean) {
      return (boolean)object;
    }
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) {
      return true;
    }
    if (a == null) {
      return false;
    }
    return a.equals(b);
  }

  private String stringify(Object object) {
    if (object == null) {
      return "nil";
    }

    if (object instanceof Double) {
      String text = object.toString();
      // 只显示整数部分
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }

  private boolean compareString(TokenType type, Object left, Object right) {
    String s1 = String.valueOf(left), s2 = String.valueOf(right);
    int res = s1.compareTo(s2);
    switch (type) {
      case GREATER:
        return res > 0;
      case GREATER_EQUAL:
        return res >= 0;
      case LESS:
        return res < 0;
      case LESS_EQUAL:
        return res <= 0;
    }
    // 不会到达这里
//    System.out.println("error: compareString");
    return false;
  }

  // 对一条表达式求值
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  // 执行一条语句
  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement: statements) {
        execute(statement);
      }
    }
    // 即使抛出异常，也会恢复环境
    finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    // 创建一个新的环境
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    environment.define(stmt.name.lexeme, null);
    // 类中的每个方法声明都会变成一个LoxFunction对象
    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      // 用户定义的函数是否名为init
      LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
      methods.put(method.name.lexeme, function);
    }
    LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    if (isPrompt) {
      Object value = evaluate(stmt.expression);
      System.out.println(stringify(value));
    }

    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    // 此处的环境为函数声明时的环境
    LoxFunction function = new LoxFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    }
    else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) {
      value = evaluate(stmt.value);
    }

    throw new Return(value);
  }

  // 如果变量被初始化，就求值。若未初始化，则把值设为nil
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = uninitialized;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        execute(stmt.body);
      }
      catch (BreakException e) {
        break;
      }
    }
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    throw new BreakException(stmt.keyword, "encountered break!");
  }

  public Object visitCommaExpr(Expr.Comma expr) {
    Object nowexpr = null;
    for (int i = 0; i < expr.commaList.size(); i++) {
      nowexpr = evaluate(expr.commaList.get(i));
    }
    return nowexpr;
  }

  @Override
  public Object visitConditionalExpr(Expr.Conditional expr) {
    if (isTruthy(evaluate(expr.condition))){
      return evaluate(expr.trueBranch);
    }
    return evaluate(expr.falseBranch);
  }

  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      // 比较运算符产生布尔值
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        if (left instanceof Double && right instanceof Double) {
          return (double)left > (double)right;
        }
        else {
          return compareString(expr.operator.type, left, right);
        }
      case GREATER_EQUAL:
        if (left instanceof Double && right instanceof Double) {
          return (double)left >= (double)right;
        }
        else {
          return compareString(expr.operator.type, left, right);
        }
      case LESS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left < (double)right;
        }
        else {
          return compareString(expr.operator.type, left, right);
        }
      case LESS_EQUAL:
        if (left instanceof Double && right instanceof Double) {
          return (double)left <= (double)right;
        }
        else {
          return compareString(expr.operator.type, left, right);
        }
        // 等式运算符需要支持混合类型
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;
        // 浮点加法和字符串连接
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        }
        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }
        // right是浮点数
        if (left instanceof String) {
            DecimalFormat decimalFormat = new DecimalFormat("0.###############"); // 指定要保留的小数位数
            String text = decimalFormat.format(right);
            return left + text;
        }
        // left是浮点数
        if (right instanceof String) {
          DecimalFormat decimalFormat = new DecimalFormat("0.###############"); // 指定要保留的小数位数
          String text = decimalFormat.format(left);
          return text + right;
        }
        // 如果上述两种情况都不满足，则抛出异常
//        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double)left / (double)right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;
    }
    // 不会到达这里
    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    // 通常callee是一个标识符
    Object callee = evaluate(expr.callee);
    List<Object> arguments = new ArrayList<>();

    // 构建函数参数列表
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    // 类型检查
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }
    LoxCallable function = (LoxCallable)callee;
    // 检查元数
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " + function.arity() +
              " arguments but got " + arguments.size() + ".");
    }
    return function.call(this, arguments);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    // 只有类的实例才具有属性
    if (object instanceof LoxInstance) {
      return ((LoxInstance)object).get(expr.name);
    }

    throw new RuntimeError(expr.name, "Only instances have properties.");
  }

  // globals时终指向全局作用域
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();
  private static Object uninitialized = new Object();



  // 定义clock本地函数
  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }
    });
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    }
    catch (RuntimeError error) {
      lox.runtimeError(error);
    }
  }
}