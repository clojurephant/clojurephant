package dev.clojurephant.plugin.common;

import java.io.Serializable;
import java.util.Objects;

public class ClojurephantModel implements Serializable {
  private String edn;

  public ClojurephantModel(String edn) {
    this.edn = Objects.requireNonNull(edn, "EDN must not be null.");;
  }

  public String getEdn() {
    return edn;
  }

  @Override
  public boolean equals(Object that) {
    if (that instanceof ClojurephantModel) {
      return this.edn.equals(((ClojurephantModel) that).edn);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return edn.hashCode();
  }

  @Override
  public String toString() {
    return edn;
  }
}
