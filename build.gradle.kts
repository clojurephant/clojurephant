plugins {
  id("org.ajoberstar.grgit")
  id("org.ajoberstar.reckon")
  id("com.diffplug.spotless")
}

///////////////////////////////////////////////////////////////////////////////
// Versioning and Release
///////////////////////////////////////////////////////////////////////////////
reckon {
  scopeFromProp()
  stageFromProp("alpha", "beta", "rc", "final")
}