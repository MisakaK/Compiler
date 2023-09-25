package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class Environment {
  // 对当前环境的外围环境的引用
  final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();
  // 追踪未初始化或赋值的变量
  private final Set<String> tracker = new HashSet<>();

  // 无参构造函数用于全局作用域环境
  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      if (tracker.contains(name.lexeme)) {
        return values.get(name.lexeme);
      }
      // 未初始化或赋值的变量
      throw new RuntimeError(name, "Uninitialized or assigned variable '" + name.lexeme + "'.");
    }

    // 遍历环境链以找到变量
    if (enclosing != null) {
      return enclosing.get(name);
    }
    // 使用未定义变量，直接抛出异常
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      tracker.add(name.lexeme);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    // 赋值操作不允许创建新变量
    throw new RuntimeError(name, "Undefined variable '" + name + "'.");
  }

  void define(String name, Object value) {
    // 允许用户重定义变量
    values.put(name, value);
  }

  void mark(String name) {
    tracker.add(name);
  }
}