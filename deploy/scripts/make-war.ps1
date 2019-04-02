param (
    [Parameter(Mandatory=$true)][string]$image
)

$warOutput = & jar -cvf $image *
$warOutput |
        Where-Object {$_ -match 'adding: '} |
        ForEach-Object {
            $_ -replace 'adding:\s+.+\(in\s+=\s+',''
        } |
        ForEach-Object {
            $_ -replace '\)\s+\(out=\s+',' '
        } |
        ForEach-Object {
            $_ -replace '\)\s*(?:(?:\(deflated\s+[0-9]+%\))|(?:\(stored\s+[0-9]+%\)))',''
        }
