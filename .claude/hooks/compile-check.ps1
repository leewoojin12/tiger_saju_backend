# PostToolUse 훅: 방금 수정/생성된 파일이 .java 이면 컴파일 검증.
# Claude Code가 stdin으로 넘기는 JSON에서 파일 경로를 읽어 판단한다.
# .java 가 아니면 아무것도 하지 않고 통과(exit 0).
# 컴파일 실패 시 exit 2 로 Claude에게 피드백을 돌려준다.

$ErrorActionPreference = "Stop"
try {
    $raw = [Console]::In.ReadToEnd()
    if (-not $raw) { exit 0 }

    $payload = $raw | ConvertFrom-Json
    $file = $payload.tool_input.file_path
    if (-not $file) { $file = $payload.tool_input.path }
    if (-not $file -or ($file -notlike "*.java")) { exit 0 }

    # 프로젝트 루트로 이동 (Claude Code가 제공하는 환경변수 우선)
    $root = $env:CLAUDE_PROJECT_DIR
    if ($root -and (Test-Path $root)) { Set-Location $root }

    $gradlew = Join-Path (Get-Location) "gradlew.bat"
    if (-not (Test-Path $gradlew)) { exit 0 }

    & $gradlew compileJava -q
    if ($LASTEXITCODE -ne 0) {
        Write-Error "compileJava 실패 (수정 파일: $file). 컴파일 오류를 먼저 해결하세요."
        exit 2
    }
    exit 0
}
catch {
    # 훅 자체의 오류는 작업을 막지 않는다.
    Write-Host "compile-check 훅 경고: $($_.Exception.Message)"
    exit 0
}
