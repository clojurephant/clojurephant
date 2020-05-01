$TemplateDirs = Get-ChildItem -Path templates -Directory -Filter 'clojurephant*'
ForEach ($TemplateDir in $TemplateDirs) {
  $TemplateResourceName = $TemplateDir.BaseName -replace '-', '_'
  $TemplateResourceDir = "$($TemplateDir.FullName)\src\main\resources\clj\new\$TemplateResourceName"
  Copy-Item -Path 'gradlew' -Destination $TemplateResourceDir
  Copy-Item -Path 'gradlew.bat' -Destination $TemplateResourceDir
  Copy-Item -Path 'gradle/wrapper/gradle-wrapper.properties' -Destination $TemplateResourceDir
  Copy-Item -Path 'gradle/wrapper/gradle-wrapper.jar' -Destination $TemplateResourceDir
}
