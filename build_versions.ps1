$versions = @(
    @{ Name = "1.21.1"; ApiVersion = "1.21.1-R0.1-SNAPSHOT" },
    @{ Name = "1.21.11"; ApiVersion = "1.21.11-R0.1-SNAPSHOT" },
    @{ Name = "26.1"; ApiVersion = "26.1.2.build.74-stable" }
)

$pomFile = "pom.xml"
$buildsDir = "builds"

if (!(Test-Path $buildsDir)) {
    New-Item -ItemType Directory -Path $buildsDir | Out-Null
}

$originalPom = Get-Content $pomFile -Raw

foreach ($version in $versions) {
    Write-Host "Building for Minecraft $($version.Name) (API: $($version.ApiVersion))..."
    
    # Replace the paper-api version in pom.xml using a regex that captures the paper-api block
    $modifiedPom = $originalPom -replace '(?s)(<groupId>io\.papermc\.paper</groupId>\s*<artifactId>paper-api</artifactId>\s*<version>)[^<]+(</version>)', ("`${1}" + $version.ApiVersion + "`${2}")
    
    Set-Content -Path $pomFile -Value $modifiedPom
    
    # Run the build
    $process = Start-Process -FilePath "mvnd.cmd" -ArgumentList "clean package" -NoNewWindow -Wait -PassThru
    
    if ($process.ExitCode -eq 0) {
        # Copy the jar
        Copy-Item -Path "target\AdvancedInvisibility-1.0.0.jar" -Destination "$buildsDir\AdvancedInvisibility-$($version.Name).jar" -Force
        Write-Host "Successfully generated builds\AdvancedInvisibility-$($version.Name).jar`n" -ForegroundColor Green
    } else {
        Write-Host "Build failed for $($version.Name)`n" -ForegroundColor Red
    }
}

# Restore original pom.xml
Set-Content -Path $pomFile -Value $originalPom
Write-Host "Done! Original pom.xml restored."
