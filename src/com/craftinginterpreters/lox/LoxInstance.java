package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  private LoxClass klcass;
  // 每个键是一个属性名称，对应的值是属性的值
  private final Map<String, Object> fields = new HashMap<>();

  LoxInstance(LoxClass kclass) {
    this.klcass = kclass;
  }

  Object get(Token name) {
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }

    throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
  }

  void set(Token name, Object value) {
    fields.put(name.lexeme, value);
  }

  @Override
  public String toString() {
    return klcass.name + " instance";
  }
}
