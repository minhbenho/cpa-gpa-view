# PowerShell script to run the JavaFX project

# Set JAVA_HOME if not already set
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
    Write-Host "JAVA_HOME set to: $env:JAVA_HOME"
}

# Change to project directory
Set-Location $PSScriptRoot

# Run using Maven wrapper
Write-Host "Building and running the project..."
& ".\mvnw.cmd" clean javafx:run
