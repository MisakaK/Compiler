package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class Environment {
  // 对当前环境的外围环境的引用
  final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  // 无参构造函数用于全局作用域环境
  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
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

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }

    return environment;
  }

  Object getAt(int distance, String name) {
    return ancestor(distance).values.get(name);
  }

  void assignAt(int distance, Token name, Object value) {
    ancestor(distance).values.put(name.lexeme, value);
  }
}