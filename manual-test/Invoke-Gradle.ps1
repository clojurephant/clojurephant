Write-Host 'Running clojurephant...'
Push-Location -Path '..'
& ./gradlew publishToMavenLocal
Pop-Location

Write-Host 'Running manual-test...'
& ./gradlew --refresh-dependencies $args
