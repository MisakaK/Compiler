package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  private LoxClass klcass;

  LoxInstance(LoxClass kclass) {
    this.klcass = kclass;
  }

  @Override
  public String toString() {
    return klcass.name + " instance";
  }
}
