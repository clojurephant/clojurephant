plugins {
  id("convention.lint")

  id("org.ajoberstar.grgit")
  id("org.ajoberstar.reckon")
}

reckon {
  scopeFromProp()
  stageFromProp("alpha", "beta", "rc", "final")
}
