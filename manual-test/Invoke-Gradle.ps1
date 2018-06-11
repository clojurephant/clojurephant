Write-Host 'Running gradle-clojure...'
Push-Location -Path '..'
& ./gradlew publishToMavenLocal
Pop-Location

Write-Host 'Running manual-test...'
& ./gradlew --refresh-dependencies $args
